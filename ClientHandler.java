import java.io.*;
import java.net.*;

/**
 * Lern-Orchestrator für einen einzelnen Client: verarbeitet GENERATE, Antworten und den KI-Support.
 */
public class ClientHandler implements Runnable {

    // Attribute
    private enum Zustand {
        LEERLAUF,
        GENERIERT_FRAGEN,
        WARTET_AUF_ANTWORT,
        BEWERTET
    }

    private static final String GENERATE_PRAEFIX = "GENERATE:";
    private static final String SUPPORT_PRAEFIX  = "SUPPORT|";

    private final Socket verbindung;
    private PrintWriter  ausgabe;
    private String       benutzername;
    private Zustand      zustand   = Zustand.LEERLAUF;
    private QuestionBank.Frage aktiveFrage;

    // Konstruktor
    public ClientHandler(Socket verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public void run() {
        try {
            BufferedReader eingabe = new BufferedReader(new InputStreamReader(verbindung.getInputStream()));
            ausgabe = new PrintWriter(verbindung.getOutputStream(), true);

            // Erste Zeile ist IMMER nur der Benutzername, kein Präfix davor.
            benutzername = eingabe.readLine();
            if (benutzername == null || benutzername.trim().isEmpty()) {
                benutzername = "user-" + verbindung.getPort();
            }
            benutzername = benutzername.trim();

            System.out.println("[Server] " + benutzername + " verbunden.");
            senden("[System] Willkommen, " + benutzername + "! Sende GENERATE:<Thema> oder SUPPORT|<Thema>|<Frage>|<Nachricht>.");

            String nachricht;
            while ((nachricht = eingabe.readLine()) != null) {
                nachrichtVerarbeiten(nachricht.trim());
            }
        } catch (IOException fehler) {
            System.out.println("[Server] Verbindung verloren: " + benutzername);
        } finally {
            verbindungSchliessen();
        }
    }

    private void nachrichtVerarbeiten(String nachricht) {
        if (nachricht == null || nachricht.isEmpty()) {
            senden("[System] Bitte sende entweder GENERATE:<Thema>, SUPPORT|... oder eine Antwort auf die aktuelle Frage.");
            return;
        }

        // Support läuft unabhängig vom aktuellen Lernrunden-Zustand jederzeit.
        if (nachricht.startsWith(SUPPORT_PRAEFIX)) {
            supportVerarbeiten(nachricht);
            return;
        }

        if (nachricht.startsWith(GENERATE_PRAEFIX)) {
            fragengenerierungStarten(nachricht.substring(GENERATE_PRAEFIX.length()).trim());
            return;
        }

        switch (zustand) {
            case LEERLAUF:
                senden("[System] Kein aktiver Fragensatz. Wähle im Lerncoach ein Thema aus.");
                break;
            case GENERIERT_FRAGEN:
                senden("[System] Die Fragen werden gerade erzeugt. Bitte warte auf die erste Frage.");
                break;
            case BEWERTET:
                senden("[System] Deine letzte Antwort wird gerade bewertet. Bitte warte auf Feedback und die nächste Frage.");
                break;
            case WARTET_AUF_ANTWORT:
                antwortVerarbeiten(nachricht);
                break;
        }
    }

    private void fragengenerierungStarten(String thema) {
        if (thema == null || thema.trim().isEmpty()) {
            senden("[System] Bitte gib ein Thema an. Beispiel: GENERATE: Subnetting");
            return;
        }

        zustand = Zustand.GENERIERT_FRAGEN;
        aktiveFrage = null;
        QuestionBank.fragenLoeschen(benutzerSchluessel());
        senden("[System] Generiere 10 IHK-nahe Fragen zu: " + thema);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int anzahl = ExamGenerator.fragenFuerBenutzerErstellen(benutzerSchluessel(), thema);
                    senden("[System] Fragensatz erstellt: " + anzahl + " Fragen.");
                    naechsteFrageStellen();
                } catch (Exception fehler) {
                    zustand = Zustand.LEERLAUF;
                    senden("[System] Fehler beim Generieren: " + fehler.getMessage());
                }
            }
        }, "exam-generator-" + benutzerSchluessel()).start();
    }

    private void antwortVerarbeiten(String userAntwort) {
        if (aktiveFrage == null) {
            zustand = Zustand.LEERLAUF;
            senden("[System] Keine aktive Frage vorhanden. Wähle im Lerncoach ein Thema aus.");
            return;
        }

        zustand = Zustand.BEWERTET;
        final QuestionBank.Frage frage = aktiveFrage;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String rueckmeldung = ApiHandler.antwortBewerten(
                            frage.frageText,
                            frage.musterloesung,
                            userAntwort
                    );
                    senden("[Feedback] " + protokolltextKodieren(rueckmeldung));
                    naechsteFrageStellen();
                } catch (Exception fehler) {
                    zustand = Zustand.WARTET_AUF_ANTWORT;
                    senden("[System] Fehler bei der Bewertung: " + fehler.getMessage());
                }
            }
        }, "answer-evaluator-" + benutzerSchluessel()).start();
    }

    // Erwartet Format: SUPPORT|Thema|Frage|Nutzerfrage
    private void supportVerarbeiten(String nachricht) {
        String[] teile       = nachricht.split("\\|", 4);
        String thema         = teile.length >= 2 ? teile[1].trim() : "";
        String aktuelleFrage = teile.length >= 3 ? teile[2].trim() : "";
        String nutzerfrage   = teile.length >= 4 ? teile[3].trim() : "";

        if (nutzerfrage.isEmpty()) {
            senden("AI|Was genau möchtest du zum aktuellen Thema wissen?");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String antwort = ApiHandler.supportChatAnfragen(thema, aktuelleFrage, nutzerfrage);
                    senden("AI|" + protokolltextKodieren(antwort));
                } catch (Exception fehler) {
                    senden("ERR|" + protokolltextKodieren("KI-Support konnte nicht antworten: " + fehler.getMessage()));
                }
            }
        }, "support-chat-" + benutzerSchluessel()).start();
    }

    private void naechsteFrageStellen() {
        int gesamtAnzahl   = QuestionBank.fragenAnzahlHolen(benutzerSchluessel());
        int naechsteNummer = QuestionBank.naechsteFragenNummer(benutzerSchluessel());
        aktiveFrage        = QuestionBank.naechsteFrageHolen(benutzerSchluessel());

        if (aktiveFrage == null) {
            zustand = Zustand.LEERLAUF;
            senden("[System] Lernrunde abgeschlossen. Wähle ein neues Thema, um weiterzumachen.");
            return;
        }

        zustand = Zustand.WARTET_AUF_ANTWORT;
        String frageText = "Frage " + naechsteNummer + "/" + gesamtAnzahl
                + " – " + aktiveFrage.thema + "\n\n" + aktiveFrage.frageText;
        senden("[Frage] " + protokolltextKodieren(frageText));
    }

    // Kodiert Zeilenumbrüche für das einzeilige Socket-Protokoll.
    private static String protokolltextKodieren(String wert) {
        if (wert == null) {
            return "";
        }
        return wert
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    // Getter
    private String benutzerSchluessel() {
        if (benutzername == null) {
            return "user-" + verbindung.getPort();
        }
        return benutzername;
    }

    private synchronized void senden(String nachricht) {
        if (ausgabe != null) {
            ausgabe.println(nachricht);
        }
    }

    private void verbindungSchliessen() {
        QuestionBank.fragenLoeschen(benutzerSchluessel());
        try {
            verbindung.close();
        } catch (IOException ignored) {}
        System.out.println("[Server] " + benutzername + " getrennt.");
    }
}
