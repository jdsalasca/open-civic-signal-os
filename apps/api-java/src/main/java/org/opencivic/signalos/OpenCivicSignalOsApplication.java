package org.opencivic.signalos;

import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class OpenCivicSignalOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenCivicSignalOsApplication.class, args);
    }

    @Bean
    @Profile({"dev", "test"}) // BE-P1-05: Seed only in non-production profiles
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                userRepository.save(new User("admin", passwordEncoder.encode("admin12345"), "admin@signalos.org", "ROLE_SUPER_ADMIN"));
                userRepository.save(new User("servant", passwordEncoder.encode("servant2026"), "staff@signalos.org", "ROLE_PUBLIC_SERVANT"));
                userRepository.save(new User("citizen", passwordEncoder.encode("citizen2026"), "citizen@signalos.org", "ROLE_CITIZEN"));
                System.out.println("Dev Fixtures: Seed users created.");
            }
        };
    }
}
