package org.opencivic.signalos;

import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
	public CommandLineRunner seedUsers(
		UserRepository userRepository,
		CommunityRepository communityRepository,
		CommunityMembershipRepository membershipRepository,
		PasswordEncoder passwordEncoder
	) {
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

			User adminUser = userRepository.findByUsername("admin").orElseThrow();
			User servantUser = userRepository.findByUsername("servant").orElseThrow();
			User citizenUser = userRepository.findByUsername("citizen").orElseThrow();

			Community losRosales = upsertCommunity(
				communityRepository,
				"rosalistas",
				"Los rosales",
				"Community coordination space for Los Rosales district."
			);
			Community centralHub = upsertCommunity(
				communityRepository,
				"central-hub",
				"Central Hub",
				"Cross-community operations and visibility hub."
			);

			upsertMembership(membershipRepository, adminUser.getId(), losRosales.getId(), CommunityRole.COORDINATOR);
			upsertMembership(membershipRepository, adminUser.getId(), centralHub.getId(), CommunityRole.COORDINATOR);
			upsertMembership(membershipRepository, servantUser.getId(), losRosales.getId(), CommunityRole.PUBLIC_SERVANT_LIAISON);
			upsertMembership(membershipRepository, servantUser.getId(), centralHub.getId(), CommunityRole.PUBLIC_SERVANT_LIAISON);
			upsertMembership(membershipRepository, citizenUser.getId(), losRosales.getId(), CommunityRole.MEMBER);
			upsertMembership(membershipRepository, citizenUser.getId(), centralHub.getId(), CommunityRole.MEMBER);
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

	private Community upsertCommunity(
		CommunityRepository communityRepository,
		String slug,
		String name,
		String description
	) {
		Community community = communityRepository.findBySlug(slug).orElseGet(Community::new);
		community.setSlug(slug);
		community.setName(name);
		community.setDescription(description);
		return communityRepository.save(community);
	}

	private void upsertMembership(
		CommunityMembershipRepository membershipRepository,
		UUID userId,
		UUID communityId,
		CommunityRole role
	) {
		CommunityMembership membership = membershipRepository
			.findByUserIdAndCommunityId(userId, communityId)
			.orElseGet(CommunityMembership::new);
		membership.setUserId(userId);
		membership.setCommunityId(communityId);
		membership.setRole(role);
		membership.setCreatedBy(userId);
		membershipRepository.save(membership);
	}
}
