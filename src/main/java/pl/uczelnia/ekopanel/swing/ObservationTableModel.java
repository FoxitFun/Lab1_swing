package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObservationTableModel extends AbstractTableModel {
    public static final int COL_YEAR = 0;
    public static final int COL_REGION_CODE = 1;
    public static final int COL_REGION_NAME = 2;
    public static final int COL_INDICATOR = 3;
    public static final int COL_VALUE = 4;
    public static final int COL_UNIT = 5;
    public static final int COL_INCLUDED = 6;

    private static final String[] COLUMNS = {
            "Rok", "Kod reg.", "Województwo", "Wskaźnik", "Wartość", "Jedn.", "Analiza"
    };
    private static final Class<?>[] COLUMN_CLASSES = {
            Integer.class, String.class, String.class, String.class,
            BigDecimal.class, String.class, Boolean.class
    };

    private List<Observation> observations = List.of();
    private Map<String, String> regionNames = Map.of();
    private Map<String, String> indicatorUnits = Map.of();

    public void setDictionaries(List<Region> regions, List<Indicator> indicators) {
        Map<String, String> names = new HashMap<>();
        for (Region region : regions) {
            names.put(region.code(), region.name());
        }
        Map<String, String> units = new HashMap<>();
        for (Indicator indicator : indicators) {
            units.put(indicator.code(), indicator.unit());
        }
        this.regionNames = names;
        this.indicatorUnits = units;
        fireTableDataChanged();
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations == null ? List.of() : List.copyOf(observations);
        fireTableDataChanged();
    }

    public Observation getRow(int modelRow) {
        return observations.get(modelRow);
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public String regionName(String code) {
        return regionNames.getOrDefault(code, "");
    }

    @Override
    public int getRowCount() {
        return observations.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Observation row = observations.get(rowIndex);
        return switch (columnIndex) {
            case COL_YEAR -> row.year();
            case COL_REGION_CODE -> row.regionCode();
            case COL_REGION_NAME -> regionNames.getOrDefault(row.regionCode(), "");
            case COL_INDICATOR -> row.indicatorCode();
            case COL_VALUE -> row.value();
            case COL_UNIT -> indicatorUnits.getOrDefault(row.indicatorCode(), "");
            case COL_INCLUDED -> row.included();
            default -> null;
        };
    }
}
