# Lab 1 — EkoPanel GUS BDL w Swing (wersja ulepszona)

> Przepisana wersja polecenia z [oryginalnej specyfikacji](https://po2-java.lazysolutions.pl/2026-2027/lab1/v2/README.html).
> Wymagania są te same. Dodano to, czego oryginał nie mówi: **skąd biorą się dane, kto kogo wywołuje
> i kiedy trzeba coś odświeżyć**. Każde zadanie kończy się punktem kontrolnym „✅ Sprawdź”.

---

## 0. Zanim zaczniesz: jak ta aplikacja jest zbudowana

### 0.1. Warstwy

Aplikacja ma cztery warstwy. Każda rozmawia **tylko z warstwą bezpośrednio pod sobą**:

| Warstwa | Pakiet | Odpowiada za | Nie wie nic o… |
|---|---|---|---|
| Model | `model` | Kształt danych (rekordy `Region`, `Indicator`, `Observation`, `Statistics`) | czymkolwiek innym |
| Repozytorium | `repository` | **Przechowywanie** danych (na razie listy w pamięci) | walidacji, oknach |
| Serwis | `service` | **Reguły**: walidacja, klucze obce, duplikaty, statystyka | oknach, Swingu |
| Interfejs | `swing`, `io` | Wyświetlanie, reagowanie na kliknięcia, czytanie plików | tym, jak dane są przechowywane |

### 0.2. Przepływ danych

```
┌──────────────────────────────┐
│ MemoryObservationRepository  │  jedyne miejsce, gdzie leży "prawdziwa" lista
│   private List<Observation>  │
└──────────────┬───────────────┘
               │ findAllObservations() zwraca List.copyOf(...)  (kopia tylko do odczytu)
┌──────────────▼───────────────┐
│ ObservationService           │  sprawdza reguły, potem przekazuje dalej
└──────────────┬───────────────┘
               │ Main tworzy serwis i przekazuje go: new MainFrame(service)
┌──────────────▼───────────────┐
│ MainFrame                    │  trzyma REFERENCJĘ do serwisu (nie listę!)
└──────────────┬───────────────┘
               │ tableModel.setObservations(service.findAllObservations())
┌──────────────▼───────────────┐
│ ObservationTableModel        │  trzyma kopię do wyświetlenia
└──────────────┬───────────────┘
               │ JTable sam pyta: getRowCount(), getValueAt(row, col)
┌──────────────▼───────────────┐
│ JTable                       │  niczego nie przechowuje, tylko rysuje
└──────────────────────────────┘
```

### 0.3. Trzy zasady, które rozwiązują 90% wątpliwości

1. **Interfejs nigdy nie trzyma własnej „prawdy”.** Po każdej zmianie (zapis, usunięcie, import)
   pobierasz świeżą listę z serwisu: `service.findAllObservations()`. Nie przekazujesz listy „na stałe”.
2. **Komponenty Swing nie przechowują danych, tylko pytają model.** Żeby zmienić tabelę, zmieniasz
   model i wywołujesz `fireTableDataChanged()`. To samo dotyczy `JComboBox`: po zmianie słowników
   trzeba mu dać nowy `DefaultComboBoxModel`.
3. **Każda akcja użytkownika ma ten sam cykl:**
   `kliknięcie → listener → service.coś(...) → refresh…() → komponenty się przerysowują`.
   Błędy z serwisu (`IllegalArgumentException`) łapiesz w listenerze i pokazujesz użytkownikowi.

### 0.4. Szkielet `MainFrame`, do którego będziesz dopisywać

Oryginał każe dopisywać wszystko do konstruktora, który rośnie do kilkuset linii. Lepiej od początku
podzielić go na metody. Nie musisz wypełniać wszystkiego od razu: puste metody z tego szkieletu
uzupełniasz w kolejnych zadaniach (numer zadania w komentarzu).

```java
public class MainFrame extends JFrame {
    // --- zależności
    private final ObservationService service;                        // 1.4
    private final StatisticsService statisticsService = new StatisticsService(); // 1.5

    // --- elementy okna (pola, bo używa ich wiele metod)
    private final JLabel statusLabel = new JLabel("Gotowe.");       // 1.4
    private final CardLayout cardLayout = new CardLayout();          // 1.4
    private final JPanel cardPanel = new JPanel(cardLayout);         // 1.4
    private final KpiCardPanel countCard = new KpiCardPanel("Liczba obserwacji"); // 1.5 (+3 kolejne)
    private final ObservationTableModel tableModel = new ObservationTableModel(); // 1.6
    private final JTable table = new JTable(tableModel);             // 1.6
    private Long editedId = null;                                    // 1.7

    public MainFrame(ObservationService service) {
        this.service = service;
        // ustawienia okna (1.4)
        // root.add(buildNorth(), NORTH); cardPanel.add(buildDataView(), "DATA"); ... (1.4–1.11)
        // setJMenuBar(buildMenuBar());                                (1.9)
        // reloadDictionaries(); refreshTable();                       (1.6)
    }

    private JComponent buildNorth()          { /* 1.4 + 1.5 */ }
    private JComponent buildDataView()       { /* 1.6 */ }
    private JComponent buildChartView()      { /* 1.11 */ }
    private JComponent buildCalculatorView() { /* 1.11 */ }
    private JMenuBar   buildMenuBar()        { /* 1.9 */ }

    public  void showStatus(String message)  { /* 1.4 */ }
    private void reloadDictionaries()        { /* 1.6: wypełnia wszystkie JComboBox ze słowników */ }
    private void refreshTable()              { /* 1.6: tableModel.setObservations(service.findAllObservations()) */ }
    public  void refreshKpiCards()           { /* 1.5, poprawiane w 1.7 */ }
    private void saveObservation()           { /* 1.7 */ }
    private void deleteObservation()         { /* 1.7 */ }
    private void importPackage()             { /* 1.10 */ }
}
```

---

## Zadanie 1.1. Projekt i pakiet danych

**Cel:** działający projekt Maven z bibliotekami i plikami danych.

1. Rozpakuj projekt startowy do `PO2-EkoPanel/ekopanel-swing`, otwórz `pom.xml` w IntelliJ, ustaw JDK 27.
2. Uruchom `pl.uczelnia.lab1.Main`, kliknij **Przywitaj**. To tylko test środowiska; w zadaniu 1.4
   zawartość `main()` zostanie zastąpiona uruchomieniem EkoPanelu.
3. W `pom.xml`: `artifactId` → `ekopanel-swing`, dodaj FlatLaf 3.7 (zostaw jSystemThemeDetector 3.9),
   zrób **Reload All Maven Projects**.
4. Utwórz pakiety `pl.uczelnia.ekopanel.model`, `.repository`, `.service`, `.io`, `.swing`
   w `src/main/java`.
5. Pliki CSV rozpakuj do `ekopanel-swing/data/`.

**⚠️ Pułapki**
- **Katalog roboczy.** Ścieżka `Path.of("data")` jest liczona względem katalogu, z którego startuje
  program (w IntelliJ: *Run → Edit Configurations → Working directory*). Ustaw go na katalog
  z `pom.xml`, inaczej `data/` nie zostanie znalezione.
- W paczce są **4** pliki testowe, nie 3: `observations-bledne.csv`, `-duplikat.csv`,
  `-brak-klucza.csv` (błędy w wierszu 77) oraz `observations-braki.csv` (pusta wartość w wierszu 32;
  oryginał go nie opisuje).

**✅ Sprawdź:** projekt się kompiluje; `data/` zawiera `regions.csv` (5 wierszy danych),
`indicators.csv` (3), `observations.csv` (75).

---

## Zadanie 1.2. Model danych i `BigDecimal`

**Cel:** trzy rekordy modelu oraz dwie klasy pomocnicze, które zamieniają tekst na liczbę i sprawdzają poprawność.

### Rekordy (`model`, każdy w osobnym pliku)

```java
public record Region(String code, String name, String macroregion) {
    @Override public String toString() { return code + " — " + name; }   // długi myślnik „—”
}
public record Indicator(String code, String name, String unit, String category) {
    @Override public String toString() { return code + " (" + unit + ") — " + name; }
}
public record Observation(Long id, int year, String regionCode,
                          String indicatorCode, BigDecimal value, boolean included) {}
```

Dlaczego `toString()`? `JComboBox<Region>` wyświetla to, co zwraca `toString()`, więc w liście
rozwijanej zobaczysz „PL82 — Podkarpackie”.

Dlaczego `Long id`, a nie `long`? Bo **nowa** obserwacja jeszcze nie ma identyfikatora: `id == null`
znaczy „dodaj”, `id != null` znaczy „edytuj”. Nadaje go repozytorium.

### `service/Numbers.parseDecimal(String text)`

```java
String cleaned = text.strip().replace(" ", "").replace(" ", "").replace(',', '.');
return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
// NumberFormatException lub NullPointerException → IllegalArgumentException("Wartość musi być poprawną liczbą dziesiętną.")
```

**⚠️ Pułapka:** polski format liczb (`6 562,80`) używa **spacji niełamliwej** (` `), a nie zwykłej.
Samo `replace(" ", "")` jej nie usunie.

### `service/ObservationValidator.validate(year, regionCode, indicatorCode, value, indicator)`

Metoda **nic nie zwraca**. Albo przechodzi po cichu, albo rzuca `IllegalArgumentException`.
Sprawdzaj **w tej kolejności** (pierwszy niespełniony warunek wygrywa):

| # | Warunek błędu | Komunikat |
|---|---|---|
| 1 | `year < 1990 \|\| year > 2035` | Rok musi być z zakresu 1990–2035. |
| 2 | `regionCode == null \|\| !regionCode.strip().matches("PL\\d{2}")` | Kod regionu musi mieć format PL i dwie cyfry (np. PL82). |
| 3 | `indicatorCode == null \|\| !indicatorCode.strip().matches("[A-Z][A-Z0-9_]{1,29}")` | Kod wskaźnika musi składać się z wielkich liter, cyfr i podkreśleń. |
| 4 | `value == null` | Wartość nie może być pusta. |
| 5 | jednostka `%` i wartość poza 0–100 (przez `compareTo`) | Stopa procentowa musi być z zakresu 0–100. |
| 6 | jednostka `PLN` i wartość < 0 | Wartość kwotowa w PLN nie może być ujemna. |

Parametr `indicator` może być `null`. Wtedy warunki 5–6 są pomijane, bo nie znamy jednostki.
Serwis zawsze przekazuje prawdziwy wskaźnik ze słownika.

**Dlaczego `compareTo`, a nie `equals`?** `new BigDecimal("5.0").equals(new BigDecimal("5.00"))`
zwraca `false` (inna skala). `compareTo` porównuje samą wartość.

**✅ Sprawdź:** `Numbers.parseDecimal(" 5,8 ")` → `5.80`; `parseDecimal("abc")` → wyjątek z komunikatem
z tabeli; `validate(2025, "PL82", "unemp", ...)` → błąd nr 3.

---

## Zadanie 1.3. Repozytorium i serwis

**Cel:** jedno miejsce przechowywania danych (repozytorium) i jedno miejsce pilnowania reguł (serwis).

### Interfejs `repository/ObservationRepository`

```java
List<Region> findAllRegions();
List<Indicator> findAllIndicators();
List<Observation> findAllObservations();
Observation save(Observation observation);
void delete(long id);
void replaceDataset(List<Region> regions, List<Indicator> indicators, List<Observation> observations);
```

### `MemoryObservationRepository`

- Pola: `private List<Region> regions`, `indicators`, `observations`, `private long nextId = 1`.
- Konstruktor: wypełnij `regions` 5 województwami (PL82, PL21, PL22, PL91, PL41) i `indicators`
  3 wskaźnikami (UNEMP_RATE, AVG_WAGE, INV_PER_CAPITA), takimi jak w plikach CSV. Obserwacje: pusta lista.
- `findAll…()` zwracają `List.copyOf(...)`. Dzięki temu nikt z zewnątrz nie zmieni list repozytorium.
- `save()`:
  - `id == null` → utwórz **nowy** rekord z `nextId++` (rekordu nie da się zmienić, więc tworzysz kopię), dodaj, zwróć go;
  - `id != null` → znajdź indeks rekordu o tym id i podmień przez `set(i, observation)`;
    jeśli nie ma, rzuć `IllegalArgumentException("Nie znaleziono obserwacji.")`.
- `delete(id)`: `removeIf(o -> o.id() == id)`; jeśli zwróciło `false`, rzuć ten sam wyjątek.
- `replaceDataset(...)`: **najpierw** zbuduj trzy nowe listy (obserwacje z nowymi id od 1),
  **potem** przypisz je do pól. Dzięki temu, jeśli w połowie poleci wyjątek, stare dane zostaną nietknięte.

### `ObservationService`

Konstruktor: `public ObservationService(ObservationRepository repository)`.

Metody, których potrzebuje interfejs. Oryginał wymienia tylko `save` i `replaceDataset`, ale okno
w zadaniach 1.6–1.7 korzysta z wszystkich poniższych:

| Metoda | Co robi |
|---|---|
| `findAllRegions()`, `findAllIndicators()`, `findAllObservations()` | przekazuje wywołanie do repozytorium |
| `save(Observation o)` | 1) region istnieje? (`Nieznany kod regionu: X`) 2) wskaźnik istnieje? (`Nieznany kod wskaźnika: X`) 3) `ObservationValidator.validate(...)` z tym wskaźnikiem 4) czy nie ma innej obserwacji z tą samą trójką rok+region+wskaźnik (pomijając rekord o tym samym id) → `Obserwacja dla tego roku, regionu i wskaźnika już istnieje.` 5) `repository.save(o)` |
| `delete(long id)` | przekazuje do repozytorium |
| `replaceDataset(regions, indicators, observations)` | 1) kody regionów unikalne 2) kody wskaźników unikalne 3) dla każdej obserwacji: klucze obce, walidator, duplikat (klucz `year + "\|" + region + "\|" + indicator` w `HashSet<String>`) 4) dopiero wtedy `repository.replaceDataset(...)` |

