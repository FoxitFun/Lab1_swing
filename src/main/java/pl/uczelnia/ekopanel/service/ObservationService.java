
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

}
