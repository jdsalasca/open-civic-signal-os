package org.opencivic.signalos;

import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OpenCivicSignalOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenCivicSignalOsApplication.class, args);
	}

	@Bean
	@Profile({"dev", "test"})
	public CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				// Admin has all roles
				User admin = new User(
					"admin", 
					passwordEncoder.encode("admin12345"), 
					"admin-signalos@yopmail.com", 
					"ROLE_SUPER_ADMIN,ROLE_PUBLIC_SERVANT,ROLE_CITIZEN"
				);
				admin.setEnabled(true);
				userRepository.save(admin);
			}
			if (userRepository.findByUsername("servant").isEmpty()) {
				// Servant has two roles
				User servant = new User(
					"servant", 
					passwordEncoder.encode("servant123"), 
					"servant-signalos@yopmail.com", 
					"ROLE_PUBLIC_SERVANT,ROLE_CITIZEN"
				);
				servant.setEnabled(true);
				userRepository.save(servant);
			}
		};
	}
}
