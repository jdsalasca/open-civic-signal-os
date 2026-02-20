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
			upsertSeedUser(
				userRepository,
				passwordEncoder,
				"admin",
				"admin12345",
				"opencivicadmin@yopmail.com",
				"ROLE_SUPER_ADMIN,ROLE_PUBLIC_SERVANT,ROLE_CITIZEN"
			);
			upsertSeedUser(
				userRepository,
				passwordEncoder,
				"servant",
				"servant123",
				"servant@yopmail.com",
				"ROLE_PUBLIC_SERVANT,ROLE_CITIZEN"
			);
			upsertSeedUser(
				userRepository,
				passwordEncoder,
				"citizen",
				"citizen123",
				"citizen@yopmail.com",
				"ROLE_CITIZEN"
			);
		};
	}

	private void upsertSeedUser(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		String username,
		String rawPassword,
		String email,
		String roles
	) {
		User user = userRepository.findByUsername(username).orElseGet(() ->
			new User(username, passwordEncoder.encode(rawPassword), email, roles)
		);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setEmail(email);
		user.setRoles(roles);
		user.setVerificationCode(null);
		user.setVerified(true);
		user.setEnabled(true);
		userRepository.save(user);
	}
}
