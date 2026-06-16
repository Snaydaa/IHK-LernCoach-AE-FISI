import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;

/**
 * IHK Lerncoach, 4-Screen-Anwendung mit CardLayout.
 *
 * Bildschirmreihenfolge:
 *   S1 Berufswahl → S2 Prüfungswahl → S3_AP1/S3_AP2 Themenwahl → S4 Flashcard-Dashboard
 *
 * Server-Protokoll (empfangen):
 *   "[Frage] ..."    -> Flashcard aktualisieren
 *   "[Feedback] ..." -> Feedback inline einblenden
 *   "[System] ..."   -> Status-Zeile aktualisieren
 *
 * Server-Protokoll (senden):
 *   Benutzername        -> erste Nachricht nach Verbindung
 *   GENERATE: Thema     -> KI-Generierung starten
 *   Antworttext         -> freie Nutzerantwort
 */
public class Client {

    private static final Color HINTERGRUND       = new Color(0xF1F5F9);
    private static final Color FLAECHE           = Color.WHITE;
    private static final Color PRIMAER           = new Color(0x2563EB);
    private static final Color PRIMAER_DUNKEL    = new Color(0x1D4ED8);
    private static final Color ORANGE            = new Color(0xEA580C);
    private static final Color ORANGE_DUNKEL     = new Color(0xC2410C);
    private static final Color DEAKTIVIERT       = new Color(0xE2E8F0);
    private static final Color DEAKTIVIERT_SCHRIFT = new Color(0x94A3B8);
    private static final Color SCHRIFT           = new Color(0x1E293B);
    private static final Color GEDAEMPT          = new Color(0x64748B);
    private static final Color RAHMEN            = new Color(0xCBD5E1);
    private static final Color BLAU_50           = new Color(0xEFF6FF);
    private static final Color BLAU_200          = new Color(0xBFDBFE);

    private static final String SCHRIFTART = schriftartAuswaehlen(
            "Segoe UI", "SF Pro Display", "Helvetica Neue", "SansSerif");

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    private Socket         verbindung;
    private PrintWriter    serverAusgabe;
    private BufferedReader serverEingabe;

    private JFrame     fenster;
    private CardLayout navigation;
    private JPanel     hauptPanel;

    private JTextField namenseingabe;

    private JProgressBar fortschrittsleiste;
    private JLabel       fortschrittsText;
    private JTextPane    frageTextfeld;
    private JTextPane    feedbackTextfeld;
    private JTextField   antworteingabe;
    private JButton      sendeButton;
    private JLabel       statusText;

    private String  benutzername    = "Markus";
    private String  aktuellesThema  = "";
    private String  aktuelleFrage   = "";
    private int     frageZaehler    = 0;
    private boolean kiLaedt         = false;