Komunikaty dla powtórzonych kodów słownika nie są narzucone; np. `Powtórzony kod regionu: PL82`.

**Kto tworzy obiekty?** `Main`:

```java
ObservationRepository repository = new MemoryObservationRepository();
ObservationService service = new ObservationService(repository);
new MainFrame(service).setVisible(true);
```

**✅ Sprawdź (np. w tymczasowym `main`):** `save` z `id = null` zwraca rekord z `id = 1`; drugi `save`
z tą samą trójką rzuca błąd duplikatu; `save` z regionem `PL99` rzuca „Nieznany kod regionu: PL99”.

---

## Zadanie 1.4. Szkielet okna i menedżery układu

**Cel:** jedno okno z paskiem nawigacji, przełączanymi widokami i paskiem statusu.

1. `Main.main()`: wszystko w `SwingUtilities.invokeLater(() -> { ... })`. Kolejność:
   `FlatLightLaf.setup()`, potem utworzenie repozytorium i serwisu, potem `new MainFrame(service).setVisible(true)`.
2. `MainFrame`: tytuł `EkoPanel — GUS BDL`, `EXIT_ON_CLOSE`, rozmiar 1080 × 720, minimum 480 × 360,
   `setLocationRelativeTo(null)`.
3. Główny panel: `new JPanel(new BorderLayout(12, 12))` z `createEmptyBorder(16, 16, 16, 16)`.

