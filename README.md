# Scientific Calculator — Java (Spring Boot) + Angular

A mini full-stack project: an Angular UI where you build an expression, and a Spring Boot REST API
that parses and evaluates it.

```
calculator-app/
├── backend/    Spring Boot 3 (Java 17) REST API  → http://localhost:8080
└── frontend/   Angular 18 (standalone components) → http://localhost:4200
```

## Features
- **Full expressions** with correct precedence and brackets: `2sin(30)+√(16)×π`
- **Trigonometry:** `sin cos tan`, inverses `sin⁻¹ cos⁻¹ tan⁻¹`, hyperbolic `sinh cosh tanh`
- **DEG / RAD** angle mode toggle
- **Logs & powers:** `ln`, `log` (base 10), `eˣ`, `10ˣ`, `x²`, `x³`, `xʸ`, `1/x`
- **Roots & more:** `√`, `∛`, `|x|`, `x!` (factorial), `mod`
- **Constants:** `π`, `e`
- **Implicit multiplication:** `2π`, `3(4)`, `2sin(30)`
- **Ans** key, and pressing an operator right after `=` continues from the last answer
- Missing closing brackets are closed automatically (`sin(30` works)
- Clear error messages: divide by zero, `√` of a negative, `ln(0)`, `tan(90°)`, etc.
- History (last 15) stored on the server — click an entry to edit it again
- Keyboard support: digits, `+ - * / ^ ! ( ) .`, `%` for mod, Enter, Backspace, Esc

## Run it

**1. Backend** (needs Java 17+ and Maven)
```bash
cd backend
mvn spring-boot:run
```

**2. Frontend** (needs Node 18+)
```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200.

## REST API

| Method | Endpoint                    | Body / Response |
|--------|-----------------------------|-----------------|
| POST   | `/api/calculator/evaluate`  | `{"expression": "2sin(30)+√(16)", "angleMode": "DEG"}` → `{"result": 5.0, ...}` |
| GET    | `/api/calculator/history`   | List of recent calculations |
| DELETE | `/api/calculator/history`   | Clears history |

Invalid input returns `400` with `{"error": "Cannot divide by zero"}`.

## How the evaluation works

`ExpressionEvaluator.java` is a small **recursive-descent parser**. Each precedence level is one method:

```
expression := term (('+' | '-') term)*
term       := unary (('*' | '/' | 'mod') unary | implicit-multiplication)*
unary      := ('-' | '+') unary | power
power      := postfix ('^' unary)?        right-associative: 2^3^2 = 512
postfix    := primary '!'*
primary    := number | π | e | function(arg) | '(' expression ')'
```

So `-2^2 = -4` and `2+3×4 = 14`, like a real scientific calculator.

## Tests
```bash
cd backend
mvn test
```

## Code tour
- `backend/.../service/ExpressionEvaluator.java` — the expression parser and math functions
- `backend/.../service/CalculatorService.java` — rounding, error checks, in-memory history
- `backend/.../controller/CalculatorController.java` — REST endpoints, CORS, error handling
- `frontend/src/app/calculator.service.ts` — HTTP client for the API
- `frontend/src/app/app.component.*` — keypad, display, DEG/RAD toggle, history
