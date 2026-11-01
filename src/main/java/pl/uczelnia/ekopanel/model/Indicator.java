package pl.uczelnia.ekopanel.model;

public record Indicator(String code, String name, String unit, String category) {
    @Override
    public String toString() {
        return code + " (" + unit + ") — " + name;
    }
}
