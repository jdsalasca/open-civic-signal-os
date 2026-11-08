package org.opencivic.signalos.service;

import java.util.Locale;
import java.util.Set;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Validates the ID token that Google Identity Services hands to the browser.
 *
 * <p>The token is signed by Google, so the backend only needs the public client id: no client
 * secret, no redirect and no session. Signature and expiry are checked by the decoded; issuer,
 * audience and the verified-email flag are checked here because a token minted for another app
 * would otherwise be accepted.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> VALID_ISSUERS = Set.of(
        "https://accounts.google.com",
        "accounts.google.com"
    );

    private final NimbusJwtDecoder decoder;
    private final String clientId;

    public GoogleTokenVerifier(@Value("${app.auth.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
    }

    public boolean isConfigured() {
        return !clientId.isBlank();
    }

    public String clientId() {
        return clientId;
    }

    public GoogleIdentity verify(String credential) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google sign-in is not configured.");
        }
        if (credential == null || credential.isBlank()) {
            throw new UnauthorizedActionException("Missing Google credential.");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (JwtException ex) {
            throw new UnauthorizedActionException("Google credential is invalid or expired.");
        }

        String issuer = jwt.getClaimAsString("iss");
        if (issuer == null || !VALID_ISSUERS.contains(issuer)) {
            throw new UnauthorizedActionException("Google credential was issued by an unknown issuer.");
        }
        if (jwt.getAudience() == null || !jwt.getAudience().contains(clientId)) {
            throw new UnauthorizedActionException("Google credential was issued for another application.");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new UnauthorizedActionException("Google account email is not verified.");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new UnauthorizedActionException("Google credential does not identify an account.");
        }

        return new GoogleIdentity(
            subject,
            email.toLowerCase(Locale.ROOT),
            jwt.getClaimAsString("name")
        );
    }

    public record GoogleIdentity(String subject, String email, String displayName) {}
}
