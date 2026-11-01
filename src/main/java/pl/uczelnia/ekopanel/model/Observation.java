package pl.uczelnia.ekopanel.model;

import java.math.BigDecimal;

public record Observation(Long id, int year, String regionCode,
                          String indicatorCode, BigDecimal value, boolean included) {

}
