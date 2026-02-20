package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.EmailService;
import org.opencivic.signalos.service.JwtService;
import org.opencivic.signalos.web.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;

import org.opencivic.signalos.service.RateLimitService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RateLimitService rateLimitService;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          EmailService emailService, JwtService jwtService, 
                          AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegistrationRequest request) {
        // ... implementation ...
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ConflictException("Username already exists in the registry.");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Identity already registered with this email address.");
        }

        User user = new User(
            request.username(),
            passwordEncoder.encode(request.password()),
            request.email(),
            "ROLE_CITIZEN"
        );
        
        // Generate a real random 6-digit code for all profiles to test mail delivery flow
        String verificationCode = String.format("%06d", secureRandom.nextInt(999999));
        
        user.setVerificationCode(verificationCode);
        user.setVerified(false);
        user.setEnabled(false);
        
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), user.getUsername(), verificationCode);

        return ResponseEntity.ok(Map.of(
            "message", "Registration successful. A verification protocol has been initiated via email.",
            "username", user.getUsername()
        ));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(@RequestBody Map<String, String> body) {
        // ... implementation ...
        String username = body.get("username");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("Identity not found."));

        if (user.isVerified()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Account is already verified."));
        }

        // Generate a NEW code for security on every resend
        String code = String.format("%06d", secureRandom.nextInt(999999));
        user.setVerificationCode(code);
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), user.getUsername(), code);

        return ResponseEntity.ok(Map.of("message", "A new verification code has been dispatched."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        
        if (!rateLimitService.tryAcquire(username)) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many attempts. Please wait."));
        }

        String code = body.get("code");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("Identity not found."));

        if (user.isVerified()) {
            return ResponseEntity.ok(Map.of("message", "Account already verified."));
        }

        // Test Backdoor: Allow 123456 in non-prod environments for E2E stability
        boolean isTestCode = ("dev".equalsIgnoreCase(activeProfile) || "test".equalsIgnoreCase(activeProfile)) 
                             && "123456".equals(code);

        if (isTestCode || (code != null && code.equals(user.getVerificationCode()))) {
            user.setVerified(true);
            user.setEnabled(true);
            user.setVerificationCode(null);
            userRepository.save(user);
            
            // New: Send welcome email after verification
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            rateLimitService.reset(username);
            
            return ResponseEntity.ok(Map.of("message", "Protocol activation complete. Account is now active."));
        } else {
            throw new UnauthorizedActionException("Invalid activation code.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!rateLimitService.tryAcquire(request.username())) {
            throw new UnauthorizedActionException("Too many login attempts. Please wait.");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedActionException("Invalid credentials."));

        if (!user.isVerified()) {
            throw new UnauthorizedActionException("Account not verified. Please check your email.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            rateLimitService.reset(request.username());
        } catch (BadCredentialsException e) {
            throw new UnauthorizedActionException("Invalid credentials provided.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        ResponseCookie cookie = createRefreshCookie(refreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(accessToken, null, user.getRoles(), user.getUsername()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new UnauthorizedActionException("Security Error: Refresh token missing.");
        }

        try {
            String username = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String accessToken = jwtService.generateToken(userDetails);
                User user = userRepository.findByUsername(username).orElseThrow();
                return ResponseEntity.ok(new AuthResponse(accessToken, null, user.getRoles(), user.getUsername()));
            }
        } catch (Exception e) {
            throw new UnauthorizedActionException("Authentication Expired.");
        }
        
        throw new UnauthorizedActionException("Token integrity check failed.");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = createRefreshCookie("", 0);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    private ResponseCookie createRefreshCookie(String value, long maxAge) {
        boolean isProd = "prod".equalsIgnoreCase(activeProfile);
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(isProd) 
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        if (authentication == null) return Map.of("role", "GUEST");
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return Map.of("username", user.getUsername(), "roles", user.getRoles(), "email", user.getEmail(), "verified", user.isVerified());
    }
}
