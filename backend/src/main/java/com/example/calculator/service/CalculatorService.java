package com.example.calculator.service;

import com.example.calculator.model.AngleMode;
import com.example.calculator.model.CalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Service
public class CalculatorService {

    private static final int MAX_HISTORY = 15;
    // 15 significant digits hides binary noise such as 0.1 + 0.2 = 0.30000000000000004
    private static final MathContext PRECISION = new MathContext(15);

    private final LinkedList<CalculationResult> history = new LinkedList<>();

    public CalculationResult evaluate(String expression, AngleMode angleMode) {
        AngleMode mode = angleMode == null ? AngleMode.DEG : angleMode;
        double raw = ExpressionEvaluator.evaluate(expression, mode);

        if (Double.isNaN(raw)) throw new IllegalArgumentException("Math error");
        if (Double.isInfinite(raw)) throw new IllegalArgumentException("Result is too large");

        double result = BigDecimal.valueOf(raw).round(PRECISION).doubleValue();
        CalculationResult calc = new CalculationResult(expression.trim(), result, mode, LocalDateTime.now());

        synchronized (history) {
            history.addFirst(calc);
            if (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }
        return calc;
    }

    public List<CalculationResult> getHistory() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public void clearHistory() {
        synchronized (history) {
            history.clear();
        }
    }
}
