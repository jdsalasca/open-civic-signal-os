package org.opencivic.signalos;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.PlaceNames;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OpenCivicSignalOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenCivicSignalOsApplication.class, args);
	}

	/**
	 * The city catalog every deployment starts with. Idempotent: an existing community with the
	 * same place key (or slug) is completed, not duplicated. Neighborhoods registered later hang
	 * off these cities through their own place key.
	 */
	@Bean
	@Profile({"dev", "prod"})
	public CommandLineRunner seedMainCities(CommunityRepository communityRepository) {
		return args -> {
			List<CitySeed> cities = List.of(
				new CitySeed("Bogotá D.C.", "Cundinamarca"),
				new CitySeed("Medellín", "Antioquia"),
				new CitySeed("Cali", "Valle del Cauca"),
				new CitySeed("Barranquilla", "Atlántico"),
				new CitySeed("Cartagena", "Bolívar"),
				new CitySeed("Bucaramanga", "Santander"),
				new CitySeed("Pereira", "Risaralda"),
				new CitySeed("Santa Marta", "Magdalena"),
				new CitySeed("Manizales", "Caldas"),
				new CitySeed("Cúcuta", "Norte de Santander")
			);
			for (CitySeed city : cities) {
				String placeKey = PlaceNames.key(city.name(), null, null);
				if (communityRepository.findByPlaceKey(placeKey).isPresent()) {
					continue;
				}
				Community community = communityRepository.findBySlug(PlaceNames.slug(city.name()))
					.orElseGet(Community::new);
				community.setName(city.name());
				community.setSlug(PlaceNames.slug(city.name()));
				community.setCountry("Colombia");
				community.setRegion(city.region());
				community.setCity(city.name());
				community.setPlaceKey(placeKey);
				if (community.getDescription() == null || community.getDescription().isBlank()) {
					community.setDescription("Espacio cívico de " + city.name() + ".");
				}
				communityRepository.save(community);
			}
			// Places created before the hierarchy existed (or by an older flow) get their city as
			// parent on the next start. Idempotent: already-linked places are skipped.
			for (Community place : communityRepository.findAll()) {
				if (place.getCity() != null && place.getParentCommunityId() == null
					&& (place.getLocality() != null || place.getNeighborhood() != null)) {
					communityRepository.findByPlaceKey(PlaceNames.key(place.getCity(), null, null))
						.ifPresent(city -> {
							place.setParentCommunityId(city.getId());
							communityRepository.save(place);
						});
				}
			}
		};
	}

	record CitySeed(String name, String region) {}

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
				"Community coordination space for Los Rosales district.",
				null
			);
			Community centralHub = upsertCommunity(
				communityRepository,
				"central-hub",
				"Central Hub",
				"Cross-community operations and visibility hub.",
				null
			);
			Community rosalistasBuildingA = upsertCommunity(
				communityRepository,
				"rosalistas-building-a",
				"Los Rosales / Building A",
				"Nested working space for Building A residents.",
				losRosales.getId()
			);

			upsertMembership(membershipRepository, adminUser.getId(), losRosales.getId(), CommunityRole.COORDINATOR);
			upsertMembership(membershipRepository, adminUser.getId(), centralHub.getId(), CommunityRole.COORDINATOR);
			upsertMembership(membershipRepository, adminUser.getId(), rosalistasBuildingA.getId(), CommunityRole.COORDINATOR);
			upsertMembership(membershipRepository, servantUser.getId(), losRosales.getId(), CommunityRole.PUBLIC_SERVANT_LIAISON);
			upsertMembership(membershipRepository, servantUser.getId(), centralHub.getId(), CommunityRole.PUBLIC_SERVANT_LIAISON);
			upsertMembership(membershipRepository, servantUser.getId(), rosalistasBuildingA.getId(), CommunityRole.PUBLIC_SERVANT_LIAISON);
			upsertMembership(membershipRepository, citizenUser.getId(), losRosales.getId(), CommunityRole.MEMBER);
			upsertMembership(membershipRepository, citizenUser.getId(), centralHub.getId(), CommunityRole.MEMBER);
			upsertMembership(membershipRepository, citizenUser.getId(), rosalistasBuildingA.getId(), CommunityRole.MEMBER);
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
		String description,
		UUID parentCommunityId
	) {
		Community community = communityRepository.findBySlug(slug).orElseGet(Community::new);
		community.setSlug(slug);
		community.setName(name);
		community.setDescription(description);
		community.setParentCommunityId(parentCommunityId);
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
