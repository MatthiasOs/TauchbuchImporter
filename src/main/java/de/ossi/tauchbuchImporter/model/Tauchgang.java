package de.ossi.tauchbuchImporter.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Tauchgang(
        Integer number,
        LocalDate date,
        LocalTime diveTimeStart,
        LocalTime diveTimeEnd,
        Integer duration,
        String location,
        String diveLocation,
        Integer depth,
        Double waterTemperature,
        String instructor,
        String comment
) {
}