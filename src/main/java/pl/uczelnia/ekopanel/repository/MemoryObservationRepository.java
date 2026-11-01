package pl.uczelnia.ekopanel.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;

public class MemoryObservationRepository implements ObservationRepository{
    private List<Region> regions;
    private List<Indicator> indicators;
    private List<Observation> observations;
    private long nextId = 1;

    public MemoryObservationRepository(){
        this.regions = new ArrayList<>(List.of(
                new Region("PL82","Podkarpackie","Wschodni"),
                new Region("PL21","Małopolskie","Południowy"),
                new Region("PL22","Śląskie","Południowy"),
                new Region("PL91","Mazowieckie","Województwo mazowieckie"),
                new Region("PL41","Wielkopolskie","Północno-zachodni")
        ));

        this.indicators = new ArrayList<>(List.of(
                new Indicator("UNEMP_RATE","Stopa bezrobocia rejestrowanego","%","Rynek pracy"),
                new Indicator("AVG_WAGE","Przeciętne miesięczne wynagrodzenie brutto","PLN","Wynagrodzenia"),
                new Indicator("INV_PER_CAPITA","Nakłady inwestycyjne na 1 mieszkańca","PLN","Inwestycje")
        ));

        this.observations = new ArrayList<>();
    }

    @Override
    public List<Region> findAllRegions(){
        return List.copyOf(regions);
    }

    @Override
    public List<Indicator> findAllIndicators(){
        return List.copyOf(indicators);
    }

    @Override
    public List<Observation> findAllObservations(){
        return List.copyOf(observations);
    }

    @Override
    public Observation save(Observation observation) {

        // Dodawanie nowej obserwacji
        if (observation.id() == null) {
            Observation newObservation = new Observation(
                    nextId++,
                    observation.year(),
                    observation.regionCode(),
                    observation.indicatorCode(),
                    observation.value(),
                    observation.included()
            );

            observations.add(newObservation);
            return newObservation;
        }

        // Edycja istniejącej obserwacji
        for (int i = 0; i < observations.size(); i++) {
            if (observations.get(i).id().equals(observation.id())) {
                observations.set(i, observation);
                return observation;
            }
        }

        // id edytowanej obeserwacji nie istnieje
        throw new IllegalArgumentException("Nie znaleziono obserwacji.");
    }

    @Override
    public void delete(long id) {
        boolean isRemoved = this.observations.removeIf(observation -> observation.id()==id);
        if (!isRemoved){
            throw new IllegalArgumentException("Nie znaleziono obserwacji.");
        }
    }
}
