import { Component, HostListener, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AngleMode, CalculationResult, CalculatorService } from './calculator.service';

type KeyKind = 'num' | 'op' | 'fn' | 'action' | 'eq';

interface Key {
  label: string;
  value: string;   // text inserted into the expression, or an action name
  kind: KeyKind;
  title?: string;
}

const k = (label: string, value: string, kind: KeyKind, title?: string): Key => ({ label, value, kind, title });

// Function names the backspace key removes as a single unit
const TOKEN_AT_END = /(asin\(|acos\(|atan\(|sinh\(|cosh\(|tanh\(|sin\(|cos\(|tan\(|ln\(|log\(|cbrt\(|abs\(|√\(|Ans| mod )$/;

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly calculator = inject(CalculatorService);

  expression = '';
  previous = '';        // e.g. "sin(30)+2 =" shown above the main line
  resultText = '';
  error = '';
  angleMode: AngleMode = 'DEG';
  history: CalculationResult[] = [];
  loading = false;

  private ans = 0;
  private justEvaluated = false;

  readonly keys: Key[] = [
    k('sin', 'sin(', 'fn'), k('cos', 'cos(', 'fn'), k('tan', 'tan(', 'fn'), k('ln', 'ln(', 'fn', 'Natural log'), k('log', 'log(', 'fn', 'Log base 10'),
    k('sin⁻¹', 'asin(', 'fn'), k('cos⁻¹', 'acos(', 'fn'), k('tan⁻¹', 'atan(', 'fn'), k('eˣ', 'e^(', 'fn'), k('10ˣ', '10^(', 'fn'),
    k('sinh', 'sinh(', 'fn'), k('cosh', 'cosh(', 'fn'), k('tanh', 'tanh(', 'fn'), k('|x|', 'abs(', 'fn', 'Absolute value'), k('x!', '!', 'fn', 'Factorial'),
    k('x²', '^2', 'fn'), k('x³', '^3', 'fn'), k('xʸ', '^', 'fn', 'Power'), k('√', '√(', 'fn', 'Square root'), k('∛', 'cbrt(', 'fn', 'Cube root'),
    k('(', '(', 'fn'), k(')', ')', 'fn'), k('π', 'π', 'fn'), k('e', 'e', 'fn', "Euler's number"), k('1/x', '^(-1)', 'fn', 'Reciprocal'),
    k('7', '7', 'num'), k('8', '8', 'num'), k('9', '9', 'num'), k('÷', '÷', 'op'), k('AC', 'AC', 'action', 'Clear all'),
    k('4', '4', 'num'), k('5', '5', 'num'), k('6', '6', 'num'), k('×', '×', 'op'), k('⌫', 'DEL', 'action', 'Backspace'),
    k('1', '1', 'num'), k('2', '2', 'num'), k('3', '3', 'num'), k('−', '−', 'op'), k('Ans', 'Ans', 'action', 'Last answer'),
    k('0', '0', 'num'), k('.', '.', 'num'), k('mod', ' mod ', 'op', 'Modulo'), k('+', '+', 'op'), k('=', '=', 'eq'),
  ];

  get mainLine(): string {
    return this.justEvaluated ? this.resultText : this.expression || '0';
  }

  ngOnInit(): void {
    this.loadHistory();
  }

  press(key: Key): void {
    this.error = '';
    switch (key.value) {
      case 'AC': return this.clear();
      case 'DEL': return this.backspace();
      case '=': return this.evaluate();
      default: return this.insert(key.value);
    }
  }

  toggleAngleMode(): void {
    this.angleMode = this.angleMode === 'DEG' ? 'RAD' : 'DEG';
  }

  @HostListener('window:keydown', ['$event'])
  onKey(event: KeyboardEvent): void {
    const map: Record<string, string> = {
      '*': '×', '/': '÷', '-': '−', '%': ' mod ',
      Enter: '=', '=': '=', Backspace: 'DEL', Escape: 'AC',
    };
    const value = map[event.key] ?? (/^[\d.+^!()]$/.test(event.key) ? event.key : null);
    if (value === null) return;
    event.preventDefault();
    this.press({ label: value, value, kind: 'action' });
  }

  private insert(text: string): void {
    if (this.justEvaluated) {
      // Continuing with an operator reuses the last answer: "= then + 5" → "Ans+5"
      const continues = /^(\+|−|×|÷|\^|!| mod )/.test(text);
      this.expression = continues ? 'Ans' : '';
      this.justEvaluated = false;
    }
    if (this.expression.length < 300) {
      this.expression += text;
    }
  }

  private clear(): void {
    this.expression = '';
    this.previous = '';
    this.resultText = '';
    this.justEvaluated = false;
  }

  private backspace(): void {
    if (this.justEvaluated) {
      this.justEvaluated = false;
      this.expression = '';
      return;
    }
    const match = this.expression.match(TOKEN_AT_END);
    this.expression = match
      ? this.expression.slice(0, -match[0].length)
      : this.expression.slice(0, -1);
  }

  private evaluate(): void {
    if (!this.expression.trim() || this.loading) return;

    const expr = this.expression;
    const toSend = expr.replace(/Ans/g, `(${this.plainNumber(this.ans)})`);
    this.loading = true;

    this.calculator.evaluate(toSend, this.angleMode).subscribe({
      next: res => {
        this.loading = false;
        this.ans = res.result;
        this.previous = `${expr} =`;
        this.resultText = this.formatNumber(res.result);
        this.justEvaluated = true;
        this.loadHistory();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.error ?? 'Backend not reachable. Is the Spring Boot server running on :8080?';
      }
    });
  }

  /** Display format: up to 12 significant digits, scientific notation for very large/small values. */
  formatNumber(n: number): string {
    if (n !== 0 && (Math.abs(n) >= 1e12 || Math.abs(n) < 1e-9)) {
      return n.toExponential(10).replace(/\.?0+e/, 'e');
    }
    return String(parseFloat(n.toPrecision(12)));
  }

  /** Full decimal form (no exponent) so the backend parser can read it back. */
  private plainNumber(n: number): string {
    return n.toLocaleString('en-US', { useGrouping: false, maximumFractionDigits: 20 });
  }

  loadHistory(): void {
    this.calculator.getHistory().subscribe({
      next: h => (this.history = h),
      error: () => (this.history = [])
    });
  }

  clearHistory(): void {
    this.calculator.clearHistory().subscribe(() => (this.history = []));
  }

  reuse(item: CalculationResult): void {
    this.error = '';
    this.expression = item.expression;
    this.angleMode = item.angleMode;
    this.justEvaluated = false;
    this.previous = '';
  }
}
