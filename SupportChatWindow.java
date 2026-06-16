import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SupportChatWindow extends JDialog {

    // Attribute
    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    private static final Color HINTERGRUND    = new Color(0xDBEAFE);
    private static final Color FLAECHE        = Color.WHITE;
    private static final Color SCHRIFT        = new Color(0x1E293B);
    private static final Color GEDAEMPT       = new Color(0x64748B);
    private static final Color RAHMEN         = new Color(0xCBD5E1);
    private static final Color PRIMAER        = new Color(0x2563EB);
    private static final Color PRIMAER_DUNKEL = new Color(0x1D4ED8);
    private static final Color BLAU_50        = new Color(0xEFF6FF);
    private static final Color BLAU_200       = new Color(0xBFDBFE);
    private static final Color ROT            = new Color(0xDC2626);

    private static final String SCHRIFTART = schriftartAuswaehlen(
            "Segoe UI", "SF Pro Display", "Helvetica Neue", "SansSerif");

    private final String benutzername;
    private final String aktuellesThema;
    private final String aktuelleFrage;

    private JLabel     statusText;
    private JTextPane  chatTextfeld;
    private JTextField eingabefeld;
    private JButton    sendeButton;

    private Socket        verbindung;
    private BufferedReader eingabe;
    private PrintWriter    ausgabe;

    private volatile boolean verbunden  = false;
    private volatile boolean geschlossen = false;

    // Konstruktor
    public SupportChatWindow(Window owner, String benutzername, String aktuellesThema, String aktuelleFrage) {
        super(owner, "KI-Support", ModalityType.MODELESS);

        this.benutzername   = benutzername  != null ? benutzername.trim()   : "Markus";
        this.aktuellesThema = aktuellesThema != null ? aktuellesThema.trim() : "Kein Thema aktiv";
        this.aktuelleFrage  = aktuelleFrage  != null ? aktuelleFrage.trim()  : "Keine aktive Frage";

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 620);
        setMinimumSize(new Dimension(640, 460));
        setLocationRelativeTo(owner);

        oberflächeErstellen();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                verbinden();
            }
        });

        systemTextAnhaengen("KI-Support wird geöffnet ...");
        systemTextAnhaengen("Kontext: " + this.aktuellesThema);
    }

    @Override
    public void dispose() {
        geschlossen = true;
        netzwerkTrennen();
        super.dispose();
    }

    private void oberflächeErstellen() {
        JPanel hauptPanel = new JPanel(new BorderLayout(0, 16));
        hauptPanel.setBackground(HINTERGRUND);
        hauptPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        hauptPanel.add(kopfzeileErstellen(),      BorderLayout.NORTH);
        hauptPanel.add(chatbereichErstellen(),    BorderLayout.CENTER);
        hauptPanel.add(eingabeleistenErstellen(), BorderLayout.SOUTH);

        setContentPane(hauptPanel);
    }

    private JPanel kopfzeileErstellen() {
        JPanel kopfzeile = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLAU_50);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22));
                g2.setColor(BLAU_200);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 22, 22));
                g2.dispose();
            }
        };
        kopfzeile.setOpaque(false);
        kopfzeile.setBorder(new EmptyBorder(18, 20, 18, 20));

        statusText = new JLabel("Verbinde ...");
        statusText.setFont(new Font(SCHRIFTART, Font.PLAIN, 13));
        statusText.setForeground(GEDAEMPT);

        kopfzeile.add(statusText, BorderLayout.CENTER);
        return kopfzeile;
    }

    private JScrollPane chatbereichErstellen() {
        chatTextfeld = new JTextPane();
        chatTextfeld.setEditable(false);
        chatTextfeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 15));
        chatTextfeld.setForeground(SCHRIFT);
        chatTextfeld.setBackground(new Color(0xF8FBFF));
        chatTextfeld.setBorder(new EmptyBorder(18, 18, 18, 18));

        JScrollPane scrollbereich = new JScrollPane(chatTextfeld);
        scrollbereich.setBorder(BorderFactory.createLineBorder(BLAU_200));
        scrollbereich.getVerticalScrollBar().setUnitIncrement(16);
        return scrollbereich;
    }

    private JPanel eingabeleistenErstellen() {
        JPanel leiste = new JPanel(new BorderLayout(12, 0));
        leiste.setOpaque(false);

        eingabefeld = stilisiertesEingabefeld("Frage an den KI-Support schreiben ...");
        eingabefeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 15));
        eingabefeld.setEnabled(false);
        eingabefeld.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                nachrichtSenden();
            }
        });

        sendeButton = abgerundetenButtonErstellen("Senden ↵", PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 130, 46);
        sendeButton.setEnabled(false);
        sendeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                nachrichtSenden();
            }
        });

        leiste.add(eingabefeld,  BorderLayout.CENTER);
        leiste.add(sendeButton,  BorderLayout.EAST);
        return leiste;
    }

    // Stellt die Socket-Verbindung im Hintergrund her.
    private void verbinden() {
        eingabeAktivieren(false);
        statusText.setText("Verbinde ...");

        SwingWorker<Void, Void> verbindungsTask = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                verbindung = new Socket(HOST, PORT);
                eingabe = new BufferedReader(new InputStreamReader(verbindung.getInputStream(), StandardCharsets.UTF_8));
                ausgabe = new PrintWriter(new OutputStreamWriter(verbindung.getOutputStream(), StandardCharsets.UTF_8), true);
                return null;
            }

            @Override
            protected void done() {
                if (geschlossen) return;
                try {
                    get();
                    verbunden = true;
                    statusText.setText("Verbunden mit KI-Support");
                    systemTextAnhaengen("Verbunden. Du kannst deine Frage direkt stellen.");
                    senden(bereinigen(benutzername));
                    leserThreadStarten();
                    eingabeAktivieren(true);
                    eingabefeld.requestFocus();
                } catch (Exception fehler) {
                    verbunden = false;
                    statusText.setText("Nicht verbunden");
                    fehlerTextAnhaengen("Verbindung fehlgeschlagen: " + fehler.getMessage());
                }
            }
        };

        verbindungsTask.execute();
    }

    private void leserThreadStarten() {
        Thread leserThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String zeile;
                    while (verbunden && (zeile = eingabe.readLine()) != null) {
                        serverzeilVerarbeiten(zeile);
                    }
                } catch (IOException fehler) {
                    if (verbunden && !geschlossen) {
                        final String meldung = fehler.getMessage();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fehlerTextAnhaengen("Verbindung verloren: " + meldung);
                            }
                        });
                    }
                } finally {
                    if (!geschlossen) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                netzwerkTrennen();
                                statusText.setText("Nicht verbunden");
                                eingabeAktivieren(false);
                            }
                        });
                    }
                }
            }
        }, "support-chat-reader");
        leserThread.setDaemon(true);
        leserThread.start();
    }

    private void serverzeilVerarbeiten(String zeile) {
        String[] teile = zeile.split("\\|", 2);
        String typ     = teile[0];
        String inhalt  = teile.length >= 2 ? teile[1] : "";

        final String finalTyp   = typ;
        final String finalInhalt = inhalt;
        final String finalZeile  = zeile;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (geschlossen) return;
                switch (finalTyp) {
                    case "AI":
                        kiTextAnhaengen(protokolltextDekodieren(finalInhalt));
                        statusText.setText("KI-Antwort erhalten");
                        eingabeAktivieren(true);
                        eingabefeld.requestFocus();
                        break;
                    case "ERR":
                        String fehlermeldung;
                        if (finalInhalt.isEmpty()) {
                            fehlermeldung = "Unbekannter Fehler";
                        } else {
                            fehlermeldung = protokolltextDekodieren(finalInhalt);
                        }
                        fehlerTextAnhaengen(fehlermeldung);
                        statusText.setText("Fehler vom Server");
                        eingabeAktivieren(true);
                        break;
                    default:
                        systemTextAnhaengen(systempraefixEntfernen(finalZeile));
                        break;
                }
            }
        });
    }

    private void nachrichtSenden() {
        String nachricht = eingabefeld.getText().trim();
        if (nachricht.isEmpty()) {
            return;
        }
        if (!verbunden || ausgabe == null) {
            fehlerTextAnhaengen("Keine Verbindung zum KI-Support.");
            return;
        }

        eingabefeld.setText("");
        chatTextAnhaengen("Du → KI", nachricht, true);
        eingabeAktivieren(false);
        statusText.setText("Warte auf KI-Antwort ...");

        String protokollzeile = "SUPPORT|" + bereinigen(aktuellesThema)
                + "|" + bereinigen(aktuelleFrage)
                + "|" + bereinigen(nachricht);

        senden(protokollzeile);
    }

    private void senden(String zeile) {
        if (ausgabe != null) {
            ausgabe.println(zeile);
        }
    }

    private void netzwerkTrennen() {
        verbunden = false;
        try {
            if (verbindung != null) {
                verbindung.close();
            }
        } catch (IOException ignored) {}
        verbindung = null;
        eingabe    = null;
        ausgabe    = null;
    }

    private void eingabeAktivieren(boolean aktiv) {
        eingabefeld.setEnabled(aktiv);
        sendeButton.setEnabled(aktiv);
    }

    private void chatTextAnhaengen(String absender, String nachricht, boolean eigenNachricht) {
        textAnhaengen(absender + ": ", eigenNachricht ? PRIMAER : SCHRIFT, true);
        textAnhaengen(nachricht + "\n\n", SCHRIFT, false);
    }

    private void kiTextAnhaengen(String nachricht) {
        textAnhaengen("KI-Support: ", PRIMAER, true);
        textAnhaengen(nachricht + "\n\n", SCHRIFT, false);
    }

    private void systemTextAnhaengen(String nachricht) {
        textAnhaengen("System: " + nachricht + "\n\n", GEDAEMPT, false);
    }

    private void fehlerTextAnhaengen(String nachricht) {
        textAnhaengen("Fehler: " + nachricht + "\n\n", ROT, true);
    }

    private void textAnhaengen(String text, Color farbe, boolean fettgedruckt) {
        StyledDocument dokument = chatTextfeld.getStyledDocument();
        SimpleAttributeSet attribute = new SimpleAttributeSet();
        StyleConstants.setForeground(attribute, farbe);
        StyleConstants.setBold(attribute, fettgedruckt);
        StyleConstants.setFontFamily(attribute, SCHRIFTART);
        StyleConstants.setFontSize(attribute, 15);
        try {
            dokument.insertString(dokument.getLength(), text, attribute);
            chatTextfeld.setCaretPosition(dokument.getLength());
        } catch (BadLocationException ignored) {}
    }

    private JTextField stilisiertesEingabefeld(String platzhalter) {
        JTextField feld = new JTextField(platzhalter) {
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLAU_200);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        feld.setFont(new Font(SCHRIFTART, Font.PLAIN, 14));
        feld.setForeground(SCHRIFT);
        feld.setBackground(BLAU_50);
        feld.setBorder(new EmptyBorder(10, 14, 10, 14));
        return feld;
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
                    hintergrund = RAHMEN;
                }
                g2.setColor(hintergrund);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
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
        return ergebnis.toString();
    }

    private static String bereinigen(String wert) {
        if (wert == null) {
            return "";
        }
        return wert.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
    }

    private static String systempraefixEntfernen(String wert) {
        if (wert != null && wert.startsWith("[System]")) {
            return wert.substring("[System]".length()).trim();
        }
        return wert != null ? wert : "";
    }

    private static String schriftartAuswaehlen(String... kandidaten) {
        for (String name : kandidaten) {
            Font probe = new Font(name, Font.PLAIN, 12);
            if (!probe.getFamily().equalsIgnoreCase("Dialog")) {
                return name;
            }
        }
        return "SansSerif";
    }
}