package de.ossi.tauchbuchImporter.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TauchgangForm {
    private Integer number;
    private LocalDate date;
    private LocalTime diveTimeStart;
    private LocalTime diveTimeEnd;
    private Integer duration;
    private String location;
    private String diveLocation;
    private Integer depth;
    private Double waterTemperature;
    private String comment;
}
