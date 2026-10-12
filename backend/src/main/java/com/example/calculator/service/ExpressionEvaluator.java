package com.example.calculator.service;

import com.example.calculator.model.AngleMode;

import java.util.List;

/**
 * Recursive-descent parser that evaluates scientific expressions.
 *
 * Grammar (lowest to highest precedence):
 *   expression := term (('+' | '-') term)*
 *   term       := unary (('*' | '/' | '%' | 'mod') unary | implicit-multiplication)*
 *   unary      := ('-' | '+') unary | power
 *   power      := postfix ('^' unary)?          (right-associative, so 2^3^2 = 2^9)
 *   postfix    := primary '!'*
 *   primary    := number | constant | function arg | '(' expression ')'
 *
 * Accepts display symbols (×, ÷, −, π, √) as well as plain ASCII.
 * A missing ')' at the end of the input is closed automatically, so "sin(30" works.
 */
public final class ExpressionEvaluator {

    private static final double SNAP_TO_ZERO = 1e-12;

    // Longest names first so "asin" wins over "a..." and "cosh" over "cos"
    private static final List<String> NAMES = List.of(
            "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "cbrt",
            "sin", "cos", "tan", "log", "abs", "exp", "mod", "ln", "pi", "e");

    private final String input;
    private final AngleMode angleMode;
    private int pos;

    private ExpressionEvaluator(String input, AngleMode angleMode) {
        this.input = input;
        this.angleMode = angleMode;
    }

    public static double evaluate(String expression, AngleMode angleMode) {
        ExpressionEvaluator parser = new ExpressionEvaluator(expression, angleMode);
        double value = parser.parseExpression();
        parser.skipSpaces();
        if (parser.pos < parser.input.length()) {
            throw new IllegalArgumentException("Unexpected '" + parser.input.charAt(parser.pos) + "'");
        }
        return value;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            if (eat('+')) value += parseTerm();
            else if (eat('-') || eat('−')) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseUnary();
        while (true) {
            if (eat('*') || eat('×')) {
                value *= parseUnary();
            } else if (eat('/') || eat('÷')) {
                double divisor = parseUnary();
                if (divisor == 0) throw new IllegalArgumentException("Cannot divide by zero");
                value /= divisor;
            } else if (eat('%') || eatWord("mod")) {
                double divisor = parseUnary();
                if (divisor == 0) throw new IllegalArgumentException("Cannot take modulo by zero");
                value %= divisor;
            } else if (startsPrimary()) {
                value *= parsePower(); // implicit multiplication: 2π, 3(4), 2sin(30)
            } else {
                return value;
            }
        }
    }

    private double parseUnary() {
        if (eat('-') || eat('−')) return -parseUnary();
        if (eat('+')) return parseUnary();
        return parsePower();
    }

    private double parsePower() {
        double base = parsePostfix();
        if (eat('^')) {
            return Math.pow(base, parseUnary());
        }
        return base;
    }

    private double parsePostfix() {
        double value = parsePrimary();
        while (eat('!')) {
            value = factorial(value);
        }
        return value;
    }

    private double parsePrimary() {
        skipSpaces();
        if (eat('(')) {
            double value = parseExpression();
            if (!eat(')') && pos < input.length()) {
                throw new IllegalArgumentException("Missing ')'");
            }
            return value;
        }
        if (eat('π')) return Math.PI;
        if (eat('√')) return applyFunction("sqrt", parseFunctionArgument());

        if (pos >= input.length()) throw new IllegalArgumentException("Incomplete expression");
        char c = input.charAt(pos);
        if (Character.isDigit(c) || c == '.') return parseNumber();
        if (Character.isLetter(c)) {
            String name = readName();
            return switch (name) {
                case "pi" -> Math.PI;
                case "e" -> Math.E;
                default -> applyFunction(name, parseFunctionArgument());
            };
        }
        throw new IllegalArgumentException("Unexpected '" + c + "'");
    }

