
package pl.uczelnia.ekopanel.service;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.repository.ObservationRepository;

import java.util.*;

public class ObservationService {

    private final ObservationRepository repository;

    public ObservationService(ObservationRepository repository) {
        this.repository = repository;
    }

    public Observation save(Observation observation) {

        // 1. Sprawdzenie istnienia regionu
        boolean regionExists = repository.findAllRegions()
                .stream()
                .anyMatch(region ->
                        region.code().equals(observation.regionCode()));

        if (!regionExists) {
            throw new IllegalArgumentException(
                    "Nieznany kod regionu: " + observation.regionCode()
            );
        }

        // 2. Znalezienie wskaźnika
        Indicator indicator = repository.findAllIndicators()
                .stream()
                .filter(item ->
                        item.code().equals(observation.indicatorCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nieznany kod wskaźnika: " + observation.indicatorCode()
                ));

        // 3. Walidacja obserwacji
        ObservationValidator.validate(
                observation.year(),
                observation.regionCode(),
                observation.indicatorCode(),
                observation.value(),
                indicator
        );

        // 4. Sprawdzenie duplikatu
        boolean duplicateExists = repository.findAllObservations()
                .stream()
                .anyMatch(existing ->
                        existing.year() == observation.year()
                                && existing.regionCode().equals(observation.regionCode())
                                && existing.indicatorCode().equals(observation.indicatorCode())
                                && !existing.id().equals(observation.id())
                );

        if (duplicateExists) {
            throw new IllegalArgumentException(
                    "Obserwacja dla tego roku, regionu i wskaźnika już istnieje."
            );
        }

        // 5. Zapis obserwacji
        return repository.save(observation);
    }

    public void replaceDataset(
            List<Region> regions,
            List<Indicator> indicators,
            List<Observation> observations
    ) {
        // 1. Sprawdzenie unikalności kodów regionów
        Set<String> regionCodes = new HashSet<>();

        for (Region region : regions) {
            if (!regionCodes.add(region.code())) {
                throw new IllegalArgumentException(
                        "Powtórzony kod regionu: " + region.code()
                );
            }
        }

        // 2. Sprawdzenie unikalności kodów wskaźników
        Set<String> indicatorCodes = new HashSet<>();
        Map<String, Indicator> indicatorsByCode = new HashMap<>();

        for (Indicator indicator : indicators) {
            if (!indicatorCodes.add(indicator.code())) {
                throw new IllegalArgumentException(
                        "Powtórzony kod wskaźnika: " + indicator.code()
                );
            }

            indicatorsByCode.put(indicator.code(), indicator);
        }

        // 3. Sprawdzenie obserwacji
        Set<String> observationKeys = new HashSet<>();

        for (Observation observation : observations) {

            // Sprawdzenie klucza obcego regionu
            if (!regionCodes.contains(observation.regionCode())) {
                throw new IllegalArgumentException(
                        "Nieznany kod regionu: " + observation.regionCode()
                );
            }

            // Sprawdzenie klucza obcego wskaźnika
            Indicator indicator = indicatorsByCode.get(
                    observation.indicatorCode()
            );

            if (indicator == null) {
                throw new IllegalArgumentException(
                        "Nieznany kod wskaźnika: " + observation.indicatorCode()
                );
            }

            // Walidacja obserwacji
            ObservationValidator.validate(
                    observation.year(),
                    observation.regionCode(),
                    observation.indicatorCode(),
                    observation.value(),
                    indicator
            );

            // Sprawdzenie unikalności trójki
            String key = observation.year()
                    + "|" + observation.regionCode()
                    + "|" + observation.indicatorCode();

            if (!observationKeys.add(key)) {
                throw new IllegalArgumentException(
                        "Obserwacja dla tego roku, regionu i wskaźnika już istnieje."
                );
            }
        }

        // 4. Dopiero po przejściu wszystkich kontroli podmieniamy dane
        repository.replaceDataset(regions, indicators, observations);
    }

    public List<Observation> findAllObservations() {
        return repository.findAllObservations();
    }

}
