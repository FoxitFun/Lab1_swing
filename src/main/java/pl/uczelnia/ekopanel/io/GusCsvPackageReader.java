package pl.uczelnia.ekopanel.io;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.service.Numbers;
import pl.uczelnia.ekopanel.service.ObservationValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GusCsvPackageReader {
    private static final String REGIONS_HEADER = "code;name;macroregion";
    private static final String INDICATORS_HEADER = "code;name;unit;category";
    private static final String OBSERVATIONS_HEADER = "year;region_code;indicator_code;value;included";

    public List<Region> readRegions(Path path) throws IOException {
        List<String> lines = readLines(path, REGIONS_HEADER);
        List<Region> regions = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);
            if (line.isBlank()) continue;
            String[] parts = split(line, 3, lineNumber);
            regions.add(new Region(parts[0], parts[1], parts[2]));
        }
        return regions;
    }

    public List<Indicator> readIndicators(Path path) throws IOException {
        List<String> lines = readLines(path, INDICATORS_HEADER);
        List<Indicator> indicators = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);
            if (line.isBlank()) continue;
            String[] parts = split(line, 4, lineNumber);
            indicators.add(new Indicator(parts[0], parts[1], parts[2], parts[3]));
        }
        return indicators;
    }

    public List<Observation> readObservations(Path path,
                                              Map<String, Region> regionsByCode,
                                              Map<String, Indicator> indicatorsByCode) throws IOException {
        List<String> lines = readLines(path, OBSERVATIONS_HEADER);
        List<Observation> observations = new ArrayList<>();
        Set<String> keys = new HashSet<>();

        for (int i = 1; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);
            if (line.isBlank()) continue;
            try {
                String[] parts = line.split(";", -1);
                if (parts.length != 5) {
                    throw new IllegalArgumentException("Oczekiwano 5 kolumn, znaleziono " + parts.length + ".");
                }
                int year;
                try {
                    year = Integer.parseInt(parts[0].strip());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Rok musi być liczbą całkowitą.");
                }
                String regionCode = parts[1].strip();
                String indicatorCode = parts[2].strip();
                BigDecimal value = parts[3].isBlank() ? null : Numbers.parseDecimal(parts[3]);
                boolean included = parseBoolean(parts[4]);

                if (!regionsByCode.containsKey(regionCode)) {
                    throw new IllegalArgumentException("Nieznany kod regionu: " + regionCode + ".");
                }
                Indicator indicator = indicatorsByCode.get(indicatorCode);
                if (indicator == null) {
                    throw new IllegalArgumentException("Nieznany kod wskaźnika: " + indicatorCode + ".");
                }

                ObservationValidator.validate(year, regionCode, indicatorCode, value, indicator);

                String key = year + "|" + regionCode + "|" + indicatorCode;
                if (!keys.add(key)) {
                    throw new IllegalArgumentException("Duplikat obserwacji " + year + " / "
                            + regionCode + " / " + indicatorCode + ".");
                }

                observations.add(new Observation(null, year, regionCode, indicatorCode, value, included));
            } catch (IllegalArgumentException e) {
                throw new IOException("Wiersz " + lineNumber + ": " + e.getMessage(), e);
            }
        }
        return observations;
    }

    private static List<String> readLines(Path path, String expectedHeader) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("Plik " + path.getFileName() + " jest pusty.");
        }
        String header = lines.getFirst().replace("﻿", "").strip();
        if (!expectedHeader.equals(header)) {
            throw new IOException("Wiersz 1: nieprawidłowy nagłówek pliku " + path.getFileName()
                    + " (oczekiwano " + expectedHeader + ").");
        }
        return lines;
    }

    private static String[] split(String line, int columns, int lineNumber) throws IOException {
        String[] parts = line.split(";", -1);
        if (parts.length != columns) {
            throw new IOException("Wiersz " + lineNumber + ": oczekiwano " + columns
                    + " kolumn, znaleziono " + parts.length + ".");
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].strip();
            if (parts[i].isEmpty()) {
                throw new IOException("Wiersz " + lineNumber + ": pusta kolumna " + (i + 1) + ".");
            }
        }
        return parts;
    }

    private static boolean parseBoolean(String text) {
        String t = text.strip().toLowerCase();
        return switch (t) {
            case "true", "1", "tak" -> true;
            case "false", "0", "nie" -> false;
            default -> throw new IllegalArgumentException("Pole included musi mieć wartość true lub false.");
        };
    }
}
