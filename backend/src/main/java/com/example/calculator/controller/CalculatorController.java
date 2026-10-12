package com.example.calculator.controller;

import com.example.calculator.model.CalculationResult;
import com.example.calculator.model.ExpressionRequest;
import com.example.calculator.service.CalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calculator")
@CrossOrigin(origins = "http://localhost:4200")
public class CalculatorController {

    private final CalculatorService service;

    public CalculatorController(CalculatorService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public CalculationResult evaluate(@Valid @RequestBody ExpressionRequest request) {
        return service.evaluate(request.expression(), request.angleMode());
    }

    @GetMapping("/history")
    public List<CalculationResult> history() {
        return service.getHistory();
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory() {
        service.clearHistory();
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
