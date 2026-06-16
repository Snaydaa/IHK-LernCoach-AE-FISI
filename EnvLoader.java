import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class EnvLoader {

    // Attribute
    private static final String DATEINAME = "schluessel.env";

    private static Map<String, String> zwischenspeicher;
    private static Path                dateiPfad;

    // Konstruktor
    private EnvLoader() {}

    public static String pflichtSchluesselLesen(String schluessel) {
        String wert = schluesselLesen(schluessel);
        if (wert == null || wert.isEmpty()) {
            throw new IllegalStateException(
                    schluessel + " nicht gefunden. Bitte " + DATEINAME + " prüfen."
            );
        }
        return wert;
    }

    public static synchronized String schluesselLesen(String schluessel) {
        String systemwert = System.getenv(schluessel);
        if (systemwert != null && !systemwert.isEmpty()) {
            return systemwert.trim();
        }

        if (zwischenspeicher == null) {
            zwischenspeicher = dateiLaden();
        }

        String dateiwert = zwischenspeicher.get(schluessel);
        if (dateiwert == null) {
            return null;
        }
        return dateiwert;
    }

    // Getter
    public static synchronized Path dateiPfadHolen() {
        if (zwischenspeicher == null) {
            zwischenspeicher = dateiLaden();
        }
        return dateiPfad;
    }

    public static synchronized boolean schluesselVorhanden(String schluessel) {
        String wert = schluesselLesen(schluessel);
        return wert != null && !wert.isEmpty();
    }

    private static Map<String, String> dateiLaden() {
        Map<String, String> werte = new HashMap<>();
        dateiPfad = null;

        Path pfad = dateiSuchen();
        if (pfad == null) {
            return werte;
        }

        dateiPfad = pfad;

        try (BufferedReader leser = Files.newBufferedReader(pfad, StandardCharsets.UTF_8)) {
            String zeile;
            while ((zeile = leser.readLine()) != null) {
                zeile = zeile.trim();

                if (zeile.isEmpty() || zeile.startsWith("#")) {
                    continue;
                }

                int gleichheitszeichen = zeile.indexOf('=');
                if (gleichheitszeichen <= 0) {
                    continue;
                }

                String schluessel = zeile.substring(0, gleichheitszeichen).trim();
                String wert       = zeile.substring(gleichheitszeichen + 1).trim();

                // Anführungszeichen entfernen falls vorhanden
                if (wert.startsWith("\"") && wert.endsWith("\"")) {
                    wert = wert.substring(1, wert.length() - 1);
                }

                if (!schluessel.isEmpty()) {
                    werte.put(schluessel, wert);
                }
            }
        } catch (IOException fehler) {
            System.err.println("Konnte " + DATEINAME + " nicht lesen: " + fehler.getMessage());
        }

        return werte;
    }

    // Sucht die schluessel.env im aktuellen Verzeichnis und eine Ebene höher.
    private static Path dateiSuchen() {
        Path[] kandidaten = {
                Paths.get(DATEINAME),
                Paths.get("src", DATEINAME),
                Paths.get("..", DATEINAME)
        };

        for (Path kandidat : kandidaten) {
            Path absolut = kandidat.toAbsolutePath().normalize();
            if (Files.exists(absolut)) {
                return absolut;
            }
        }

        return null;
    }
}