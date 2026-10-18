package de.ossi.tauchbuchImporter.service;

import de.ossi.tauchbuchImporter.model.TauchgangForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class PadiLogbookClient {

    private static final String MUTATION = """
            mutation insert_logbook_logs($general: [logbook_logs_insert_input!]!) {
              insert_logbook_logs(objects: $general) {
                affected_rows
                returning { id dive_title dive_date dive_location status }
              }
            }
            """;

    private static final DateTimeFormatter DIVE_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter TITLE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RestClient client;
    private final String affiliateId;
    private final String token;

    public PadiLogbookClient(RestClient.Builder builder,
                             @Value("${padi.base-url}") String baseUrl,
                             @Value("${padi.affiliate-id}") String affiliateId,
                             @Value("${padi.token}") String token) {
        this.client = builder.baseUrl(baseUrl).build();
        this.affiliateId = affiliateId;
        this.token = token;
    }

    /**
     * Gibt die Anzahl der angelegten Einträge zurück.
     */
    public int export(List<TauchgangForm> dives) {
        List<Map<String, Object>> objects = dives.stream().map(this::toPadi).toList();

        Map<String, Object> body = Map.of(
                "query", MUTATION,
                "variables", Map.of("general", objects));

        Map<String, Object> response = client.post()
                .uri("/api/Logbook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("affiliate-id", affiliateId)
                .header("x-platform", "web")
                .header("Origin", "https://learning.padi.com")
                .header("Referer", "https://learning.padi.com/")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        log.info("PADI-Antwort: {}", response);

        // GraphQL meldet Fehler (z. B. abgelaufener Token) oft mit HTTP 200 und einem "errors"-Feld
        if (response == null || response.containsKey("errors")) {
            throw new IllegalStateException("PADI-Fehler: "
                    + (response == null ? "keine Antwort" : response.get("errors")));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) response.get("data"))
                .get("insert_logbook_logs");

        List<?> returning = (List<?>) data.get("returning");
        return returning != null ? returning.size() : 0;
    }

    private Map<String, Object> toPadi(TauchgangForm d) {
        if (d.getDate() == null) {
            throw new IllegalArgumentException("Tauchgang ohne Datum kann nicht exportiert werden");
        }
        String now = LocalDateTime.now().format(TIMESTAMP);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("affiliate_id", affiliateId);
        log.put("log_type", "Recreational");
        log.put("log_number", d.getNumber() != null ? d.getNumber().toString() : null);
        log.put("created_date", now);
        log.put("update_date", now);
        log.put("dive_title", title(d));
        log.put("dive_location", location(d));
        log.put("dive_date", d.getDate().format(DIVE_DATE));
        log.put("status", "Publish");

        Map<String, Object> depthTimes = new LinkedHashMap<>();
        put(depthTimes, "bottom_time", d.getDuration());
        put(depthTimes, "max_depth", d.getDepth());
        nested(log, "depth_times", depthTimes);

        Map<String, Object> conditions = new LinkedHashMap<>();
        putDecimal(conditions, "surface_water_temp", d.getWaterTemperature());
        nested(log, "conditions", conditions);

        Map<String, Object> experiences = new LinkedHashMap<>();
        put(experiences, "notes", d.getComment());
        nested(log, "experiences", experiences);

        return log;
    }

    private static String title(TauchgangForm d) {
        String date = d.getDate().format(TITLE_DATE);
        if (d.getDiveLocation() != null && !d.getDiveLocation().isBlank()) {
            return d.getDiveLocation() + " – " + date;
        }
        return (d.getNumber() != null ? "Tauchgang " + d.getNumber() : "Tauchgang") + " – " + date;
    }

    private static String location(TauchgangForm d) {
        List<String> parts = new ArrayList<>();
        if (d.getDiveLocation() != null && !d.getDiveLocation().isBlank()) parts.add(d.getDiveLocation());
        if (d.getLocation() != null && !d.getLocation().isBlank()) parts.add(d.getLocation());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static void put(Map<String, Object> m, String key, Object value) {
        if (value != null) m.put(key, value.toString());
    }

    private static void putDecimal(Map<String, Object> m, String key, Double value) {
        if (value != null) m.put(key, String.format(Locale.ROOT, "%.3f", value));
    }

    private static void nested(Map<String, Object> parent, String key, Map<String, Object> data) {
        if (!data.isEmpty()) parent.put(key, Map.of("data", data));
    }
}