```
┌──────────────────────── BorderLayout ────────────────────────┐
│ NORTH:  [Dane] [Wykres] [Kalkulator] [Szczegóły regionu…]    │ ← w 1.5 dojdą tu karty KPI
│ CENTER: cardPanel (CardLayout): "DATA" | "CHART" | "CALCULATOR" │
│ SOUTH:  statusLabel "Gotowe."                                │
└──────────────────────────────────────────────────────────────┘
```

4. **Ćwiczenie (nie zostaje w kodzie):** ustaw pasek przycisków kolejno na `FlowLayout(LEFT, 8, 8)`,
   `BoxLayout(Y_AXIS)` i obserwuj zwężanie okna do 480 px. **W kodzie zostaje** `GridLayout(1, 4, 8, 8)`.
5. Trzy widoki to na razie puste `JPanel`, dodane do `cardPanel` pod kluczami `"DATA"`, `"CHART"`,
   `"CALCULATOR"`. Przyciski: `cardLayout.show(cardPanel, "DATA")` itd.
6. `public void showStatus(String message) { statusLabel.setText(message); }`

**⚠️ Pułapka:** `BorderLayout.NORTH` mieści **jeden** komponent. Skoro w 1.5 pod przyciskami mają
dojść karty KPI, od razu zrób panel-opakowanie, np. `JPanel north = new JPanel(new BorderLayout(8, 8))`
z przyciskami w jego `NORTH` i kartami w `CENTER`.

**✅ Sprawdź:** przyciski przełączają widoki bez otwierania nowych okien; status pokazuje „Gotowe.” (z kropką).

---

## Zadanie 1.5. Komponent `KpiCardPanel` i statystyka

