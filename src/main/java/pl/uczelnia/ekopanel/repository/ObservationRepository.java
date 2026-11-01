package pl.uczelnia.ekopanel.repository;

import java.util.List;
import pl.uczelnia.ekopanel.model.*;

public interface ObservationRepository {
    List<Region> findAllRegions();
    List<Indicator> findAllIndicators();
    List<Observation> findAllObservations();
    Observation save(Observation observation);

    void delete(long id);

    void replaceDataset(List<Region> regions, List<Indicator> indicators, List<Observation> observations);
}
