package com.example.calculator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpressionRequest(
        @NotBlank @Size(max = 1000) String expression,
        AngleMode angleMode) {
}
