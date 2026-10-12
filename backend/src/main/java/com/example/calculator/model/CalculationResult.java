package com.example.calculator.model;

import java.time.LocalDateTime;

public record CalculationResult(
        String expression,
        double result,
        AngleMode angleMode,
        LocalDateTime timestamp) {
}