**Cel:** wielokrotnego użytku „karta” z liczbą oraz serwis liczący statystyki.

### `swing/KpiCardPanel extends JPanel`

- Konstruktor `KpiCardPanel(String title)`: `BoxLayout(this, Y_AXIS)`; ramka =
  `createCompoundBorder(createLineBorder(kolor), createEmptyBorder(10, 12, 10, 12))`.
- Trzy etykiety: **tytuł** zwykły 12 pt, **wartość pogrubiona 18 pt** (początkowo `"—"`), **podtytuł** 11 pt.
- `public void setMetric(String formattedValue, String subtitle)`: ustawia wartość i podtytuł.
  Podtytuł to dowolny krótki opis, np. jednostka albo `d = 1,23 p.p.`.

W `buildNorth()` dodaj panel `GridLayout(1, 4, 8, 8)` z czterema kartami:
„Liczba obserwacji”, „Średnia (x̄)”, „Mediana (Me)”, „Odchylenie (s / d)”.

### `model/Statistics` i `service/StatisticsService.calculate(List<Observation> rows)`

```java
public record Statistics(int count, BigDecimal min, BigDecimal max, BigDecimal mean,
                         BigDecimal median, BigDecimal sampleSd, BigDecimal meanAbsDeviation) {}
```

Algorytm krok po kroku (x₁…xₙ to wartości po odfiltrowaniu):

1. Weź tylko obserwacje z `included() == true` i `value() != null`. Posortuj wartości rosnąco.
2. `n == 0` → zwróć `new Statistics(0, null, null, null, null, null, null)`.
3. `min` = pierwsza, `max` = ostatnia wartość.
4. **Średnia robocza** `x̄ = (Σ xᵢ) / n` ze skalą **4** (`divide(n, 4, HALF_UP)`). Do wyniku: `setScale(2, HALF_UP)`.
5. **Mediana:** `n` nieparzyste → element środkowy; `n` parzyste → średnia dwóch środkowych (`divide(2, 2, HALF_UP)`).
6. **Odchylenie przeciętne** `d = (Σ |xᵢ − x̄|) / n`, liczone od średniej **roboczej** (skala 4), wynik skala 2.
7. **Odchylenie standardowe z próby** `s = √( Σ (xᵢ − x̄)² / (n − 1) )`:
   - `n == 1` → `s = null` (dzielenie przez zero);
   - wariancję dziel ze skalą 8, pierwiastek: `variance.sqrt(MathContext.DECIMAL64)`, wynik skala 2.

### `service/StatisticsCheck` (zwykła klasa z `main`)

Dla każdego przypadku wywołaj `calculate(...)` i porównaj każde pole przez `compareTo`
(dla `null` porównaj osobno). Przy niezgodności rzuć `AssertionError` z nazwą testu.

| Wejście | count | min | max | mean | median | s | d |
|---|---|---|---|---|---|---|---|
| `List.of()` | 0 | null | null | null | null | null | null |
| 1 rekord, `included = false` | 0 | null | null | null | null | null | null |
| 5.50 | 1 | 5.50 | 5.50 | 5.50 | 5.50 | **null** | 0.00 |
| 2.00, 4.00 | 2 | 2.00 | 4.00 | 3.00 | 3.00 | 1.41 | 1.00 |
| 7.25 ×3 | 3 | 7.25 | 7.25 | 7.25 | 7.25 | 0.00 | 0.00 |
| 9.10, 8.20, 7.50, 6.80, 6.10 | 5 | 6.10 | 9.10 | 7.54 | 7.50 | 1.17 | 0.89 |

### `refreshKpiCards()` (wersja tymczasowa)

Na tym etapie **nie ma jeszcze tabeli**, więc licz ze wszystkich obserwacji:

```java
Statistics s = statisticsService.calculate(service.findAllObservations());
countCard.setMetric(String.valueOf(s.count()), "");
meanCard.setMetric(format(s.mean()), "");   // ...
```

Napisz pomocniczą metodę `format(BigDecimal)`: dla `null` zwraca `"—"`, w przeciwnym razie
`NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"))` z 2 miejscami po przecinku (daje „4,73”).
Nie używaj `String.valueOf`, bo da „4.73” albo „null”.

**⚠️ Pułapka:** nie odwołuj się w tym zadaniu do `tableModel`. Jeśli zadeklarujesz pole bez `new`,
program padnie z `NullPointerException` przy starcie.

**✅ Sprawdź:** `StatisticsCheck` przechodzi wszystkie 6 testów; okno pokazuje 4 karty z `0` i `—`.

---

## Zadanie 1.6. Formularz i tabela

**Cel:** lewa część to formularz, prawa to tabela z filtrami, rozdzielone suwakiem.

```
┌─ JSplitPane(HORIZONTAL_SPLIT, resizeWeight 0.32) ─────────────────────────┐
│ JScrollPane( formularz )      │ [filtr wskaźnika ▼] [szukaj…] [Importuj] │
│  Rok:          [2025      ]   │ ┌──────────────────────────────────────┐ │
│  Województwo:  [PL82 — … ▼]   │ │ JScrollPane( JTable )                │ │
│  Wskaźnik GUS: [UNEMP_… ▼]    │ │                                      │ │
│  Wartość:      [5,80      ]   │ │                                      │ │
│  ☑ Uwzględnij w analizie      │ └──────────────────────────────────────┘ │
│  [Nowa] [Zapisz] [Usuń]       │                                          │
└───────────────────────────────┴──────────────────────────────────────────┘
```

### Formularz (`GridBagLayout`)

- Kolumna 0 (etykiety): `weightx = 0`, `fill = NONE`. Kolumna 1 (pola): `weightx = 1.0`,
  `fill = HORIZONTAL`. Ćwiczenie: zacznij od `weightx = 0` w kolumnie 1 i zobacz, że pola się nie rozciągają.
