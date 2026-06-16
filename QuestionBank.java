import java.util.*;

public class QuestionBank {

    public static class Frage {

        // Attribute
        public final String thema;
        public final String frageText;
        public final String musterloesung;

        // Konstruktor
        public Frage(String thema, String frageText, String musterloesung) {
            this.thema         = thema;
            this.frageText     = frageText;
            this.musterloesung = musterloesung;
        }
    }

    // Attribute
    private static final Map<String, List<Frage>> FRAGEN_PRO_BENUTZER = new HashMap<>();
    private static final Map<String, Integer>     FRAGEN_INDEX        = new HashMap<>();

    // Konstruktor
    private QuestionBank() {}

    public static synchronized void generierteFragenSpeichern(String benutzerId, List<Frage> fragen) {
        FRAGEN_PRO_BENUTZER.put(benutzerId, new ArrayList<>(fragen));
        FRAGEN_INDEX.put(benutzerId, 0);
    }

    public static synchronized Frage naechsteFrageHolen(String benutzerId) {
        List<Frage> fragen = FRAGEN_PRO_BENUTZER.get(benutzerId);
        if (fragen == null || fragen.isEmpty()) {
            return null;
        }
        int index = FRAGEN_INDEX.getOrDefault(benutzerId, 0);
        if (index >= fragen.size()) {
            return null;
        }
        FRAGEN_INDEX.put(benutzerId, index + 1);
        return fragen.get(index);
    }

    public static synchronized int fragenAnzahlHolen(String benutzerId) {
        List<Frage> fragen = FRAGEN_PRO_BENUTZER.get(benutzerId);
        if (fragen == null) {
            return 0;
        }
        return fragen.size();
    }

    public static synchronized int naechsteFragenNummer(String benutzerId) {
        return FRAGEN_INDEX.getOrDefault(benutzerId, 0) + 1;
    }

    public static synchronized void fragenLoeschen(String benutzerId) {
        FRAGEN_PRO_BENUTZER.remove(benutzerId);
        FRAGEN_INDEX.remove(benutzerId);
    }
}