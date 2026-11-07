package pl.uczelnia.ekopanel.service;

import java.math.BigDecimal;

import pl.uczelnia.ekopanel.model.Indicator;

public class ObservationValidator {
    public static void validate(int year, String regionCode, String indicatorCode,
                                BigDecimal value, Indicator indicator){
        if (year < 1990 || year > 2035){
            throw new IllegalArgumentException(
                    "Rok musi być z zakresu 1990–2035."
            );
        }

        if (regionCode == null || !regionCode.strip().matches("PL\\d{2}")){
            throw new IllegalArgumentException(
                    "Kod regionu musi mieć format PL i dwie cyfry (np. PL82)."
            );
        }

        if (indicatorCode == null || !indicatorCode.strip().matches("[A-Z][A-Z0-9_]{1,29}")) {
            throw new IllegalArgumentException(
                    "Kod wskaźnika musi składać się z wielkich liter, cyfr i podkreśleń."
            );
        }

        if (value == null) {
            throw new IllegalArgumentException(
                    "Wartość nie może być pusta."
            );
        }

        if (indicator != null && "%".equals(indicator.unit())
                && (value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException(
                    "Stopa procentowa musi być z zakresu 0–100."
            );
        }

        if (indicator != null && "PLN".equals(indicator.unit()) && value.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException(
                    "Wartość kwotowa w PLN nie może być ujemna."
            );
        }
    }
}
