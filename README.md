# LernCoach IT

Mein eigener Prüfungstrainer für die IHK-Abschlussprüfung als Fachinformatiker Anwendungsentwicklung.

Das Projekt ist noch Work in Progress und und entsteht parallel zu meiner Ausbildung. Ich wollte ein Tool bauen, das mir beim Lernen wirklich hilft und bei dem ich gleichzeitig Java, Swing, Netzwerkprogrammierung und eine API-Anbindung praktisch üben kann.

## Was das Programm macht

Im Programm wählt man zuerst den Prüfungsteil AP1 oder AP2 und danach ein Thema aus. Danach generiert das Programm 10 passende Übungsfragen.

Die Antworten werden als Freitext eingegeben. Anschließend bekommt man ein kurzes Feedback dazu, was schon richtig war, was noch fehlt und wie eine bessere Antwort aussehen könnte. Die Bewertung orientiert sich an typischen IHK-Erwartungen, ist aber natürlich kein offizielles Bewertungssystem.

Zusätzlich gibt es einen eingebauten KI-Support-Chat. Darüber kann man während des Lernens Rückfragen zum aktuellen Thema oder zur aktuellen Aufgabe stellen.

## Aufbau

```text
Client.java            Swing-Oberfläche mit mehreren Screens
Server.java            Startet den lokalen TCP-Server
ClientHandler.java     Verarbeitet die Nachrichten eines Clients
ExamGenerator.java     Erstellt Fragen über die OpenAI API
ApiHandler.java        Bewertet Antworten und beantwortet Support-Fragen
QuestionBank.java      Speichert die generierten Fragen im Arbeitsspeicher
SupportChatWindow.java Separates Fenster für den KI-Support
EnvLoader.java         Liest den API-Key aus der schluessel.env
```

## Voraussetzungen

* Java 17 oder neuer
* Eine Datei `schluessel.env` im Projektordner


## Starten

Zuerst den Server starten, danach den Client:

```text
javac *.java
java Server
java Client
```

## Verwendete Technik

* Java Swing für die Oberfläche
* TCP-Sockets für die Kommunikation zwischen Client und Server
* OpenAI API für Fragengenerierung, Bewertung und Support-Chat
* Einfacher eigener JSON-Parser ohne externes Framework

## KI-Nutzung

In diesem Projekt wird die OpenAI API bewusst als Teil der Anwendung genutzt. Die KI generiert Übungsfragen, bewertet Antworten und beantwortet Rückfragen im Support-Chat.

Die inhaltliche Grundlage für die Prompts stammt aus meinen AP1/AP2-Lernnotizen und aus Lernprompts, die ich im Austausch mit einem Mitschüler bekommen habe. Ich habe diese Inhalte für mein Programm angepasst und daraus System-Prompts für die Fragengenerierung und Bewertung gebaut.

Die größte Herausforderung war dabei nicht nur die API-Anbindung, sondern die Ausgabe der KI in ein festes Format zu bringen, damit das Programm die Antworten weiterverarbeiten kann.

Die Grundstruktur des einfachen JSON-Parsers ist mit KI-Unterstützung entstanden. Diesen Teil möchte ich später wahrscheinlich durch eine richtige JSON-Bibliothek ersetzen.

GUI, Server-Logik, Client-Server-Protokoll, Lernablauf und die Integration der einzelnen Klassen habe ich selbst umgesetzt und Schritt für Schritt angepasst.

## Aktueller Stand

Das Projekt funktioniert als erster Prototyp, ist aber noch nicht fertig.

Geplant sind unter anderem:

* Eigene Aufgabendatenbank mit geprüftem Lernmaterial
* Speichern von Lernfortschritten
* Verbesserung der Bewertung
* Später eventuell eine richtige JSON-Bibliothek statt eigenem Parser