- Pomocnicza metoda `addFormField(panel, gbc, row, label, field)` oszczędzi powtórzeń.
- `JCheckBox` w wierszu 4 z `gridwidth = 2`. Przyciski w `GridLayout(1, 3, 8, 8)` pod polami.
- Całość w `JScrollPane` z `HORIZONTAL_SCROLLBAR_NEVER`.

### `ObservationTableModel extends AbstractTableModel`: **7 kolumn**

(oryginał pisze „6 kolumn”, ale tabela ma 7)

| # | Nagłówek | Wartość | `getColumnClass` |
|---|---|---|---|
| 0 | Rok | `row.year()` | `Integer.class` |
| 1 | Kod reg. | `row.regionCode()` | `String.class` |
| 2 | Województwo | nazwa z mapy regionów | `String.class` |
| 3 | Wskaźnik | `row.indicatorCode()` | `String.class` |
| 4 | Wartość | `row.value()` | `BigDecimal.class` |
| 5 | Jedn. | jednostka z mapy wskaźników | `String.class` |
| 6 | Analiza | `row.included()` | `Boolean.class` (JTable pokaże checkbox) |

Metody modelu (oryginał wymienia tylko część):

```java
private List<Observation> observations = List.of();          // nigdy null!
private Map<String, String> regionNames = Map.of();         // kod → nazwa
private Map<String, String> indicatorUnits = Map.of();      // kod → jednostka

public void setObservations(List<Observation> list) { observations = List.copyOf(list); fireTableDataChanged(); }
public void setDictionaries(List<Region> r, List<Indicator> i) { /* zbuduj obie mapy */ fireTableDataChanged(); }
public Observation getRow(int modelRow) { return observations.get(modelRow); }   // potrzebne w 1.7
// + getRowCount, getColumnCount, getColumnName, getColumnClass, getValueAt (switch po kolumnie)
```

**Skąd mapy?** `MainFrame` wywołuje `tableModel.setDictionaries(service.findAllRegions(), service.findAllIndicators())`
przy starcie i po każdym imporcie (w `reloadDictionaries()`).

### Tabela i filtry

- `TableRowSorter<ObservationTableModel> sorter = new TableRowSorter<>(tableModel); table.setRowSorter(sorter);`
- Renderer kolumny Wartość: podklasa `DefaultTableCellRenderer` z nadpisanym `setValue(...)`,
  formatującym przez `NumberFormat` pl-PL (2 miejsca), wyrównanie do prawej.
- `filterIndicatorCombo` jest typu `JComboBox<Object>`: pierwszy element to tekst `"Wszystkie wskaźniki"`,
  dalej obiekty `Indicator`; domyślnie wybrany UNEMP_RATE.
- `filterField` ma podpowiedź „Szukaj województwa lub kodu” (`putClientProperty("JTextField.placeholderText", ...)`).

### `reloadDictionaries()` i `refreshTable()`

```java
private void reloadDictionaries() {
    // dla każdego JComboBox: nowy DefaultComboBoxModel z service.findAll…(), ustaw domyślny wybór
    tableModel.setDictionaries(service.findAllRegions(), service.findAllIndicators());
}
private void refreshTable() {
    tableModel.setObservations(service.findAllObservations());
    refreshKpiCards();
}
```

Wywołaj obie na końcu konstruktora.

**✅ Sprawdź:** okno startuje bez wyjątków; listy rozwijane mają domyślnie PL82 i UNEMP_RATE; tabela jest pusta.

---

## Zadanie 1.7. Zapis, edycja, usuwanie, filtrowanie

**Cel:** pełne operacje CRUD działające poprawnie także przy posortowanej i przefiltrowanej tabeli.

### Kluczowe pojęcie: indeks widoku ≠ indeks modelu

Po sortowaniu wiersz nr 0 **na ekranie** może być elementem nr 17 **na liście w modelu**. Dlatego:

```java
int viewRow  = table.getSelectedRow();                  // indeks na ekranie
int modelRow = table.convertRowIndexToModel(viewRow);   // indeks na liście
Observation row = tableModel.getRow(modelRow);
```

### Zaznaczenie wiersza → formularz

```java
table.getSelectionModel().addListSelectionListener(e -> {
    if (e.getValueIsAdjusting()) return;        // ignoruj zdarzenia „w trakcie” przeciągania
    // brak zaznaczenia: editedId = null, deleteButton.setEnabled(false), koniec
    // jest zaznaczenie: editedId = row.id(); wpisz rok, wybierz region/wskaźnik w combo,
    //                   wartość (toPlainString().replace('.', ',')), checkbox; deleteButton.setEnabled(true)
});
```

### Zapisz

1. `Integer.parseInt(yearField.getText().strip())`, wybrany `Region` i `Indicator`,
   `Numbers.parseDecimal(valueField.getText())`, `includedBox.isSelected()`.
2. `service.save(new Observation(editedId, year, region.code(), indicator.code(), value, included))`.
   `editedId == null` oznacza nowy rekord, w przeciwnym razie edycję.
3. `refreshTable()`, `showStatus("Zapisano obserwację.")`.
4. Cały blok w `try { ... } catch (IllegalArgumentException e)`. W `catch`: `showStatus(...)`
   oraz `JOptionPane.showMessageDialog(this, e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE)`.
   `NumberFormatException` z `parseInt` też jest `IllegalArgumentException`.

### Nowa

`table.clearSelection()`, `editedId = null`, wyczyść pola.

### Usuń

Przycisk domyślnie nieaktywny. Po kliknięciu:
`showConfirmDialog(this, "Usunąć obserwację 2025 / PL82 / UNEMP_RATE?", ...)` z prawdziwymi polami
rekordu; przy `YES_OPTION` wywołaj `service.delete(editedId)` i `refreshTable()`.

