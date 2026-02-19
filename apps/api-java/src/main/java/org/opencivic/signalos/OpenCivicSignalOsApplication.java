package org.opencivic.signalos;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OpenCivicSignalOsApplication {
  public static void main(String[] args) {
    SpringApplication.run(OpenCivicSignalOsApplication.class, args);
  }
}

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
class CivicSignalController {
  @GetMapping("/health")
  public String health() {
    return "open-civic-signal-os-api:ok";
  }

  @GetMapping("/signals/prioritized")
  public List<PrioritizedSignal> prioritizedSignals() {
    return List.of(
      new PrioritizedSignal(
        "sig-003",
        "Unsafe crossing near school",
        "safety",
        new ScoreBreakdown(150, 125, 30, 15.8),
        320.8,
        "NEW"
      ),
      new PrioritizedSignal(
        "sig-001",
        "Public lighting broken in sector A",
        "infrastructure",
        new ScoreBreakdown(150, 100, 30, 15.0),
        295.0,
        "IN_PROGRESS"
      ),
      new PrioritizedSignal(
        "sig-002",
        "Need digital literacy workshops",
        "education",
        new ScoreBreakdown(90, 125, 22, 12.8),
        249.8,
        "NEW"
      )
    );
  }

  record ScoreBreakdown(double urgency, double impact, double affectedPeople, double communityVotes) {}

  record PrioritizedSignal(
    String id,
    String title,
    String category,
    ScoreBreakdown scoreBreakdown,
    double priorityScore,
    String status
  ) {}
}
