package de.ossi.tauchbuchImporter.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewForm {
    private List<TauchgangForm> dives = new ArrayList<>();
}