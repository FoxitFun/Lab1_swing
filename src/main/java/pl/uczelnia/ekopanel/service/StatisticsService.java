package pl.uczelnia.ekopanel.service;

import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatisticsService {
    public Statistics calculate(List<Observation> rows) {

        List<Observation> filteredRows = new ArrayList<>();
        List<BigDecimal> sortedValues = new ArrayList<>();

        // 1. Filtrowanie
        for (Observation row : rows) {
            if (!row.included() || row.value() == null) {
                continue;
            }

            filteredRows.add(row);
            sortedValues.add(row.value());
        }

        sortedValues.sort(Comparator.naturalOrder());

        int count = sortedValues.size();

        // 2. Obsługa pustego zbioru
        if (count == 0) {
            return new Statistics(0, null, null, null, null, null, null);
        }

        BigDecimal n = BigDecimal.valueOf(count);

        // 3. Min, max i suma
        BigDecimal min = sortedValues.getFirst();
        BigDecimal max = sortedValues.getLast();
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal value : sortedValues) {
            sum = sum.add(value);
        }

        // 4. Średnia robocza i wynikowa
        BigDecimal meanWorking = sum.divide(n, 4, RoundingMode.HALF_UP);
        BigDecimal mean = meanWorking.setScale(2, RoundingMode.HALF_UP);

        // 5. Mediana
        BigDecimal median;

        if (count % 2 != 0) {
            median = sortedValues.get(count / 2);
        } else {
            int index1 = count / 2 - 1;
            int index2 = count / 2;

            BigDecimal middleSum = sortedValues.get(index1)
                    .add(sortedValues.get(index2));

            median = middleSum.divide(
                    BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP
            );
        }

        // 6. Przeciętne odchylenie bezwzględne
        BigDecimal sumMeanAbsDeviation = BigDecimal.ZERO;

        for (BigDecimal value : sortedValues) {
            BigDecimal deviation = value.subtract(meanWorking).abs();
            sumMeanAbsDeviation = sumMeanAbsDeviation.add(deviation);
        }

        BigDecimal meanAbsDeviation = sumMeanAbsDeviation
                .divide(n, 2, RoundingMode.HALF_UP);

        // 7. Odchylenie standardowe z próby
        BigDecimal sampleSd = null;

        if (count > 1) {
            BigDecimal sumSquaredDeviations = BigDecimal.ZERO;

            for (BigDecimal value : sortedValues) {
                BigDecimal deviation = value.subtract(meanWorking);
                BigDecimal squaredDeviation = deviation.multiply(deviation);

                sumSquaredDeviations = sumSquaredDeviations.add(squaredDeviation);
            }

            BigDecimal divisor = BigDecimal.valueOf(count - 1);

            BigDecimal variance = sumSquaredDeviations.divide(
                    divisor, 8, RoundingMode.HALF_UP
            );

            sampleSd = variance.sqrt(MathContext.DECIMAL64)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new Statistics(
                count, min, max, mean, median, sampleSd, meanAbsDeviation
        );
    }
}
