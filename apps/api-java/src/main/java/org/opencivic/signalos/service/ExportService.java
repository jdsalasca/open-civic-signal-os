package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Service
public class ExportService {

    private final SignalRepository signalRepository;

    public ExportService(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    public ByteArrayInputStream exportSignalsToCsv() {
        List<Signal> signals = signalRepository.findAll();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            // Header
            writer.println("ID,Title,Category,Status,PriorityScore,Urgency,Impact,AffectedPeople,Votes,CreatedAt");

            for (Signal s : signals) {
                List<String> data = Arrays.asList(
                    s.getId().toString(),
                    escapeCsv(s.getTitle()),
                    s.getCategory(),
                    s.getStatus(),
                    String.valueOf(s.getPriorityScore()),
                    String.valueOf(s.getUrgency()),
                    String.valueOf(s.getImpact()),
                    String.valueOf(s.getAffectedPeople()),
                    String.valueOf(s.getCommunityVotes()),
                    s.getCreatedAt().toString()
                );
                writer.println(String.join(",", data));
            }
            writer.flush();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\R", " ");
        if (escapedData.contains(",") || escapedData.contains(""") || escapedData.contains("'")) {
            escapedData = escapedData.replace(""", """");
            escapedData = """ + escapedData + """;
        }
        return escapedData;
    }
}
