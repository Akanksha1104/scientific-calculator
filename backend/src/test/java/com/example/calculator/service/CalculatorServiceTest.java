package com.example.calculator.service;

import com.example.calculator.model.AngleMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    private double deg(String expr) {
        return service.evaluate(expr, AngleMode.DEG).result();
    }

    private double rad(String expr) {
        return service.evaluate(expr, AngleMode.RAD).result();
    }

    @Test
    void arithmeticAndPrecedence() {
        assertEquals(14, deg("2 + 3 * 4"));
        assertEquals(20, deg("(2 + 3) * 4"));
        assertEquals(2.5, deg("5 / 2"));
        assertEquals(1, deg("7 mod 3"));
        assertEquals(0.3, deg("0.1 + 0.2"));
        assertEquals(6, deg("2×3"));
        assertEquals(-1, deg("2−3"));
        assertEquals(4, deg("8÷2"));
    }

    @Test
    void powersAndUnaryMinus() {
        assertEquals(512, deg("2^3^2"));
        assertEquals(-4, deg("-2^2"));
        assertEquals(0.5, deg("2^-1"));
        assertEquals(120, deg("5!"));
    }

    @Test
    void trigonometryInDegreesAndRadians() {
        assertEquals(0.5, deg("sin(30)"));
        assertEquals(0, deg("cos(90)"));
        assertEquals(1, deg("tan(45)"));
        assertEquals(0, rad("sin(π)"));
        assertEquals(30, deg("asin(0.5)"));
        assertThrows(IllegalArgumentException.class, () -> deg("tan(90)"));
    }

    @Test
    void functionsConstantsAndImplicitMultiplication() {
        assertEquals(4, deg("√(16)"));
        assertEquals(3, deg("√9"));
        assertEquals(3, deg("cbrt(27)"));
        assertEquals(2, deg("log(100)"));
        assertEquals(1, deg("ln(e)"));
        assertEquals(6.28318530717959, deg("2π"));
        assertEquals(12, deg("3(4)"));
        assertEquals(1, deg("2sin(30)"));
        assertEquals(5, deg("abs(-5)"));
        assertEquals(1, deg("sin(90"));  // auto-closed parenthesis
    }

    @Test
    void errors() {
        assertThrows(IllegalArgumentException.class, () -> deg("1/0"));
        assertThrows(IllegalArgumentException.class, () -> deg("√(-4)"));
        assertThrows(IllegalArgumentException.class, () -> deg("ln(0)"));
        assertThrows(IllegalArgumentException.class, () -> deg("2.5!"));
        assertThrows(IllegalArgumentException.class, () -> deg("2 +"));
        assertThrows(IllegalArgumentException.class, () -> deg("foo(2)"));
        assertThrows(IllegalArgumentException.class, () -> deg("asin(2)"));
    }

    @Test
    void historyKeepsLatestFirst() {
        deg("1 + 1");
        deg("2 * 2");
        assertEquals("2 * 2", service.getHistory().get(0).expression());
        service.clearHistory();
        assertTrue(service.getHistory().isEmpty());
    }
}
