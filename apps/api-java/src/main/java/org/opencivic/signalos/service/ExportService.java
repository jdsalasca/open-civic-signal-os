package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.web.dto.TrustPacket;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Service
public class ExportService {

    private final SignalRepository signalRepository;
    private final PrioritizationService prioritizationService;

    public ExportService(SignalRepository signalRepository, PrioritizationService prioritizationService) {
        this.signalRepository = signalRepository;
        this.prioritizationService = prioritizationService;
    }

    public ByteArrayInputStream exportSignalsToCsv() {
        List<Signal> signals = signalRepository.findAll();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            // Header
            writer.println("ID,Title,Category,Status,PriorityScore,Score_Urgency,Score_Impact,Score_People,Score_Votes,VerificationHash,CreatedAt");

            for (Signal s : signals) {
                TrustPacket packet = prioritizationService.getTrustPacket(s.getId());
                
                List<String> data = Arrays.asList(
                    s.getId().toString(),
                    escapeCsv(s.getTitle()),
                    s.getCategory(),
                    s.getStatus(),
                    String.valueOf(packet.finalScore()),
                    String.valueOf(packet.scoreBreakdown().urgency()),
                    String.valueOf(packet.scoreBreakdown().impact()),
                    String.valueOf(packet.scoreBreakdown().affectedPeople()),
                    String.valueOf(packet.scoreBreakdown().communityVotes()),
                    packet.verificationHash(),
                    s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""
                );
                writer.println(String.join(",", data));
            }
            writer.flush();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        // Clean line breaks
        String cleaned = data.replace("\n", " ").replace("\r", " ");
        // If contains comma or quotes, wrap in quotes and escape internal quotes
        if (cleaned.contains(",") || cleaned.contains("\"")) {
            cleaned = "\"" + cleaned.replace("\"", "\"\"") + "\"";
        }
        return cleaned;
    }
}
