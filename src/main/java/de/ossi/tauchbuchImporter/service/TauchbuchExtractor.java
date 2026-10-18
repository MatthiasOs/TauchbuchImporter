package de.ossi.tauchbuchImporter.service;

import de.ossi.tauchbuchImporter.model.TauchbuchSeite;
import de.ossi.tauchbuchImporter.model.Tauchgang;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.List;

@Slf4j
@Service
public class TauchbuchExtractor {

    private static final String PROMPT = """
            Das Bild zeigt eine handgeschriebene Seite aus einem Tauchlogbuch.
            Extrahiere jeden Tauchgang auf der Seite.
            Regeln:
            - Erfinde nichts. Wenn ein Wert nicht lesbar oder nicht vorhanden ist, setze null.
            - Tiefe in Metern, Zeit in Minuten, Druck in bar, Datum im Format YYYY-MM-DD.
            - Gib nur die Daten zurück, keine Erklärungen.
            - Speichere die Einheiten der Werte in einem extra Feld ab.
            """;

    private final ChatClient chatClient;

    public TauchbuchExtractor(ChatClient.Builder builder) {
        this.chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
    }

    public List<Tauchgang> extract(byte[] image, String contentType) {
        TauchbuchSeite seite = chatClient.prompt()
                .user(u -> u
                        .text(PROMPT)
                        .media(MimeType.valueOf(contentType), new ByteArrayResource(image)))
                .call()
                .entity(TauchbuchSeite.class);

        return seite != null && seite.tauchgaenge() != null
                ? seite.tauchgaenge()
                : List.of();
    }
}
