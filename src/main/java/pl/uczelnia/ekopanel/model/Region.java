package pl.uczelnia.ekopanel.model;

public record Region(String code, String name, String macroregion) {
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
