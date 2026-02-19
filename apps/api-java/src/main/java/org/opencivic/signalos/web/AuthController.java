package org.opencivic.signalos.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.EmailService;
import org.opencivic.signalos.service.JwtService;
import org.opencivic.signalos.web.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          EmailService emailService, JwtService jwtService, 
                          AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(
            request.username(),
            passwordEncoder.encode(request.password()),
            request.email(),
            "ROLE_CITIZEN"
        );
        user.setEnabled(true);
        userRepository.save(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User registered successfully. Welcome to Signal OS!"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        User user = userRepository.findByUsername(request.username()).orElseThrow();

        // P1-5: Move Refresh Token to HttpOnly Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(accessToken, null, user.getRole(), user.getUsername()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken") String refreshToken) {
        // P1-5: Extract refresh token from cookie instead of body
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        if (jwtService.isTokenValid(refreshToken, userDetails)) {
            String accessToken = jwtService.generateToken(userDetails);
            User user = userRepository.findByUsername(username).orElseThrow();
            return ResponseEntity.ok(new AuthResponse(accessToken, null, user.getRole(), user.getUsername()));
        }
        throw new RuntimeException("Invalid Refresh Token");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // P1-5: Clear the HttpOnly cookie on logout
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        if (authentication == null) return Map.of("role", "GUEST");
        
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        
        return Map.of(
            "username", user.getUsername(),
            "role", user.getRole(),
            "email", user.getEmail()
        );
    }
}
