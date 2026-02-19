package org.opencivic.signalos;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

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
  CommandLineRunner init(SignalRepository signalRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args -> {
      // Seed Admin
      if (userRepository.findByUsername("admin").isEmpty()) {
        User admin = new User("admin", passwordEncoder.encode("admin12345"), "admin@opencivic.org", "ROLE_SUPER_ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);
      }

      // Seed Staff for backward compatibility
      if (userRepository.findByUsername("servant").isEmpty()) {
        User staff = new User("servant", passwordEncoder.encode("servant2026"), "staff@opencivic.org", "ROLE_PUBLIC_SERVANT");
        staff.setEnabled(true);
        userRepository.save(staff);
      }

      // Seed Signals
      if (signalRepository.count() == 0) {
        signalRepository.saveAll(List.of(
          new Signal(UUID.randomUUID(), "Broken Street Lamp", "Lights are out in Sector A", "infrastructure", 5, 4, 500, 87, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now()),
          new Signal(UUID.randomUUID(), "Pothole in Main Ave", "Large pothole causing traffic issues", "infrastructure", 3, 5, 2000, 150, 0.0, null, "IN_PROGRESS", new ArrayList<>(), LocalDateTime.now().minusDays(1)),
          new Signal(UUID.randomUUID(), "Unsafe crossing near school", "The zebra crossing is not visible", "safety", 5, 5, 350, 79, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now())
        ));
      }
    };
  }
}
