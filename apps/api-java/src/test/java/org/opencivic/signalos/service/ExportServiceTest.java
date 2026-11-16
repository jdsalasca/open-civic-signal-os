package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.web.dto.TrustPacket;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The export already reads every signal. Fetching each one again inside the loop was an N + 1
 * hidden behind a correct CSV: the file was right and the cost grew with the number of rows.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private SignalRepository signalRepository;

    @Mock
    private PrioritizationService prioritizationService;

    @InjectMocks
    private ExportService exportService;

    @Test
    void buildsThePacketFromTheSignalAlreadyLoaded() throws Exception {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("Avenue, lighting");
        signal.setCategory("PUBLIC_SAFETY");
        signal.setStatus("NEW");
        signal.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));

        TrustPacket packet = new TrustPacket(
                signal.getId(),
                signal.getTitle(),
                signal.getStatus(),
                signal.getCreatedAt(),
                240,
                new ScoreBreakdown(3, 3, 50, 10),
                "formula",
                "hash"
        );

        when(signalRepository.findAll()).thenReturn(List.of(signal));
        when(prioritizationService.getTrustPacket(signal)).thenReturn(packet);

        String csv = new String(exportService.exportSignalsToCsv().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Avenue, lighting\"");
        assertThat(csv).contains("240");
        verify(prioritizationService).getTrustPacket(signal);
        verify(prioritizationService, never()).getTrustPacket(any(UUID.class));
    }
}