### Filtr

```java
sorter.setRowFilter(new RowFilter<>() {
    public boolean include(Entry<? extends ObservationTableModel, ? extends Integer> entry) {
        Observation o = entry.getModel().getRow(entry.getIdentifier());  // identifier = indeks w modelu
        // 1) jeśli w filterIndicatorCombo wybrany Indicator i jego kod ≠ o.indicatorCode() → false
        // 2) tekst z filterField (toLowerCase(Locale.ROOT)) zawarty w nazwie lub kodzie regionu → true
    }
});
```

Po zmianie combo lub tekstu (`ActionListener` / `DocumentListener`) wywołaj `sorter.sort()`.

### Poprawka `refreshKpiCards()`: KPI liczone z **widocznych** wierszy

Oczekiwane wyniki w 1.10 (N = 25 przy filtrze UNEMP_RATE) pokazują, że karty mają liczyć tylko to,
co widać w tabeli:

```java
List<Observation> visible = new ArrayList<>();
for (int v = 0; v < table.getRowCount(); v++)                 // getRowCount() tabeli = po filtrze
    visible.add(tableModel.getRow(table.convertRowIndexToModel(v)));
Statistics s = statisticsService.calculate(visible);
```

Żeby karty same się odświeżały po każdej zmianie filtra, sortowania i danych:
`sorter.addRowSorterListener(e -> refreshKpiCards());`. Wtedy `refreshTable()` nie musi już ich wołać.

**✅ Sprawdź (scenariusz z oryginału):** dodaj 2025 / PL82 / UNEMP_RATE / 5,80 → zaznacz → zmień
na 5,60 → Zapisz (wiersz się zmienia, nie dubluje) → sortuj po Wartość → wpisz „pod” w filtr
→ zaznacz → Usuń. Usuwa się właściwy rekord.

---

## Zadanie 1.8. Okno `RegionDetailsDialog`

**Cel:** modalne okno z podsumowaniem jednego województwa.

```java
public RegionDetailsDialog(Frame owner, Region region, List<Indicator> indicators,
                           List<Observation> regionObservations) {
    super(owner, "Szczegóły regionu: " + region.name(), true);   // true = modalne
```

**Skąd argumenty?** Wywołuje je `MainFrame`:

```java
Region region = /* region zaznaczonego wiersza tabeli, a gdy brak zaznaczenia: regionCombo.getSelectedItem() */;
List<Observation> regionObs = service.findAllObservations().stream()
        .filter(o -> o.regionCode().equals(region.code())).toList();
RegionDetailsDialog d = new RegionDetailsDialog(this, region, service.findAllIndicators(), regionObs);
d.setSize(640, 340);
d.setLocationRelativeTo(this);
d.setVisible(true);   // w oknie modalnym ta linia czeka, aż dialog zostanie zamknięty
```

Zawartość dialogu (`BorderLayout`):
- **NORTH:** kod (`region.code()`), nazwa, makroregion.
- **CENTER:** `JTable` z `DefaultTableModel` (tu wystarczy gotowy model). Jeden wiersz na wskaźnik:
  Kod, Nazwa, Jednostka, Liczba lat, Średnia, Min, Max, Ostatnia wartość (2024).
  Statystyki policz **tym samym** `StatisticsService` na obserwacjach danego wskaźnika.
- **SOUTH:** przycisk Zamknij → `dispose()`.
- Escape: `getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);`

Podłącz pod przycisk „Szczegóły regionu…” oraz podwójne kliknięcie w tabeli
(`MouseListener`, `e.getClickCount() == 2`).

**✅ Sprawdź:** po imporcie danych dialog dla PL82 pokazuje dla UNEMP_RATE: 5 lat, średnia 7,54, min 6,10,
max 9,10, ostatnia 6,10. Escape zamyka okno.

---

## Zadanie 1.9. Menu, skróty i menu kontekstowe

**Cel:** każda operacja dostępna z menu, skrótu i prawego przycisku myszy.

Pomocnicza metoda oszczędzi dużo kodu:

```java
private JMenuItem menuItem(String text, int keyCode, ActionListener action) {
    JMenuItem item = new JMenuItem(text);
    item.setAccelerator(KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK));
    item.addActionListener(action);
    return item;
}
```

| Menu (mnemonik) | Pozycje → **istniejąca** metoda |
|---|---|
| Plik (`VK_P`) | Importuj pakiet GUS CSV… Ctrl+O → `importPackage()` · Nowa obserwacja Ctrl+N → `newObservation()` · Zapisz obserwację Ctrl+S → `saveObservation()` · separator · Zakończ Ctrl+Q → `dispatchEvent(new WindowEvent(this, WINDOW_CLOSING))` |
| Widok (`VK_W`) | Dane i tabela Ctrl+1 · Wykres serii czasowej Ctrl+2 · Kalkulator dynamiki Ctrl+3 → `cardLayout.show(...)` · separator · Szczegóły wybranego regionu… Ctrl+I → `openRegionDetails()` |
| Motyw (`VK_M`) | 3 × `JRadioButtonMenuItem` w jednym `ButtonGroup`: Systemowy, Jasny, Ciemny → `ThemeManager.apply(...)` (zad. 1.12; do tego czasu mogą nic nie robić) |
| Pomoc (`VK_O`) | O programie EkoPanel → `JOptionPane` z opisem danych i `System.getProperty("java.version")` |

Zasada: listener w menu **tylko wywołuje** metodę, którą już masz. Nie kopiuj logiki zapisu do menu.

Menu kontekstowe tabeli: `JPopupMenu` z pozycjami Edytuj w formularzu, Pokaż szczegóły regionu…,
Pokaż ten region i wskaźnik na wykresie, Usuń obserwację; `table.setComponentPopupMenu(popup)`.

