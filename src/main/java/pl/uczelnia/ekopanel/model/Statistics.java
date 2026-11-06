package pl.uczelnia.ekopanel.model;

import java.math.BigDecimal;

public record Statistics(int count, BigDecimal min, BigDecimal max, BigDecimal mean, BigDecimal median,
                         BigDecimal sampleSd, BigDecimal meanAbsDeviation) {

}
