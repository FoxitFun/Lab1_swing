package pl.uczelnia.ekopanel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Numbers {
    public static BigDecimal parseDecimal(String text){
        try {
            String cleaned = text.strip().replace(" ", "").replace(",", ".");
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Wartość musi być poprawną liczbą dziesiętną."
            );
        }
    }
}
