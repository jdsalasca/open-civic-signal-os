package org.opencivic.signalos.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private GoogleTokenVerifier verifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private GoogleAuthService service;

    @Test
    void firstSignInCreatesAVerifiedCitizenWithADerivedUsername() {
        when(verifier.verify("credential")).thenReturn(
            new GoogleTokenVerifier.GoogleIdentity("google-sub-1", "ana@example.com", "Ana Pérez"));
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user-ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDetailsService.loadUserByUsername("user-ana")).thenReturn(
            org.springframework.security.core.userdetails.User.withUsername("user-ana")
                .password("random-hash").roles("CITIZEN").build());
        when(jwtService.generateToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        GoogleAuthService.GoogleSession session = service.signIn("credential");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("user-ana", user.getUsername());
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("google-sub-1", user.getGoogleSub());
        assertEquals("Ana Pérez", user.getDisplayName());
        assertEquals("ROLE_CITIZEN", user.getRoles());
        assertTrue(user.isVerified());
        assertTrue(user.isEnabled());
        assertNotNull(user.getPassword());
        assertEquals("access", session.accessToken());
        assertEquals("refresh", session.refreshToken());
    }

    @Test
    void anExistingEmailIsLinkedInsteadOfDuplicated() {
        User existing = new User("luis-lopez", "old-hash", "luis@example.com", "ROLE_CITIZEN");
        when(verifier.verify("credential")).thenReturn(
            new GoogleTokenVerifier.GoogleIdentity("google-sub-2", "luis@example.com", "Luis"));
        when(userRepository.findByGoogleSub("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("luis@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDetailsService.loadUserByUsername("luis-lopez")).thenReturn(
            org.springframework.security.core.userdetails.User.withUsername("luis-lopez")
                .password("old-hash").roles("CITIZEN").build());
        when(jwtService.generateToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        GoogleAuthService.GoogleSession session = service.signIn("credential");

        assertEquals("luis-lopez", session.user().getUsername());
        assertEquals("google-sub-2", existing.getGoogleSub());
        assertTrue(existing.isVerified());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void aReturningGoogleAccountDoesNotTouchTheEmailLookup() {
        User existing = new User("ana", "hash", "ana@example.com", "ROLE_CITIZEN");
        existing.setGoogleSub("google-sub-3");
        when(verifier.verify("credential")).thenReturn(
            new GoogleTokenVerifier.GoogleIdentity("google-sub-3", "ana@example.com", "Ana"));
        when(userRepository.findByGoogleSub("google-sub-3")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(
            org.springframework.security.core.userdetails.User.withUsername("ana")
                .password("hash").roles("CITIZEN").build());
        when(jwtService.generateToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        service.signIn("credential");

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository, never()).findByUsername(anyString());
        assertEquals("google-sub-3", existing.getGoogleSub());
    }
}
