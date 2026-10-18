package de.ossi.tauchbuchImporter.controller;

import de.ossi.tauchbuchImporter.model.ReviewForm;
import de.ossi.tauchbuchImporter.model.Tauchgang;
import de.ossi.tauchbuchImporter.model.TauchgangForm;
import de.ossi.tauchbuchImporter.service.PadiLogbookClient;
import de.ossi.tauchbuchImporter.service.TauchbuchExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Controller
public class UploadController {

    private final TauchbuchExtractor extractor;
    private final PadiLogbookClient padi;

    public UploadController(TauchbuchExtractor extractor, PadiLogbookClient padi) {
        this.extractor = extractor;
        this.padi = padi;
    }

    @GetMapping("/")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/extract")
    public String extract(@RequestParam MultipartFile image, Model model) throws IOException {
        if (image.isEmpty()) {
            model.addAttribute("error", "Bitte eine Bilddatei auswählen.");
            return "upload";
        }

        byte[] bytes = image.getBytes();
        String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

        List<Tauchgang> dives;
        try {
            dives = extractor.extract(bytes, contentType);
        } catch (Exception e) {
            log.error("Extraktion fehlgeschlagen", e);
            model.addAttribute("error", "Die Seite konnte nicht ausgelesen werden: " + e.getMessage());
            return "upload";
        }

        ReviewForm form = new ReviewForm();
        form.setDives(dives.stream().map(this::toForm).toList());

        model.addAttribute("reviewForm", form);
        model.addAttribute("imageData",
                "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes));
        return "review";
    }

    @PostMapping("/export")
    public String export(@ModelAttribute ReviewForm reviewForm, Model model) {
        try {
            int count = padi.export(reviewForm.getDives());
            model.addAttribute("count", count);
            return "done";
        } catch (Exception e) {
            log.error("Export fehlgeschlagen", e);
            model.addAttribute("error", "Export fehlgeschlagen: " + e.getMessage());
            return "review";
        }
    }

    private TauchgangForm toForm(Tauchgang t) {
        TauchgangForm f = new TauchgangForm();
        f.setNumber(t.number());
        f.setDate(t.date());
        f.setLocation(t.location());
        f.setDiveLocation(t.diveLocation());
        f.setDepth(t.depth());
        f.setDuration(t.duration());
        f.setDiveTimeStart(t.diveTimeStart());
        f.setDiveTimeEnd(t.diveTimeEnd());
        f.setWaterTemperature(t.waterTemperature());
        f.setComment(t.comment());
        return f;
    }
}