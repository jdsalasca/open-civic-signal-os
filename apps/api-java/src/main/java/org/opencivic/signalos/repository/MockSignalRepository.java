package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class MockSignalRepository implements SignalRepository {
    
    @Override
    public List<Signal> findAll() {
        return List.of(
            new Signal(UUID.randomUUID(), "Broken Street Lamp", "Lights are out in Sector A", "infrastructure", 5, 4, 500, 87, 0.0, null, "NEW", LocalDateTime.now()),
            new Signal(UUID.randomUUID(), "Pothole in Main Ave", "Large pothole causing traffic issues", "infrastructure", 3, 5, 2000, 150, 0.0, null, "NEW", LocalDateTime.now().minusDays(1)),
            new Signal(UUID.randomUUID(), "Graffiti in Park", "Minor graffiti on playground equipment", "aesthetic", 1, 2, 50, 10, 0.0, null, "NEW", LocalDateTime.now().minusHours(5))
        );
    }
}