**⚠️ Pułapka:** prawy przycisk **nie zaznacza** wiersza. Dodaj `MouseListener`, który w `mousePressed`
przy prawym przycisku robi `table.setRowSelectionInterval(row, row)` dla `table.rowAtPoint(e.getPoint())`.

**✅ Sprawdź:** Alt+P otwiera menu Plik; Ctrl+2 przełącza na wykres; prawy klik na wierszu zaznacza go i pokazuje menu.

---

## Zadanie 1.10. Import CSV w tle (`SwingWorker`)

**Cel:** wczytanie trzech plików bez zamrażania okna; błędny plik nie niszczy obecnych danych.

### `io/GusCsvPackageReader`

| Metoda | Nagłówek (wiersz 1) | Zwraca |
|---|---|---|
| `readRegions(Path)` | `code;name;macroregion` | `List<Region>` |
| `readIndicators(Path)` | `code;name;unit;category` | `List<Indicator>` |
| `readObservations(Path, Map<String,Region>, Map<String,Indicator>)` | `year;region_code;indicator_code;value;included` | `List<Observation>` (id = `null`) |

- Czytaj `Files.readAllLines(path, StandardCharsets.UTF_8)`. Linia `i` w liście to wiersz `i + 1` w pliku.
- Dziel `line.split(";", -1)` (`-1` zachowuje puste kolumny na końcu).
- W `readObservations` dla każdego wiersza: rok (`parseInt`), wartość (`Numbers.parseDecimal`, pusty
  tekst to `null`), `included` (`true`/`false`), klucze obce w mapach, `ObservationValidator.validate`,
  duplikat w `HashSet<String>`.
- **Dowolny** `IllegalArgumentException` w wierszu opakuj: `throw new IOException("Wiersz " + lineNumber + ": " + e.getMessage(), e);`

### Akcja importu w `MainFrame`

```java
private void importPackage() {
    JFileChooser chooser = new JFileChooser(/* katalog data/ */);
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
    Path obsPath = chooser.getSelectedFile().toPath();
    // pobierz bieżące słowniki z serwisu TUTAJ (w wątku EDT), na wypadek braku plików słownikowych
    setImportRunning(true);                       // wyłącz Zapisz i Importuj (przyciski + menu)
    showStatus("Wczytywanie pakietu GUS…");

    new SwingWorker<ImportResult, Void>() {
        protected ImportResult doInBackground() throws Exception {
            // WĄTEK W TLE: tylko czytanie plików. NIE dotykaj tu komponentów Swing ani serwisu.
            // regions.csv / indicators.csv w tym samym katalogu? wczytaj : użyj bieżących
            // zbuduj mapy kod → obiekt, wywołaj readObservations(...)
        }
        protected void done() {
            // ZNOWU WĄTEK EDT: tu wolno zmieniać okno
            setImportRunning(false);
            try {
                ImportResult r = get();           // rzuca ExecutionException, jeśli doInBackground rzucił
                service.replaceDataset(r.regions(), r.indicators(), r.observations());
                reloadDictionaries();
                refreshTable();
                showStatus("Wczytano 75 obserwacji (5 regionów, 3 wskaźniki).");  // z prawdziwych rozmiarów
            } catch (ExecutionException e) {
                showError(e.getCause().getMessage());   // „Wiersz 77: …”
            } catch (InterruptedException | IllegalArgumentException e) { ... }
        }
    }.execute();
}
```

`ImportResult` to mały rekord trzymający trzy listy: `record ImportResult(List<Region> regions, List<Indicator> indicators, List<Observation> observations) {}`.

Dlaczego dane się nie psują przy błędzie? Bo `service.replaceDataset` jest wołane **tylko** po
udanym odczycie, a repozytorium podmienia listy dopiero po zbudowaniu całości (1.3).

**✅ Sprawdź:**

| Plik | Wynik |
|---|---|
| `observations.csv` | 75 obserwacji. Filtr UNEMP_RATE: N = 25, x̄ = 4,73 %, Me = 4,30 %, s = 1,66 p.p., d = 1,23 p.p. Filtr AVG_WAGE: N = 25, x̄ = 6 562,80, Me = 6 490,00, s = 1 298,97, d = 1 046,91 PLN |
| `observations-bledne.csv` | Wiersz 77: Wartość musi być poprawną liczbą dziesiętną. Zostaje 75 |
| `observations-duplikat.csv` | Wiersz 77: duplikat 2020 / PL82 / UNEMP_RATE. Zostaje 75 |
| `observations-brak-klucza.csv` | Wiersz 77: Nieznany kod regionu: PL99. Zostaje 75 |
| `observations-braki.csv` | Wiersz 32: Wartość nie może być pusta. Zostaje 75 |

---

## Zadanie 1.11. Wykres i kalkulator dynamiki

### `swing/RateChartPanel extends JPanel`

Metoda `setSeries(Region region, Indicator indicator, List<Observation> rows)` zapamiętuje dane
i woła `repaint()`. **Rysowanie dzieje się wyłącznie w `paintComponent`**, które Swing wywołuje sam:

```java
@Override protected void paintComponent(Graphics g) {
    super.paintComponent(g);                       // czyści tło
    Graphics2D g2 = (Graphics2D) g.create();       // kopia, żeby nie psuć ustawień innym komponentom
    try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 1) tytuł: nazwa wskaźnika + [jednostka]
        // 2) osie
        // 3) przeliczenie: x z roku, y z row.value().doubleValue()  (oś Y ekranu rośnie W DÓŁ!)
        // 4) łamana kolorem #0F766E (w ciemnym motywie #5EEAD4)
    } finally {
        g2.dispose();
    }
}
```

Przeliczenie wartości na piksel: `y = top + height − (v − min) / (max − min) * height`.

