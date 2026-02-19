package org.opencivic.signalos;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class OpenCivicSignalOsApplication {
  public static void main(String[] args) {
    SpringApplication.run(OpenCivicSignalOsApplication.class, args);
  }

  @Bean
  CommandLineRunner init(SignalRepository repository) {
    return args -> {
      if (repository.count() == 0) {
        repository.saveAll(List.of(
          new Signal(UUID.randomUUID(), "Broken Street Lamp", "Lights are out in Sector A", "infrastructure", 5, 4, 500, 87, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now()),
          new Signal(UUID.randomUUID(), "Pothole in Main Ave", "Large pothole causing traffic issues", "infrastructure", 3, 5, 2000, 150, 0.0, null, "IN_PROGRESS", new ArrayList<>(), LocalDateTime.now().minusDays(1)),
          new Signal(UUID.randomUUID(), "Unsafe crossing near school", "The zebra crossing is not visible", "safety", 5, 5, 350, 79, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now())
        ));
      }
    };
  }
}