    // Einstiegspunkt

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        UIManager.put("OptionPane.buttonFont", new Font(SCHRIFTART, Font.BOLD, 13));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client().starten();
            }
        });
    }

    private void starten() {
        fenster = new JFrame("IHK Lerncoach");
        fenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenster.setSize(920, 700);
        fenster.setMinimumSize(new Dimension(700, 540));
        fenster.setLocationRelativeTo(null);
        fenster.getContentPane().setBackground(HINTERGRUND);

        navigation = new CardLayout();
        hauptPanel = new JPanel(navigation);
        hauptPanel.setBackground(HINTERGRUND);

        hauptPanel.add(bildschirm1Erstellen(),      "S1");
        hauptPanel.add(bildschirm2Erstellen(),      "S2");
        hauptPanel.add(bildschirm3Erstellen("AP1"), "S3_AP1");
        hauptPanel.add(bildschirm3Erstellen("AP2"), "S3_AP2");
        hauptPanel.add(bildschirm4Erstellen(),      "S4");

        fenster.add(hauptPanel);
        fenster.setVisible(true);
    }

    // Screen 1: Berufswahl

    private JPanel bildschirm1Erstellen() {
        JPanel hintergrund = zentrierterHintergrund();
        JPanel karte       = schwebendeKarte(480, 520);

        JLabel logo = new JLabel("🎓", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        logo.setAlignmentX(.5f);

        namenseingabe = eingabefeldErstellen("Dein Name, z.B. Max Mustermann");
        namenseingabe.setMaximumSize(new Dimension(360, 44));
        namenseingabe.setAlignmentX(.5f);

        JButton fiaeButton = abgerundetenButtonErstellen(
                "Fachinformatiker Anwendungsentwicklung",
                PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 360, 52);
        fiaeButton.setAlignmentX(.5f);
        fiaeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String eingabeText = namenseingabe.getText().trim();
                if (eingabeText.isEmpty()) {
                    benutzername = "Markus";
                } else {
                    benutzername = eingabeText;
                }
                navigation.show(hauptPanel, "S2");
            }
        });

        JButton fisiButton = abgerundetenButtonErstellen(
                "Fachinformatiker Systemintegration",
                DEAKTIVIERT, DEAKTIVIERT, DEAKTIVIERT_SCHRIFT, 360, 52);
        fisiButton.setAlignmentX(.5f);
        fisiButton.setEnabled(false);

        JLabel baldLabel = labelErstellen("(coming soon)", 11, Font.PLAIN, DEAKTIVIERT_SCHRIFT);
        baldLabel.setAlignmentX(.5f);

       
        befuellen(karte,
                abstand(28), logo,
                abstand(6),  labelErstellen("IHK Lerncoach", 28, Font.BOLD, SCHRIFT),
            abstand(3),  labelErstellen("Prüfungsvorbereitung für Fachinformatiker", 14, Font.PLAIN, GEDAEMPT),
            abstand(24), trennlinieErstellen(360),
            abstand(18), labelErstellen("Dein Name:", 12, Font.PLAIN, GEDAEMPT),
            abstand(6),  namenseingabe,
            abstand(22), labelErstellen("Ausbildungsberuf auswählen:", 12, Font.PLAIN, GEDAEMPT),
            abstand(10), fiaeButton,
            abstand(10), fisiButton,
            abstand(2),  baldLabel,
            abstand(28)
        );

        hintergrund.add(karte);
        return hintergrund;
    }

    // Screen 2: Prüfungswahl (AP1 vs AP2)

    private JPanel bildschirm2Erstellen() {
        JPanel hintergrund = zentrierterHintergrund();
        JPanel karte       = schwebendeKarte(580, 380);

        JPanel kachelzeile = new JPanel(new GridLayout(1, 2, 18, 0));
        kachelzeile.setOpaque(false);
        kachelzeile.setMaximumSize(new Dimension(480, 148));
        kachelzeile.setAlignmentX(.5f);

        JButton ap1Button = grosseKachelErstellen("AP1", "Teil 1", "");
        JButton ap2Button = grosseKachelErstellen("AP2", "Teil 2", "");
        ap1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigation.show(hauptPanel, "S3_AP1");
            }
        });
        ap2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigation.show(hauptPanel, "S3_AP2");
            }
        });
        kachelzeile.add(ap1Button);
        kachelzeile.add(ap2Button);

        JButton zurueckButton = geistButtonErstellen("← Zurück");
        zurueckButton.setAlignmentX(.5f);
        zurueckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigation.show(hauptPanel, "S1");
            }
        });

        befuellen(karte,
            abstand(38), labelErstellen("Welche Prüfung willst du üben?", 21, Font.BOLD, SCHRIFT),
            abstand(5),  labelErstellen("Fachinformatiker Anwendungsentwicklung", 13, Font.PLAIN, GEDAEMPT),
            abstand(34), kachelzeile,
            abstandshalter(), zurueckButton, abstand(26)
        );

        hintergrund.add(karte);
        return hintergrund;
    }

    // Screen 3: Themenwahl (AP1 oder AP2)

    private JPanel bildschirm3Erstellen(String ap) {
        boolean istAP1 = "AP1".equals(ap);

        String[][] themenListe;
        if (istAP1) {
            themenListe = new String[][] {
                {"IPv4 / IPv6 & Subnetting",           "AP1 – IPv4/IPv6/Subnetting"},
                {"Routingtabellen & Routing",           "AP1 – Routingtabellen"},
                {"Firewall-Arten & DMZ",                "AP1 – Firewalls"},
                {"Verschlüsselung (sym./asym.)",        "AP1 – Verschlüsselung"},
                {"OSI-Schichtenmodell",                 "AP1 – OSI-Modell"},
                {"Netzwerkkomponenten (Switch/Hub/AP)", "AP1 – Netzwerkkomponenten"},
                {"IT-Sicherheit & Schutzziele",        "AP1 – IT-Sicherheit"},
                {"Datenschutz (DSGVO / BDSG)",         "AP1 – Datenschutz"},
                {"BSI & Informationssicherheit",       "AP1 – Informationssicherheit"},
                {"Malware, Ransomware & BYOD",         "AP1 – Malware/BYOD"},
                {"Schreibtischtest (Trace-Tabelle)",   "AP1 – Schreibtischtest"},
                {"Pseudocode & Struktogramme",         "AP1 – Pseudocode"},
                {"UML-Diagramme (Klasse/UseCases)",    "AP1 – UML-Diagramme"},
                {"Paritäten & Prüfverfahren",          "AP1 – Paritäten"},
                {"SQL-Grundlagen & SELECT",            "AP1 – SQL-Grundlagen"},
                {"Entity-Relationship-Modell (ERM)",   "AP1 – ERM"},
                {"Projektmanagement-Basics",           "AP1 – Projektmanagement"},
                {"Netzplantechnik & Kritischer Pfad",  "AP1 – Netzplantechnik"},
                {"SMART-Methode & Zielsetzung",        "AP1 – SMART-Methode"},
                {"Agile Methoden (Scrum / Kanban)",    "AP1 – Agile Methoden"},
                {"Qualitätsmanagement (PDCA/Six Sigma)","AP1 – Qualitätsmanagement"},
                {"Nutzwertanalyse",                    "AP1 – Nutzwertanalyse"},
                {"Kosten-Nutzen-Analyse",              "AP1 – Kosten-Nutzen-Analyse"},
                {"Kalkulationsrechnung",               "AP1 – Kalkulationsrechnung"},
                {"Fachmathematik (USV / Datenmenge)",  "AP1 – Fachmathematik"},
                {"Energieeffizienz & Leistungsdaten",  "AP1 – Energieeffizienz"},
                {"Hardware & Client-Konfiguration",    "AP1 – Hardware"},
                {"KI-Grundlagen & Anwendungsbeispiele","AP1 – KI-Grundlagen"},
            };
        } else {
            themenListe = new String[][] {
                {"OOP (Vererbung, Polymorphie, Kapselung)","AP2 – OOP-Grundlagen"},
                {"Algorithmen & Kontrollstrukturen",    "AP2 – Algorithmen"},
                {"Sortier- & Suchalgorithmen",          "AP2 – Sortier-/Suchalgorithmen"},
                {"UML (Klasse, Sequenz, Aktivität, Zustand)","AP2 – UML-Diagramme"},
                {"Pseudocode & Aktivitätsdiagramme",   "AP2 – Pseudocode"},
                {"Design Patterns (MVC, Singleton, Observer)","AP2 – Design Patterns"},
                {"Modularisierung & Softwarearchitektur","AP2 – Softwarearchitektur"},
                {"GUI-Gestaltung & Usability/UX",      "AP2 – GUI & Usability"},
                {"Mockups & Wireframes",                "AP2 – Mockups"},
                {"Agile Entwicklung (Scrum / Wasserfall)","AP2 – Entwicklungsmodelle"},
                {"Versionskontrolle (Git: Merge/Push/Pull)","AP2 – Versionskontrolle"},
                {"CI/CD & Deployment",                 "AP2 – CI/CD"},
                {"Testverfahren (Black-Box / White-Box)","AP2 – Testverfahren"},
                {"Debugging, Tracing & Fehlersuche",   "AP2 – Debugging"},
                {"Testprozess & Testdokumentation",     "AP2 – Testdokumentation"},
                {"Qualitätssicherung & Abnahmeprotokoll","AP2 – Qualitätssicherung"},
                {"SQL JOINs & komplexe Abfragen",      "AP2 – SQL-JOINs"},
                {"Datenbankdesign & Normalisierung",    "AP2 – DB-Normalisierung"},
                {"ERM, Kardinalität & Schlüssel",       "AP2 – ERM & Schlüssel"},
                {"NoSQL, Datenbankmodelle & Data Lake", "AP2 – NoSQL-Modelle"},
                {"SQL-DDL/DML (CREATE, INSERT, UPDATE)","AP2 – SQL-DDL/DML"},
                {"Aggregatfunktionen & GROUP BY/HAVING","AP2 – SQL-Aggregation"},
                {"REST-APIs & Schnittstellen",         "AP2 – REST-APIs"},
                {"Cloud & Bereitstellungsmodelle (SaaS/IaaS)","AP2 – Cloud-Modelle"},
                {"Netzwerkprotokolle (TCP/UDP/HTTP/VPN)","AP2 – Netzwerkprotokolle"},
                {"Datenaustauschformate (JSON/XML/CSV)","AP2 – Datenaustausch"},
                {"IT-Sicherheit in der Softwareentwicklung","AP2 – IT-Sicherheit"},
                {"Bedrohungsszenarien (SQL-Injection/DDoS)","AP2 – Bedrohungsszenarien"},
                {"Berechtigungskonzepte & Zugriffskontrolle","AP2 – Berechtigungskonzepte"},
                {"Kommunikation & Kundenberatung",     "AP2 – Kommunikation"},
                {"Rechtliche Grundlagen (HGB/BGB/AGB)","AP2 – Rechtliche Grundlagen"},
                {"WiSo – Betrieb & Organisation",      "AP2 – WiSo-Betrieb"},
                {"WiSo – Arbeitsrecht & Tarifrecht",   "AP2 – WiSo-Arbeitsrecht"},
                {"WiSo – Umweltschutz & Nachhaltigkeit","AP2 – WiSo-Umweltschutz"},
            };
        }

        JPanel bildschirm = new JPanel(new BorderLayout());
        bildschirm.setBackground(HINTERGRUND);

        JPanel kopfzeile = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FLAECHE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(RAHMEN);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        kopfzeile.setOpaque(false);
        kopfzeile.setLayout(new BoxLayout(kopfzeile, BoxLayout.Y_AXIS));
        kopfzeile.setBorder(new EmptyBorder(28, 48, 18, 48));

        JLabel titelLabel      = labelErstellen(ap + " – Thema auswählen", 22, Font.BOLD, SCHRIFT);
        JLabel untertitelLabel = labelErstellen(
                "KI generiert deine individuelle Prüfung live — wähle ein Thema", 13, Font.PLAIN, GEDAEMPT);
        titelLabel.setAlignmentX(.5f);
        untertitelLabel.setAlignmentX(.5f);

        final String simulationsSchluessel = themenListe[0][1];
        JButton simulationsButton = abgerundetenButtonErstellen(
                "  " + ap + "Prüfungssimulation  (30-Fragen-Mix)",
                ORANGE, ORANGE_DUNKEL, Color.WHITE, 460, 52);
        simulationsButton.setAlignmentX(.5f);
        simulationsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lernenStarten(simulationsSchluessel);
            }
        });

        kopfzeile.add(titelLabel);
        kopfzeile.add(abstand(4));
        kopfzeile.add(untertitelLabel);
        kopfzeile.add(abstand(18));
        kopfzeile.add(simulationsButton);
        kopfzeile.add(abstand(18));

        JPanel themenPanel = new JPanel();
        themenPanel.setBackground(HINTERGRUND);
        themenPanel.setLayout(new BoxLayout(themenPanel, BoxLayout.Y_AXIS));
        themenPanel.setBorder(new EmptyBorder(16, 48, 16, 48));

        for (String[] thema : themenListe) {
            final String schluessel   = thema[1];
            final String beschriftung = thema[0];
            JButton themenButton = themenButtonErstellen(beschriftung, 560, 46, true);
            themenButton.setAlignmentX(.5f);
            themenButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    lernenStarten(schluessel);
                }
            });
            themenPanel.add(themenButton);
            themenPanel.add(abstand(8));
        }

        JScrollPane scrollbereich = new JScrollPane(themenPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollbereich.setBorder(BorderFactory.createEmptyBorder());
        scrollbereich.getVerticalScrollBar().setUnitIncrement(16);
        scrollbereich.setOpaque(false);
        scrollbereich.getViewport().setOpaque(false);

        JPanel fusszeile = new JPanel(new FlowLayout(FlowLayout.CENTER));
        fusszeile.setBackground(HINTERGRUND);
        fusszeile.setBorder(new EmptyBorder(8, 0, 16, 0));
        JButton zurueckButton = geistButtonErstellen("← Zurück zur Prüfungswahl");
        zurueckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigation.show(hauptPanel, "S2");
            }
        });
        fusszeile.add(zurueckButton);

        bildschirm.add(kopfzeile,   BorderLayout.NORTH);
        bildschirm.add(scrollbereich, BorderLayout.CENTER);
        bildschirm.add(fusszeile,   BorderLayout.SOUTH);

        return bildschirm;
    }

    // Screen 4: Flashcard-Dashboard

    private JPanel bildschirm4Erstellen() {
        JPanel bildschirm = new JPanel(new BorderLayout());
        bildschirm.setBackground(HINTERGRUND);
        bildschirm.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel kopfleiste = new JPanel(new BorderLayout(14, 0));
        kopfleiste.setOpaque(false);
        kopfleiste.setBorder(new EmptyBorder(0, 0, 20, 0));

        JButton homeButton = geistButtonErstellen("← Menü");
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigation.show(hauptPanel, "S1");
                sitzungZuruecksetzen();
            }
        });

        fortschrittsText = new JLabel("0 / 30 Fragen");
        fortschrittsText.setFont(new Font(SCHRIFTART, Font.PLAIN, 12));
        fortschrittsText.setForeground(GEDAEMPT);

        // TODO: Progressbar auf 30 wenn die geplante 30-Fragen-Simulation implementiert ist
        fortschrittsleiste = new JProgressBar(0, 30);
        fortschrittsleiste.setValue(0);
        fortschrittsleiste.setForeground(PRIMAER);
        fortschrittsleiste.setBackground(new Color(0xE2E8F0));
        fortschrittsleiste.setBorderPainted(false);
        fortschrittsleiste.setStringPainted(false);
        fortschrittsleiste.setPreferredSize(new Dimension(0, 8));

        JPanel fortschrittsSpalte = new JPanel(new BorderLayout(0, 6));
        fortschrittsSpalte.setOpaque(false);
        fortschrittsSpalte.add(fortschrittsleiste, BorderLayout.CENTER);
        fortschrittsSpalte.add(fortschrittsText,   BorderLayout.EAST);

        JButton supportButton = abgerundetenButtonErstellen(
                "KI-Support fragen", PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 190, 40);
        supportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                supportChatOeffnen();
            }
        });

        kopfleiste.add(homeButton,          BorderLayout.WEST);
        kopfleiste.add(fortschrittsSpalte,  BorderLayout.CENTER);
        kopfleiste.add(supportButton,       BorderLayout.EAST);

        JPanel fragenkarte = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 24));
                g2.fill(new RoundRectangle2D.Float(4, 7,
                        getWidth() - 5, getHeight() - 6, 24, 24));
                g2.setColor(FLAECHE);
                g2.fill(new RoundRectangle2D.Float(0, 0,
                        getWidth() - 5, getHeight() - 8, 22, 22));
                g2.setColor(RAHMEN);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f,
                        getWidth() - 6, getHeight() - 9, 22, 22));
                g2.dispose();
            }
        };
        fragenkarte.setOpaque(false);
        fragenkarte.setBorder(new EmptyBorder(36, 52, 36, 52));

        // Abzeichen "FRAGE"
        JLabel abzeichen = new JLabel("FRAGE");
        abzeichen.setFont(new Font(SCHRIFTART, Font.BOLD, 10));
        abzeichen.setForeground(PRIMAER);
        abzeichen.setBackground(BLAU_50);
        abzeichen.setOpaque(true);
        abzeichen.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BLAU_200, 1, true),
            new EmptyBorder(3, 10, 3, 10)));
        JPanel abzeichenZeile = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        abzeichenZeile.setOpaque(false);
        abzeichenZeile.add(abzeichen);

        // Frage-Textbereich
        frageTextfeld = new JTextPane();
        frageTextfeld.setEditable(false);
        frageTextfeld.setOpaque(false);
        frageTextfeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 19));
        frageTextfeld.setForeground(SCHRIFT);
        frageTextfeld.setBorder(new EmptyBorder(24, 0, 24, 0));
        frageTextSetzen("Thema wählen, um zu starten.\n\nDrücke ← Menü und wähle ein Thema.");

        statusText = new JLabel("Bereit");
        statusText.setFont(new Font(SCHRIFTART, Font.PLAIN, 12));
        statusText.setForeground(GEDAEMPT);
        statusText.setHorizontalAlignment(SwingConstants.CENTER);

        fragenkarte.add(abzeichenZeile, BorderLayout.NORTH);
        fragenkarte.add(frageTextfeld,  BorderLayout.CENTER);
        fragenkarte.add(statusText,     BorderLayout.SOUTH);

        JPanel feedbackKarte = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FLAECHE);
                g2.fill(new RoundRectangle2D.Float(0, 0,
                        getWidth() - 1, getHeight() - 1, 16, 16));
                g2.setColor(BLAU_200);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f,
                        getWidth() - 2, getHeight() - 2, 16, 16));
                g2.dispose();
            }
        };
        feedbackKarte.setOpaque(false);
        feedbackKarte.setBorder(new EmptyBorder(12, 16, 12, 16));
        feedbackKarte.setPreferredSize(new Dimension(0, 132));

        feedbackTextfeld = new JTextPane();
        feedbackTextfeld.setEditable(false);
        feedbackTextfeld.setOpaque(false);
        feedbackTextfeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 13));
        feedbackTextfeld.setForeground(SCHRIFT);
        feedbackTextfeld.setBorder(new EmptyBorder(0, 0, 0, 0));
        feedbackTextSetzen("Coach-Feedback", "Noch keine Bewertung. Beantworte zuerst eine Frage.");

        JScrollPane feedbackScrollbereich = new JScrollPane(feedbackTextfeld,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        feedbackScrollbereich.setBorder(BorderFactory.createEmptyBorder());
        feedbackScrollbereich.setOpaque(false);
        feedbackScrollbereich.getViewport().setOpaque(false);
        feedbackScrollbereich.getVerticalScrollBar().setUnitIncrement(12);
        feedbackKarte.add(feedbackScrollbereich, BorderLayout.CENTER);

        JPanel mittelStack = new JPanel(new BorderLayout(0, 14));
        mittelStack.setOpaque(false);
        mittelStack.add(fragenkarte,   BorderLayout.CENTER);
        mittelStack.add(feedbackKarte, BorderLayout.SOUTH);

        JPanel eingabeleiste = new JPanel(new BorderLayout(14, 0));
        eingabeleiste.setOpaque(false);
        eingabeleiste.setBorder(new EmptyBorder(20, 0, 0, 0));

        antworteingabe = eingabefeldErstellen("Deine Antwort eingeben …");
        antworteingabe.setFont(new Font(SCHRIFTART, Font.PLAIN, 15));
        antworteingabe.setEnabled(false);

        sendeButton = abgerundetenButtonErstellen("Senden ↵", PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 126, 48);
        sendeButton.setEnabled(false);

        ActionListener sendeAktion = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                antwortAbschicken();
            }
        };
        antworteingabe.addActionListener(sendeAktion);
        sendeButton.addActionListener(sendeAktion);

        eingabeleiste.add(antworteingabe, BorderLayout.CENTER);
        eingabeleiste.add(sendeButton,    BorderLayout.EAST);

        bildschirm.add(kopfleiste,   BorderLayout.NORTH);
        bildschirm.add(mittelStack,  BorderLayout.CENTER);
        bildschirm.add(eingabeleiste, BorderLayout.SOUTH);

        return bildschirm;
    }

    // Netzwerk & Protokoll


    private void lernenStarten(String thema) {
        navigation.show(hauptPanel, "S4");
        sitzungZuruecksetzen();
        aktuellesThema = thema;
        aktuelleFrage  = "";

        kiLaedt = true;
        ladezustandAnzeigen(thema);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (verbindung != null && !verbindung.isClosed()) {
                        verbindung.close();
                    }
                    verbindung    = new Socket(HOST, PORT);
                    serverAusgabe = new PrintWriter(verbindung.getOutputStream(), true);
                    serverEingabe = new BufferedReader(
                                        new InputStreamReader(verbindung.getInputStream()));

                    serverAusgabe.println(benutzername);
                    leseschleife(thema);

                } catch (IOException fehler) {
                    final String fehlermeldung = fehler.getMessage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            kiLaedt = false;
                            frageTextSetzen("Keine Verbindung zum Server\n\n"
                                   + HOST + ":" + PORT + "ist nicht erreichbar.\n\n"
                                   + "Starte den Server mit: java Server");
                            statusText.setText(fehlermeldung);
                            antworteingabe.setEnabled(false);
                            sendeButton.setEnabled(false);
                        }
                    });
                }
            }
        }).start();
    }

    // Ladebildschirm anzeigen, muss auf dem EDT aufgerufen werden.
    private void ladezustandAnzeigen(String thema) {
        String kurzName;
        if (thema.contains("–")) {
            kurzName = thema.substring(thema.indexOf("–") + 1).trim();
        } else {
            kurzName = thema;
        }
        frageTextSetzen("KI generiert deine individuelle Prüfung …\n\n"
               + "Thema: " + kurzName + "\n\n"
               + "Bitte warten (ca. 10 Sekunden)");
        statusText.setText("Verbinde und generiere Fragen …");
        antworteingabe.setEnabled(false);
        antworteingabe.setText("");
        sendeButton.setEnabled(false);
        feedbackTextSetzen("Coach-Feedback", "Noch keine Bewertung. Beantworte zuerst eine Frage.");
    }

    // Blockierende Leseschleife, läuft immer auf einem eigenen Thread.
    private void leseschleife(String thema) {
        try {
            String nachricht;
            while ((nachricht = serverEingabe.readLine()) != null) {
                final String aktuelleZeile = nachricht;

                // Willkommen: KI-Generierung starten
                if (aktuelleZeile.contains("Willkommen")) {
                    serverAusgabe.println("GENERATE: " + thema);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Fragen werden generiert …");
                        }
                    });
                    continue;
                }

                // Nächste Frage empfangen
                if (aktuelleZeile.startsWith("[Frage]")) {
                    frageZaehler++;
                    final int frageNummer = frageZaehler;

                    final String rohesFrage;
                    if (aktuelleZeile.startsWith("[Frage]")) {
                        rohesFrage = aktuelleZeile.substring("[Frage]".length()).trim();
                    } else {
                        rohesFrage = aktuelleZeile.trim();
                    }
                    final String frageText   = protokolltextDekodieren(rohesFrage);
                    aktuelleFrage            = frageText;
                    final boolean warAmLaden = kiLaedt;
                    kiLaedt = false;

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fortschrittsleiste.setValue(Math.min(frageNummer, 30));
                            fortschrittsText.setText(frageNummer + " / 10 Fragen");
                            frageTextSetzen(frageText);
                            if (warAmLaden) {
                                statusText.setText("KI-Prüfung bereit! Beantworte die Frage.");
                            } else {
                                statusText.setText("Beantworte die Frage und klicke Senden.");
                            }
                            antworteingabe.setEnabled(true);
                            sendeButton.setEnabled(true);
                            antworteingabe.setText("");
                            antworteingabe.requestFocus();
                        }
                    });
                    continue;
                }

                // Feedback empfangen
                if (aktuelleZeile.startsWith("[Feedback]")) {
                    String restText = aktuelleZeile.substring("[Feedback]".length()).trim();
                    if (!restText.isEmpty()) {
                        final String rueckmeldung = protokolltextDekodieren(restText);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                feedbackEinblenden(rueckmeldung);
                            }
                        });
                    }
                    continue;
                }

                // System-Status
                if (aktuelleZeile.startsWith("[System]")) {
                    final String systemText = aktuelleZeile.substring(8).trim();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText(systemText);
                        }
                    });
                    continue;
                }
            }
        } catch (IOException fehler) {
            final String fehlermeldung = fehler.getMessage();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    statusText.setText("Verbindung unterbrochen: " + fehlermeldung);
                }
            });
        }
    }

    private void antwortAbschicken() {
        String antwortText = antworteingabe.getText().trim();
        if (antwortText.isEmpty() || serverAusgabe == null) {
            return;
        }
        serverAusgabe.println(antwortText);
        antworteingabe.setText("");
        sendeButton.setEnabled(false);
        statusText.setText("Antwort wird bewertet …");
    }

    // Feedback-Anzeige (wird auf dem EDT aufgerufen)

    private void feedbackEinblenden(String rueckmeldung) {
        feedbackTextSetzen("Bewertung deiner letzten Antwort", rueckmeldung);
        statusText.setText("Feedback ist unten im Dashboard sichtbar.");
    }

    // Zustandsverwaltung

    private void sitzungZuruecksetzen() {
        frageZaehler  = 0;
        kiLaedt       = false;
        aktuelleFrage = "";
        if (fortschrittsleiste != null) {
            fortschrittsleiste.setValue(0);
        }
        if (fortschrittsText != null) {
            fortschrittsText.setText("0 / 30 Fragen");
        }
        feedbackTextSetzen("Coach-Feedback", "Noch keine Bewertung. Beantworte zuerst eine Frage.");
    }

    private void feedbackTextSetzen(String titel, String inhalt) {
        if (feedbackTextfeld == null) {
            return;
        }
        String anzeigeText;
        if (inhalt == null) {
            anzeigeText = "";
        } else {
            anzeigeText = inhalt;
        }
        feedbackTextfeld.setText(titel + "\n\n" + anzeigeText);
        StyledDocument dokument = feedbackTextfeld.getStyledDocument();
        SimpleAttributeSet attribute = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribute, StyleConstants.ALIGN_LEFT);
        StyleConstants.setLineSpacing(attribute, 0.18f);
        dokument.setParagraphAttributes(0, dokument.getLength(), attribute, false);
        feedbackTextfeld.setCaretPosition(0);
    }

    // Dekodiert \\n und \\r Sequenzen zurück in echte Zeilenumbrüche.
    private static String protokolltextDekodieren(String wert) {
        if (wert == null || wert.isEmpty()) {
            return "";
        }
        StringBuilder ergebnis = new StringBuilder();
        boolean maskiert = false;

        for (int i = 0; i < wert.length(); i++) {
            char zeichen = wert.charAt(i);
            if (maskiert) {
                switch (zeichen) {
                    case 'n':  ergebnis.append('\n'); break;
                    case 'r':  ergebnis.append('\r'); break;
                    case '\\': ergebnis.append('\\'); break;
                    default:   ergebnis.append(zeichen); break;
                }
                maskiert = false;
            } else if (zeichen == '\\') {
                maskiert = true;
            } else {
                ergebnis.append(zeichen);
            }
        }
        if (maskiert) {
            ergebnis.append('\\');
        }
        return ergebnis.toString();
    }

    private void frageTextSetzen(String inhalt) {
        frageTextfeld.setText(inhalt);
        StyledDocument dokument = frageTextfeld.getStyledDocument();
        SimpleAttributeSet attribute = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribute, StyleConstants.ALIGN_CENTER);
        StyleConstants.setLineSpacing(attribute, 0.38f);
        dokument.setParagraphAttributes(0, dokument.getLength(), attribute, false);
    }

    // Widget-Fabrik-Hilfsmethoden

    private void supportChatOeffnen() {
        String eingabeText;
        if (namenseingabe != null) {
            eingabeText = namenseingabe.getText().trim();
        } else {
            eingabeText = "";
        }
        String anzeigeName;
        if (eingabeText.isEmpty()) {
            anzeigeName = benutzername;
        } else {
            anzeigeName = eingabeText;
        }
        SupportChatWindow supportFenster = new SupportChatWindow(
                fenster, anzeigeName, aktuellesThema, aktuelleFrage);
        supportFenster.setVisible(true);
    }

    private Component abstand(int hoehe) {
        return Box.createVerticalStrut(hoehe);
    }

    private Component abstandshalter() {
        return Box.createVerticalGlue();
    }

    private JSeparator trennlinieErstellen(int maxBreite) {
        JSeparator trennlinie = new JSeparator();
        trennlinie.setForeground(new Color(0xE2E8F0));
        trennlinie.setMaximumSize(new Dimension(maxBreite, 1));
        trennlinie.setAlignmentX(.5f);
        return trennlinie;
    }

    private void befuellen(JPanel panel, Component... elemente) {
        for (Component element : elemente) {
            panel.add(element);
        }
    }

    private JPanel zentrierterHintergrund() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(HINTERGRUND);
        return panel;
    }

    private JPanel schwebendeKarte(int breite, int hoehe) {
        JPanel karte = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 22));
                g2.fill(new RoundRectangle2D.Float(5, 8,
                        getWidth() - 6, getHeight() - 6, 24, 24));
                g2.setColor(FLAECHE);
                g2.fill(new RoundRectangle2D.Float(0, 0,
                        getWidth() - 6, getHeight() - 8, 22, 22));
                g2.dispose();
            }
        };
        karte.setOpaque(false);
        karte.setLayout(new BoxLayout(karte, BoxLayout.Y_AXIS));
        karte.setPreferredSize(new Dimension(breite, hoehe));
        karte.setBorder(new EmptyBorder(0, 40, 0, 40));
        return karte;
    }

    private JLabel labelErstellen(String inhalt, int schriftgroesse, int schriftstil, Color farbe) {
        JLabel label = new JLabel(inhalt, SwingConstants.CENTER);
        label.setFont(new Font(SCHRIFTART, schriftstil, schriftgroesse));
        label.setForeground(farbe);
        label.setAlignmentX(.5f);
        return label;
    }

    private JTextField eingabefeldErstellen(String platzhalter) {
        JTextField eingabefeld = new JTextField(platzhalter) {
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color rahmenFarbe;
                if (isEnabled()) {
                    rahmenFarbe = RAHMEN;
                } else {
                    rahmenFarbe = DEAKTIVIERT;
                }
                g2.setColor(rahmenFarbe);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        eingabefeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 14));
        eingabefeld.setForeground(SCHRIFT);
        eingabefeld.setBackground(FLAECHE);
        eingabefeld.setBorder(new EmptyBorder(10, 14, 10, 14));
        eingabefeld.setDisabledTextColor(DEAKTIVIERT_SCHRIFT);
        return eingabefeld;
    }

    private JButton abgerundetenButtonErstellen(String beschriftung, Color hintergrundfarbe,
                                                Color hoverfarbe, Color schriftfarbe,
                                                int breite, int hoehe) {
        JButton schaltflaeche = new JButton(beschriftung) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color hintergrund;
                if (isEnabled()) {
                    hintergrund = getBackground();
                } else {
                    hintergrund = DEAKTIVIERT;
                }
                g2.setColor(hintergrund);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        schaltflaeche.setBackground(hintergrundfarbe);
        schaltflaeche.setForeground(schriftfarbe);
        schaltflaeche.setFont(new Font(SCHRIFTART, Font.BOLD, 13));
        schaltflaeche.setOpaque(false);
        schaltflaeche.setContentAreaFilled(false);
        schaltflaeche.setBorderPainted(false);
        schaltflaeche.setFocusPainted(false);
        schaltflaeche.setPreferredSize(new Dimension(breite, hoehe));
        schaltflaeche.setMaximumSize(new Dimension(breite, hoehe));
        schaltflaeche.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        schaltflaeche.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (schaltflaeche.isEnabled()) {
                    schaltflaeche.setBackground(hoverfarbe);
                    schaltflaeche.repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (schaltflaeche.isEnabled()) {
                    schaltflaeche.setBackground(hintergrundfarbe);
                    schaltflaeche.repaint();
                }
            }
        });
        return schaltflaeche;
    }

    private JButton themenButtonErstellen(String beschriftung, int breite, int hoehe, boolean aktiv) {
        final Color textFarbe;
        final Color rahmenFarbe;
        if (aktiv) {
            textFarbe   = SCHRIFT;
            rahmenFarbe = RAHMEN;
        } else {
            textFarbe   = DEAKTIVIERT_SCHRIFT;
            rahmenFarbe = DEAKTIVIERT;
        }

        JButton schaltflaeche = new JButton(beschriftung) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(rahmenFarbe);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        schaltflaeche.setBackground(FLAECHE);
        schaltflaeche.setForeground(textFarbe);

        final int schriftStil;
        if (aktiv) {
            schriftStil = Font.BOLD;
        } else {
            schriftStil = Font.PLAIN;
        }
        schaltflaeche.setFont(new Font(SCHRIFTART, schriftStil, 13));
        schaltflaeche.setOpaque(false);
        schaltflaeche.setContentAreaFilled(false);
        schaltflaeche.setBorderPainted(false);
        schaltflaeche.setFocusPainted(false);
        schaltflaeche.setEnabled(aktiv);
        schaltflaeche.setPreferredSize(new Dimension(breite, hoehe));
        schaltflaeche.setMaximumSize(new Dimension(breite, hoehe));

        if (aktiv) {
            schaltflaeche.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            schaltflaeche.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    schaltflaeche.setBackground(new Color(0xF1F5F9));
                    schaltflaeche.repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    schaltflaeche.setBackground(FLAECHE);
                    schaltflaeche.repaint();
                }
            });
        }
        return schaltflaeche;
    }

    private JButton grosseKachelErstellen(String titel, String untertitel, String symbol) {
        JButton kachel = new JButton() {
            boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (hover) {
                    g2.setColor(BLAU_50);
                } else {
                    g2.setColor(FLAECHE);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));

                if (hover) {
                    g2.setColor(BLAU_200);
                } else {
                    g2.setColor(RAHMEN);
                }
                float strichBreite;
                if (hover) {
                    strichBreite = 1.8f;
                } else {
                    strichBreite = 1.2f;
                }
                g2.setStroke(new BasicStroke(strichBreite));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));

                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(SCHRIFT);
                g2.drawString(symbol, (getWidth() - fm.stringWidth(symbol)) / 2, 48);

                g2.setFont(new Font(SCHRIFTART, Font.BOLD, 18));
                fm = g2.getFontMetrics();
                if (hover) {
                    g2.setColor(PRIMAER);
                } else {
                    g2.setColor(SCHRIFT);
                }
                g2.drawString(titel, (getWidth() - fm.stringWidth(titel)) / 2, 78);

                g2.setFont(new Font(SCHRIFTART, Font.PLAIN, 12));
                fm = g2.getFontMetrics();
                g2.setColor(GEDAEMPT);
                g2.drawString(untertitel, (getWidth() - fm.stringWidth(untertitel)) / 2, 98);

                g2.dispose();
            }
        };
        kachel.setOpaque(false);
        kachel.setContentAreaFilled(false);
        kachel.setBorderPainted(false);
        kachel.setFocusPainted(false);
        kachel.setPreferredSize(new Dimension(220, 124));
        kachel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return kachel;
    }

    private JButton geistButtonErstellen(String beschriftung) {
        JButton schaltflaeche = new JButton(beschriftung);
        schaltflaeche.setFont(new Font(SCHRIFTART, Font.PLAIN, 13));
        schaltflaeche.setForeground(GEDAEMPT);
        schaltflaeche.setContentAreaFilled(false);
        schaltflaeche.setBorderPainted(false);
        schaltflaeche.setFocusPainted(false);
        schaltflaeche.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        schaltflaeche.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                schaltflaeche.setForeground(SCHRIFT);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                schaltflaeche.setForeground(GEDAEMPT);
            }
        });
        return schaltflaeche;
    }

    private static String schriftartAuswaehlen(String... kandidaten) {
        for (String schriftname : kandidaten) {
            Font probeschrift = new Font(schriftname, Font.PLAIN, 12);
            if (!probeschrift.getFamily().equalsIgnoreCase("Dialog")) {
                return schriftname;
            }
        }
        return "SansSerif";
    }
}
