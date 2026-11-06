package pl.uczelnia.ekopanel.service;

import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StatisticsCheck {
    public static void main(String[] args) {

        StatisticsService service = new StatisticsService();

        // TEST 1 – pusta lista
        checkStatistics(
                "Pusta lista",
                new Statistics(0, null, null, null, null, null, null),
                service.calculate(List.of())
        );

        // TEST 2 – rekord wykluczony z analizy
        Observation excluded = new Observation(
                1L, 2025, "PL82", "UNEMP_RATE",
                new BigDecimal("5.50"), false
        );

        checkStatistics(
                "Wykluczony rekord",
                new Statistics(0, null, null, null, null, null, null),
                service.calculate(List.of(excluded))
        );

        // TEST 3 – jedna obserwacja
        Observation single = new Observation(
                2L, 2025, "PL82", "UNEMP_RATE",
                new BigDecimal("5.50"), true
        );

        checkStatistics(
                "Jedna obserwacja",
                new Statistics(
                        1,
                        new BigDecimal("5.50"),
                        new BigDecimal("5.50"),
                        new BigDecimal("5.50"),
                        new BigDecimal("5.50"),
                        null,
                        new BigDecimal("0.00")
                ),
                service.calculate(List.of(single))
        );

        // TEST 4 – dwie obserwacje
        Observation first = new Observation(
                3L, 2025, "PL82", "UNEMP_RATE",
                new BigDecimal("2.00"), true
        );

        Observation second = new Observation(
                4L, 2025, "PL82", "UNEMP_RATE",
                new BigDecimal("4.00"), true
        );

        checkStatistics(
                "Dwie obserwacje",
                new Statistics(
                        2,
                        new BigDecimal("2.00"),
                        new BigDecimal("4.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("1.41"),
                        new BigDecimal("1.00")
                ),
                service.calculate(List.of(first, second))
        );

        // TEST 5 – trzy identyczne wartości
        List<Observation> identical = List.of(
                new Observation(5L, 2023, "PL82", "UNEMP_RATE",
                        new BigDecimal("7.25"), true),
                new Observation(6L, 2024, "PL82", "UNEMP_RATE",
                        new BigDecimal("7.25"), true),
                new Observation(7L, 2025, "PL82", "UNEMP_RATE",
                        new BigDecimal("7.25"), true)
        );

        checkStatistics(
                "Trzy identyczne wartości",
                new Statistics(
                        3,
                        new BigDecimal("7.25"),
                        new BigDecimal("7.25"),
                        new BigDecimal("7.25"),
                        new BigDecimal("7.25"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00")
                ),
                service.calculate(identical)
        );

        // TEST 6 – pięć lat danych dla PL82
        List<Observation> fiveYears = List.of(
                new Observation(8L, 2020, "PL82", "UNEMP_RATE",
                        new BigDecimal("9.10"), true),
                new Observation(9L, 2021, "PL82", "UNEMP_RATE",
                        new BigDecimal("8.20"), true),
                new Observation(10L, 2022, "PL82", "UNEMP_RATE",
                        new BigDecimal("7.50"), true),
                new Observation(11L, 2023, "PL82", "UNEMP_RATE",
                        new BigDecimal("6.80"), true),
                new Observation(12L, 2024, "PL82", "UNEMP_RATE",
                        new BigDecimal("6.10"), true)
        );

        checkStatistics(
                "Pięć lat danych",
                new Statistics(
                        5,
                        new BigDecimal("6.10"),
                        new BigDecimal("9.10"),
                        new BigDecimal("7.54"),
                        new BigDecimal("7.50"),
                        new BigDecimal("1.17"),
                        new BigDecimal("0.89")
                ),
                service.calculate(fiveYears)
        );

        System.out.println("\nWszystkie testy zakończone powodzeniem!");
    }

    private static void checkDecimal(String name, BigDecimal expected, BigDecimal actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            throw new AssertionError(
                    name + ": oczekiwano " + expected + ", otrzymano " + actual
            );
        }
    }

    private static void checkStatistics(String testName, Statistics expected, Statistics actual) {

        if (expected.count() != actual.count()) {
            throw new AssertionError(testName + ": niepoprawny count");
        }

        checkDecimal(testName + " min", expected.min(), actual.min());
        checkDecimal(testName + " max", expected.max(), actual.max());
        checkDecimal(testName + " mean", expected.mean(), actual.mean());
        checkDecimal(testName + " median", expected.median(), actual.median());
        checkDecimal(testName + " sampleSd", expected.sampleSd(), actual.sampleSd());
        checkDecimal(testName + " meanAbsDeviation",
                expected.meanAbsDeviation(), actual.meanAbsDeviation());

        System.out.println("[OK] " + testName);
    }

}