Widok CHART: pasek z `chartRegionCombo` (domyślnie PL82) i `chartIndicatorCombo` (UNEMP_RATE).
Po zmianie któregokolwiek wybierz z `service.findAllObservations()` pasujące rekordy i wywołaj
`chartPanel.setSeries(...)`. Odświeżaj też wykres po imporcie.

### `service/ChangeCalculator`

| Metoda | Wzór | Kod |
|---|---|---|
| `difference(start, end)` | Δx = x₁ − x₀ | `end.subtract(start).setScale(2, HALF_UP)` |
| `relativeChange(start, end)` | r = (x₁ − x₀) / x₀ · 100% | jeśli `start` = 0, to `IllegalArgumentException("Nie można obliczyć zmiany względnej dla zera.")`; potem `end.subtract(start).multiply(100).divide(start, 2, HALF_UP)` |
| `dynamicsIndex(start, end)` | i = x₁ / x₀ · 100% | `end.multiply(100).divide(start, 2, HALF_UP)` (też sprawdź zero) |

Widok CALCULATOR: dwa pola tekstowe (x₀, x₁), przycisk „Oblicz”, etykieta wyniku. Wartości czytaj
przez `Numbers.parseDecimal`; znak „+” dopisz sam dla dodatnich.

**✅ Sprawdź:** 4520,00 → 6980,00: „Przyrost: +2460,00; tempo zmian: +54,42 %; indeks dynamiki: 154,42 %”.
9,10 → 6,10: „Przyrost: -3,00; tempo zmian: -32,97 %; indeks dynamiki: 67,03 %”.
Wykres PL82/UNEMP_RATE opada od 9,10 do 6,10.

---

## Zadanie 1.12. Responsywność, motywy, weryfikacja

### Responsywny układ

```java
getContentPane().addComponentListener(new ComponentAdapter() {
    public void componentResized(ComponentEvent e) { updateResponsiveLayout(); }
});
```

W `updateResponsiveLayout()` (szerokość = `getContentPane().getWidth()`):
- `< 760` → `splitPane.setOrientation(VERTICAL_SPLIT)`, dzielnik 0.45; w przeciwnym razie `HORIZONTAL_SPLIT`, 0.32.
- `< 680` → panel KPI `setLayout(new GridLayout(2, 2, 8, 8))`, inaczej `GridLayout(1, 4, 8, 8)`; potem `revalidate()`.
- Zmieniaj układ **tylko gdy próg faktycznie został przekroczony** (zapamiętaj poprzedni stan w polu),
  inaczej dzielnik będzie „skakał” przy każdym pikselu.
- `setDividerLocation(double)` działa dopiero, gdy panel ma rozmiar, więc wywołaj go w `SwingUtilities.invokeLater(...)`.

### Motywy

- `enum ThemeMode { SYSTEM, LIGHT, DARK }`
- `ThemeManager` (metody statyczne):
  - `init()` czyta zapisany tryb z `Preferences.userRoot().node("pl/uczelnia/ekopanel/swing")` i go stosuje;
  - `apply(ThemeMode)` zapisuje tryb w `Preferences`, wywołuje `FlatLightLaf.setup()` lub `FlatDarkLaf.setup()`
    (dla SYSTEM decyduje `OsThemeDetector.getDetector().isDark()`), a potem
    `for (Window w : Window.getWindows()) SwingUtilities.updateComponentTreeUI(w);` (to obejmuje też otwarte `JDialog`).
- W `Main`: `ThemeManager.init()` zaraz po `FlatLightLaf.setup()`.

**⚠️ Pułapki**
- `OsThemeDetector.removeListener(...)` w jSystemThemeDetector 3.9 na Windows rzuca `NullPointerException`,
  jeśli listener nie był wcześniej zarejestrowany. Rejestruj listener **raz** i nigdy go nie usuwaj;
  w samym listenerze sprawdzaj, czy aktywny tryb to SYSTEM.
- Listener detektora działa w **innym wątku**, więc zmianę motywu opakuj w `SwingUtilities.invokeLater`.
- Kolory ustawione „na sztywno” (np. `Color.BLACK` w ramce karty KPI) nie zmienią się z motywem.
  Bierz je z `UIManager.getColor(...)` i odśwież w nadpisanym `updateUI()`.
- Komunikaty `WARNING: A restricted method…` (FlatLaf) i `SLF4J: Failed to load class…` to nie błędy.
  Pierwszy znika po dodaniu VM option `--enable-native-access=ALL-UNNAMED`.

### Weryfikacja: `docs/lab1.md`

12 prób = 3 skale (100%, 150%, 200%) × 2 rozmiary (480 × 360, 1080 × 720) × 2 motywy.
Skalę wymusisz bez zmieniania ustawień Windows przez VM option `-Dsun.java2d.uiScale=1.5`.
Dla każdej próby zapisz: orientację `JSplitPane`, układ KPI (1×4 / 2×2), uwagi (np. ucięte elementy).

**Do oddania:** projekt, `data/`, `docs/lab1.md`, zrzuty: układ szeroki i wąski, `RegionDetailsDialog`,
motyw jasny i ciemny.

---

## Ściągawka: „gdzie to wywołać?”

| Zdarzenie | Co wywołać |
|---|---|
| Start okna | `reloadDictionaries()`, `refreshTable()` |
| Zapis / usunięcie | `service.save/delete(...)`, potem `refreshTable()` |
| Import udany | `service.replaceDataset(...)`, `reloadDictionaries()`, `refreshTable()` |
| Zmiana filtra | `sorter.sort()` (KPI odświeżą się same przez `RowSorterListener`) |
| Zmiana combo wykresu | `refreshChart()` |
| Zmiana motywu | `ThemeManager.apply(...)`, `chartPanel.repaint()` |
| Zmiana rozmiaru okna | `updateResponsiveLayout()` |
