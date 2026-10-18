# TauchbuchImporter

Liest handgeschriebene Tauchbuch-Seiten per lokalem KI-Modell aus und überträgt die Tauchgänge in das digitale
PADI-Logbuch (<https://learning.padi.com/logbook>).

## So funktioniert es

1. Foto einer Tauchbuchseite im Browser hochladen
2. Ein lokales Vision-Modell (über [Ollama](https://ollama.com) und Spring AI) liest die Seite aus und liefert die
   Tauchgänge als strukturierte Daten
3. Die erkannten Werte werden in einem Formular neben dem Originalbild angezeigt und können korrigiert werden
4. Nach der Bestätigung werden die Tauchgänge an das PADI-Logbuch gesendet

Die Bilderkennung läuft komplett lokal, es werden keine Bilder an Cloud-Dienste geschickt. Nur die bestätigten
Tauchgangsdaten gehen an PADI.

## Voraussetzungen

- Java 25 (JDK)
- [Ollama](https://ollama.com/download) mit einem Vision-Modell
- Ein PADI-Konto mit Zugang zum digitalen Logbuch
- Empfohlen: Rechner mit GPU (ca. 8 GB VRAM) oder viel Geduld auf der CPU

Maven muss nicht installiert sein, das Projekt bringt den Maven Wrapper (`mvnw`) mit.

## Ollama installieren

1. Windows-Installer von <https://ollama.com/download> herunterladen und ausführen
2. Ein neues Terminal öffnen (Eingabeaufforderung, PowerShell oder Git Bash)
3. Das Modell einmalig herunterladen (ca. 6 GB):

```bash
ollama pull qwen2.5vl:7b
```

4. Prüfen, ob es installiert ist:

```bash
ollama list
```

Ollama läuft danach als Hintergrunddienst auf `http://localhost:11434`.

## Konfiguration

Die Zugangsdaten für PADI stehen **nicht** im Repository. Lege die Datei
`src/main/resources/application-local.properties` an (sie ist in `.gitignore` eingetragen und wird nicht eingecheckt):

```properties
padi.base-url=https://logbook.global-prod.padi.com
padi.affiliate-id=DEINE_AFFILIATE_ID
padi.token=DEIN_ID_TOKEN
```

| Eigenschaft         | Bedeutung                                                                                                                                   |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `padi.base-url`     | Basis-URL der PADI-Logbuch-Schnittstelle                                                                                                    |
| `padi.affiliate-id` | Deine PADI-ID. Du findest sie in den Browser-Entwicklertools (F12, Tab Netzwerk) im Header `affiliate-id` eines Logbuch-Requests.           |
| `padi.token`        | Der **idToken** deiner PADI-Anmeldung (nicht der Access-Token), ohne das Wort `Bearer`, ohne Anführungszeichen und ohne Leerzeichen am Ende |

Hinweise zum Token:

- Siehe itToken im "refresh" Aufruf im Browser, wenn man sich bei der Website einloggt.
- Der Token läuft nach kurzer Zeit ab. Bei einem Fehler beim Export den Token im Browser neu auslesen, in die Datei
  eintragen und die Anwendung neu starten.
- Der Token gibt Zugriff auf dein PADI-Konto. Nicht weitergeben, nicht einchecken, nicht in Logs oder Chats posten.
- Alternativ kann der Token als Umgebungsvariable `PADI_TOKEN` gesetzt werden.

## Starten

Im Projektordner, mit aktiviertem Profil `local`:

**Git Bash / Linux / macOS**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

**Windows PowerShell**

```powershell
$env:SPRING_PROFILES_ACTIVE="local"; ./mvnw.cmd spring-boot:run
```

**Windows Eingabeaufforderung**

```cmd
set SPRING_PROFILES_ACTIVE=local && mvnw.cmd spring-boot:run
```

Im Log sollte `The following 1 profile is active: "local"` erscheinen.

Anschließend im Browser öffnen: <http://localhost:9090>

## Benutzung

1. Seite im Browser öffnen, Foto der Tauchbuchseite auswählen und hochladen
2. Warten, bis die Seite ausgelesen ist (je nach Rechner bis zu mehreren Minuten)
3. Erkannte Werte mit dem Originalbild vergleichen und korrigieren
4. Auf "Bestätigen und exportieren" klicken
5. Den Eintrag im PADI-Logbuch kontrollieren

Tipps für gute Ergebnisse: gleichmäßiges Licht, Foto gerade von oben, keine Schatten, hohe Auflösung.

## Wichtige Hinweise

- **Inoffizielle Schnittstelle:** PADI bietet für das Logbuch keine dokumentierte öffentliche API an. Das Projekt nutzt
  dieselbe Schnittstelle wie die PADI-Website. Sie kann sich jederzeit ändern, dann muss der Client angepasst werden.
  Nutzungsbedingungen von PADI beachten.
- **Erkennung ist nicht perfekt:** Handschrift wird nicht fehlerfrei gelesen. Die Review-Seite ist deshalb ein fester
  Bestandteil des Ablaufs, Zahlen immer kontrollieren.
- **Kein Duplikatschutz:** Wird eine Seite zweimal exportiert, entstehen doppelte Einträge im Logbuch.

## Projektstruktur

| Klasse / Datei                                      | Aufgabe                                                        |
|-----------------------------------------------------|----------------------------------------------------------------|
| `UploadController`                                  | Upload, Review-Seite und Export-Endpunkt                       |
| `TauchbuchExtractor`                                | Bild und Prompt an das Modell, Antwort als `Tauchgang`-Objekte |
| `PadiLogbookClient`                                 | Übertragung der Tauchgänge an PADI (GraphQL-Mutation)          |
| `templates/upload.html`, `review.html`, `done.html` | Weboberfläche (Thymeleaf)                                      |

## Technik

Java 25, Spring Boot, Spring AI (Ollama), Thymeleaf, Maven
