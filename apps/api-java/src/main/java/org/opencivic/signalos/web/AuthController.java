package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.EmailService;
import org.opencivic.signalos.web.dto.UserRegistrationRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody UserRegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(
            request.username(),
            passwordEncoder.encode(request.password()),
            request.email(),
            request.role() != null ? "ROLE_" + request.role() : "ROLE_CITIZEN"
        );
        user.setEnabled(true);
        userRepository.save(user);

        // Async email sending
        new Thread(() -> emailService.sendWelcomeEmail(user.getEmail(), user.getUsername())).start();

        return Map.of("message", "User registered successfully. Welcome to Signal OS!");
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        if (authentication == null) return Map.of("role", "GUEST");
        
        return Map.of(
            "username", authentication.getName(),
            "roles", authentication.getAuthorities()
        );
    }
}
