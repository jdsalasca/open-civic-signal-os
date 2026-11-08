package org.opencivic.signalos.service;

import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a verified Google identity into a local session.
 *
 * <p>The first sign-in links the Google subject to an existing account with the same email, or
 * creates a citizen account. A local password is never required: a random one is stored so the
 * account cannot be entered through the password form until the person sets one.
 */
@Service
public class GoogleAuthService {

    private final GoogleTokenVerifier verifier;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public GoogleAuthService(
        GoogleTokenVerifier verifier,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        UserDetailsService userDetailsService,
        JwtService jwtService
    ) {
        this.verifier = verifier;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public GoogleSession signIn(String credential) {
        GoogleTokenVerifier.GoogleIdentity identity = verifier.verify(credential);

        User user = userRepository.findByGoogleSub(identity.subject()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
            if (user == null) {
                user = new User(
                    uniqueUsername(identity.email()),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    identity.email(),
                    "ROLE_CITIZEN"
                );
                user.setDisplayName(displayNameFor(identity));
            }
            user.setGoogleSub(identity.subject());
        }
        // A Google email is verified by Google, so the local account no longer needs its own
        // activation step.
        user.setVerified(true);
        user.setEnabled(true);
        user = userRepository.save(user);

        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        return new GoogleSession(
            user,
            jwtService.generateToken(details),
            jwtService.generateRefreshToken(details)
        );
    }

    private String displayNameFor(GoogleTokenVerifier.GoogleIdentity identity) {
        String name = identity.displayName();
        return name == null || name.isBlank() ? identity.email() : name.trim();
    }

    private String uniqueUsername(String email) {
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = localPart.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]", "")
            .replaceAll("^[._-]+", "");
        if (base.length() < 4) {
            base = "user-" + base;
        }
        base = base.substring(0, Math.min(base.length(), 40));

        String candidate = base;
        int suffix = 2;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base.substring(0, Math.min(base.length(), 44)) + "-" + suffix++;
        }
        return candidate;
    }

    public record GoogleSession(User user, String accessToken, String refreshToken) {}
}
