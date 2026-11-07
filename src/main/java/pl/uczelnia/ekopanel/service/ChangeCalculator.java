package pl.uczelnia.ekopanel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ChangeCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // Przyrost bezwzględny: Δx = x1 − x0
    public BigDecimal difference(BigDecimal start, BigDecimal end) {
        return end.subtract(start).setScale(2, RoundingMode.HALF_UP);
    }

    // Tempo zmian: r = (x1 − x0) / x0 · 100%
    public BigDecimal relativeChange(BigDecimal start, BigDecimal end) {
        requireNonZero(start);
        return end.subtract(start).multiply(HUNDRED).divide(start, 2, RoundingMode.HALF_UP);
    }

    // Indeks dynamiki: i = x1 / x0 · 100%
    public BigDecimal dynamicsIndex(BigDecimal start, BigDecimal end) {
        requireNonZero(start);
        return end.multiply(HUNDRED).divide(start, 2, RoundingMode.HALF_UP);
    }

    private static void requireNonZero(BigDecimal start) {
        if (start.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Nie można obliczyć zmiany względnej dla zera.");
        }
    }
}
