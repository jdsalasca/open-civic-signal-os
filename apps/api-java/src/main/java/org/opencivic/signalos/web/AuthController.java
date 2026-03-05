package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.InterfaceMode;
import org.opencivic.signalos.domain.ProfileVisibility;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.EmailService;
import org.opencivic.signalos.service.EmailDeliveryResult;
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
    private final CommunityMembershipRepository membershipRepository;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${support.contact.email:support@open-civic.local}")
    private String supportContactEmail;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          EmailService emailService, JwtService jwtService, 
                          AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          RateLimitService rateLimitService,
                          CommunityMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.rateLimitService = rateLimitService;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegistrationRequest request) {
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
        
        String verificationCode = String.format("%06d", secureRandom.nextInt(999999));
        
        user.setVerificationCode(verificationCode);
        user.setVerified(false);
        user.setEnabled(false);
        
        userRepository.save(user);

        EmailDeliveryResult delivery = emailService.sendVerificationCode(user.getEmail(), user.getUsername(), verificationCode);
        if (delivery.delivered()) {
            return ResponseEntity.ok(Map.of(
                "message", "Registration successful. A verification protocol has been initiated via email.",
                "username", user.getUsername(),
                "emailDeliveryStatus", "SENT",
                "supportEmail", supportContactEmail
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Registration successful, but verification email could not be delivered. Use resend or contact support.",
            "username", user.getUsername(),
            "emailDeliveryStatus", "FAILED",
            "supportEmail", supportContactEmail,
            "deliveryFailureReason", delivery.failureReason() == null ? "unknown" : delivery.failureReason()
        ));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        String username = request.username();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("Identity not found."));

        if (user.isVerified()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Account is already verified."));
        }

        // Generate a NEW code for security on every resend
        String code = String.format("%06d", secureRandom.nextInt(999999));
        user.setVerificationCode(code);
        userRepository.save(user);

        EmailDeliveryResult delivery = emailService.sendVerificationCode(user.getEmail(), user.getUsername(), code);
        if (delivery.delivered()) {
            return ResponseEntity.ok(Map.of(
                "message", "A new verification code has been dispatched.",
                "emailDeliveryStatus", "SENT",
                "supportEmail", supportContactEmail
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Could not deliver verification email. Retry later or contact support.",
            "emailDeliveryStatus", "FAILED",
            "supportEmail", supportContactEmail,
            "deliveryFailureReason", delivery.failureReason() == null ? "unknown" : delivery.failureReason()
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@Valid @RequestBody VerifyAccountRequest request) {
        String username = request.username();
        
        if (!rateLimitService.tryAcquire(username)) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many attempts. Please wait."));
        }

        String code = request.code();

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
        return Map.ofEntries(
            Map.entry("username", user.getUsername()),
            Map.entry("roles", user.getRoles()),
            Map.entry("email", user.getEmail()),
            Map.entry("verified", user.isVerified()),
            Map.entry("displayName", displayNameFor(user)),
            Map.entry("civicRole", user.getCivicRole()),
            Map.entry("affiliations", user.getAffiliations()),
            Map.entry("bio", user.getBio()),
            Map.entry("profileVisibility", user.getProfileVisibility().name()),
            Map.entry("affiliationVisibility", user.getAffiliationVisibility().name()),
            Map.entry("interfaceMode", user.getInterfaceMode().name())
        );
    }

    @GetMapping("/profile/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        User user = requireAuthenticatedUser(authentication);
        return toProfileResponse(user, ViewerScope.ADMINS, true);
    }

    @PutMapping("/profile/me")
    public UserProfileResponse updateMyProfile(
        @Valid @RequestBody UpdateUserProfileRequest request,
        Authentication authentication
    ) {
        User user = requireAuthenticatedUser(authentication);
        user.setDisplayName(trimToNull(request.displayName()));
        user.setCivicRole(trimToNull(request.civicRole()));
        user.setBio(trimToNull(request.bio()));
        user.setAffiliations(sanitizeAffiliations(request.affiliations()));
        user.setProfileVisibility(request.profileVisibility() == null ? ProfileVisibility.PUBLIC : request.profileVisibility());
        user.setAffiliationVisibility(
            request.affiliationVisibility() == null ? ProfileVisibility.COMMUNITY : request.affiliationVisibility()
        );
        user.setInterfaceMode(request.interfaceMode() == null ? InterfaceMode.SIMPLE : request.interfaceMode());
        userRepository.save(user);
        return toProfileResponse(user, ViewerScope.ADMINS, true);
    }

    @GetMapping("/profile/{username}")
    public UserProfileResponse getProfile(
        @PathVariable String username,
        Authentication authentication,
        @RequestHeader(name = "X-Community-Id", required = false) UUID communityId
    ) {
        User viewedUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found."));
        ViewerScope viewerScope = resolveViewerScope(authentication, viewedUser, communityId);
        return toProfileResponse(viewedUser, viewerScope, false);
    }

    private User requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedActionException("Authentication required.");
        }
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private ViewerScope resolveViewerScope(Authentication authentication, User viewedUser, UUID communityId) {
        if (authentication == null) {
            return ViewerScope.PUBLIC;
        }

        User viewer = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (viewer == null) {
            return ViewerScope.PUBLIC;
        }

        if (viewer.getId().equals(viewedUser.getId()) || viewer.getRoleList().contains("ROLE_SUPER_ADMIN")) {
            return ViewerScope.ADMINS;
        }

        if (communityId != null) {
            var viewerMembership = membershipRepository.findByUserIdAndCommunityId(viewer.getId(), communityId);
            var viewedMembership = membershipRepository.findByUserIdAndCommunityId(viewedUser.getId(), communityId);
            if (viewerMembership.isPresent() && viewedMembership.isPresent()) {
                return isAdminCommunityRole(viewerMembership.get().getRole()) ? ViewerScope.ADMINS : ViewerScope.COMMUNITY;
            }
        }

        Set<UUID> viewedCommunities = new HashSet<>();
        membershipRepository.findByUserId(viewedUser.getId())
            .forEach(membership -> viewedCommunities.add(membership.getCommunityId()));

        for (CommunityMembership membership : membershipRepository.findByUserId(viewer.getId())) {
            if (viewedCommunities.contains(membership.getCommunityId())) {
                return isAdminCommunityRole(membership.getRole()) ? ViewerScope.ADMINS : ViewerScope.COMMUNITY;
            }
        }

        return ViewerScope.PUBLIC;
    }

    private boolean isAdminCommunityRole(CommunityRole role) {
        return role == CommunityRole.MODERATOR
            || role == CommunityRole.COORDINATOR
            || role == CommunityRole.PUBLIC_SERVANT_LIAISON;
    }

    private UserProfileResponse toProfileResponse(User user, ViewerScope viewerScope, boolean includeEmail) {
        String email = includeEmail || viewerScope == ViewerScope.ADMINS ? user.getEmail() : null;
        boolean revealProfile = canReveal(user.getProfileVisibility(), viewerScope);
        String displayName = revealProfile ? displayNameFor(user) : user.getUsername();
        String civicRole = revealProfile ? user.getCivicRole() : null;
        String bio = revealProfile ? user.getBio() : null;
        List<String> affiliations = canReveal(user.getAffiliationVisibility(), viewerScope) ? user.getAffiliations() : List.of();

        return new UserProfileResponse(
            user.getUsername(),
            displayName,
            email,
            user.isVerified(),
            civicRole,
            bio,
            affiliations,
            user.getProfileVisibility(),
            user.getAffiliationVisibility(),
            user.getInterfaceMode(),
            viewerScope.name()
        );
    }

    private boolean canReveal(ProfileVisibility visibility, ViewerScope viewerScope) {
        if (viewerScope == ViewerScope.ADMINS) {
            return true;
        }
        return switch (visibility) {
            case PUBLIC -> true;
            case COMMUNITY -> viewerScope == ViewerScope.COMMUNITY;
            case ADMINS -> false;
        };
    }

    private String displayNameFor(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
            ? user.getUsername()
            : user.getDisplayName();
    }

    private List<String> sanitizeAffiliations(List<String> affiliations) {
        if (affiliations == null) {
            return List.of();
        }
        return affiliations.stream()
            .map(this::trimToNull)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .limit(8)
            .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private enum ViewerScope {
        PUBLIC,
        COMMUNITY,
        ADMINS
    }
}