    /** Argument in parentheses, or a bare operand such as "sin 30" / "√9". */
    private double parseFunctionArgument() {
        skipSpaces();
        if (pos < input.length() && input.charAt(pos) == '(') {
            return parsePrimary();
        }
        return parseUnary();
    }

    private double parseNumber() {
        int start = pos;
        boolean seenDot = false;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                pos++;
            } else {
                break;
            }
        }
        String text = input.substring(start, pos);
        if (text.equals(".")) throw new IllegalArgumentException("Invalid number");
        return Double.parseDouble(text);
    }

    private String readName() {
        int start = pos;
        while (pos < input.length() && Character.isLetter(input.charAt(pos))) pos++;
        String word = input.substring(start, pos).toLowerCase();
        // Allow names written back to back, e.g. "esin(30)" = e × sin(30)
        for (String name : NAMES) {
            if (word.startsWith(name)) {
                if (name.equals("mod")) break;
                pos = start + name.length();
                return name;
            }
        }
        throw new IllegalArgumentException("Unknown name '" + word + "'");
    }

    private double applyFunction(String name, double x) {
        return switch (name) {
            case "sin" -> snap(Math.sin(toRadians(x)));
            case "cos" -> snap(Math.cos(toRadians(x)));
            case "tan" -> {
                double cos = snap(Math.cos(toRadians(x)));
                if (cos == 0) throw new IllegalArgumentException("tan is undefined for this angle");
                yield snap(Math.sin(toRadians(x))) / cos;
            }
            case "asin" -> fromRadians(Math.asin(requireRange(x, "asin")));
            case "acos" -> fromRadians(Math.acos(requireRange(x, "acos")));
            case "atan" -> fromRadians(Math.atan(x));
            case "sinh" -> Math.sinh(x);
            case "cosh" -> Math.cosh(x);
            case "tanh" -> Math.tanh(x);
            case "ln" -> Math.log(requirePositive(x, "ln"));
            case "log" -> Math.log10(requirePositive(x, "log"));
            case "sqrt" -> {
                if (x < 0) throw new IllegalArgumentException("Square root of a negative number");
                yield Math.sqrt(x);
            }
            case "cbrt" -> Math.cbrt(x);
            case "abs" -> Math.abs(x);
            case "exp" -> Math.exp(x);
            default -> throw new IllegalArgumentException("Unknown function '" + name + "'");
        };
    }

    private static double factorial(double n) {
        if (n < 0 || n != Math.floor(n)) {
            throw new IllegalArgumentException("Factorial needs a non-negative whole number");
        }
        if (n > 170) throw new IllegalArgumentException("Number too large for factorial");
        double result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    private double toRadians(double x) {
        return angleMode == AngleMode.DEG ? Math.toRadians(x) : x;
    }

    private double fromRadians(double x) {
        return angleMode == AngleMode.DEG ? Math.toDegrees(x) : x;
    }

    private static double requireRange(double x, String fn) {
        if (x < -1 || x > 1) throw new IllegalArgumentException(fn + " needs a value between -1 and 1");
        return x;
    }

    private static double requirePositive(double x, String fn) {
        if (x <= 0) throw new IllegalArgumentException(fn + " needs a positive number");
        return x;
    }

    /** Removes floating-point noise such as sin(180°) = 1.2e-16. */
    private static double snap(double x) {
        return Math.abs(x) < SNAP_TO_ZERO ? 0 : x;
    }

    private boolean startsPrimary() {
        skipSpaces();
        if (pos >= input.length()) return false;
        char c = input.charAt(pos);
        if (input.startsWith("mod", pos)) return false;
        return Character.isDigit(c) || Character.isLetter(c) || c == '.' || c == '(' || c == 'π' || c == '√';
    }

    private boolean eat(char expected) {
        skipSpaces();
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean eatWord(String word) {
        skipSpaces();
        if (input.startsWith(word, pos)) {
            pos += word.length();
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }
}
