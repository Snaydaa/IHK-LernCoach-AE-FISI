import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/**
 * Backend IHK Lerncoach.
 * Akzeptiert Verbindungen und übergibt sie an dedizierte ClientHandler.
 */
public class Server {

    // Attribute
    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException {
        Path envPfad = EnvLoader.dateiPfadHolen();
        String envPfadText;
        if (envPfad == null) {
            envPfadText = "nicht gefunden";
        } else {
            envPfadText = envPfad.toString();
        }

        System.out.println("[Server] Working directory: " + System.getProperty("user.dir"));
        System.out.println("[Server] schluessel.env: " + envPfadText);
        System.out.println("[Server] OPENAI_API_KEY geladen: " + EnvLoader.schluesselVorhanden("OPENAI_API_KEY"));
        System.out.println("[Server] IHK Lerncoach Backend gestartet auf Port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientVerbindung = serverSocket.accept();
                System.out.println("[Server] Neue Verbindung: " + clientVerbindung.getInetAddress());

                // Jeder Client bekommt einen eigenen, isolierten Thread.
                Thread verbindungsThread = new Thread(new ClientHandler(clientVerbindung));
                verbindungsThread.start();
            }
        }
    }
}
