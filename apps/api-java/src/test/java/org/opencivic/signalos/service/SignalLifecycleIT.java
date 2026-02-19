package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SignalLifecycleIT {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private SignalRepository signalRepository;

    @Test
    void shouldValidateStatusTransitions() {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setStatus(SignalStatus.NEW.name());
        signalRepository.save(signal);

        // Valid transition
        assertDoesNotThrow(() -> prioritizationService.updateStatus(signal.getId(), SignalStatus.IN_PROGRESS.name()));
        
        // Invalid transition from RESOLVED
        prioritizationService.updateStatus(signal.getId(), SignalStatus.RESOLVED.name());
        assertThrows(RuntimeException.class, () -> 
            prioritizationService.updateStatus(signal.getId(), SignalStatus.IN_PROGRESS.name()));
    }

    @Test
    void shouldPreventMergingSignalWithItself() {
        UUID id = UUID.randomUUID();
        assertThrows(ConflictException.class, () -> 
            prioritizationService.mergeSignals(id, List.of(id)));
    }

    @Test
    void shouldFailWhenSignalNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> 
            prioritizationService.getSignalById(UUID.randomUUID())
                .orElseThrow(() -> new ResourceNotFoundException("Not found")));
    }
}
