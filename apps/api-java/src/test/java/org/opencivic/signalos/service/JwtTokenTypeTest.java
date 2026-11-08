package org.opencivic.signalos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both tokens are signed with the same key and subject. Without a type claim, the 7-day refresh
 * token was accepted as a Bearer access token by every endpoint.
 */
class JwtTokenTypeTest {

    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "0123456789abcdef0123456789abcdef0123456789abcdef");
        user = User.withUsername("admin").password("unused").roles("CITIZEN").build();
    }

    @Test
    void accessAndRefreshTokensAreNotInterchangeable() {
        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        assertTrue(jwtService.isAccessToken(access));
        assertFalse(jwtService.isRefreshToken(access));
        assertTrue(jwtService.isRefreshToken(refresh));
        assertFalse(jwtService.isAccessToken(refresh));
    }
}
