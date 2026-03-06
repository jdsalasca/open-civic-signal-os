package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String roles; // Comma-separated

    private String displayName;
    private String civicRole;

    @Column(columnDefinition = "TEXT")
    private String affiliationsCsv;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileVisibility affiliationVisibility = ProfileVisibility.COMMUNITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterfaceMode interfaceMode = InterfaceMode.SIMPLE;

    @Column(nullable = false)
    private String avatarPreset = "civic-sunrise";

    private boolean enabled = false;
    
    // V3: Verification Flow
    private String verificationCode;
    private boolean isVerified = false;

    public User() {}

    public User(String username, String password, String email, String roles) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public List<String> getRoleList() {
        if (roles == null) return List.of();
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getCivicRole() { return civicRole; }
    public void setCivicRole(String civicRole) { this.civicRole = civicRole; }
    public String getAffiliationsCsv() { return affiliationsCsv; }
    public void setAffiliationsCsv(String affiliationsCsv) { this.affiliationsCsv = affiliationsCsv; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public ProfileVisibility getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(ProfileVisibility profileVisibility) { this.profileVisibility = profileVisibility; }
    public ProfileVisibility getAffiliationVisibility() { return affiliationVisibility; }
    public void setAffiliationVisibility(ProfileVisibility affiliationVisibility) { this.affiliationVisibility = affiliationVisibility; }
    public InterfaceMode getInterfaceMode() { return interfaceMode; }
    public void setInterfaceMode(InterfaceMode interfaceMode) { this.interfaceMode = interfaceMode; }
    public String getAvatarPreset() { return avatarPreset; }
    public void setAvatarPreset(String avatarPreset) { this.avatarPreset = avatarPreset; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public List<String> getAffiliations() {
        if (affiliationsCsv == null || affiliationsCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(affiliationsCsv.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    public void setAffiliations(List<String> affiliations) {
        if (affiliations == null || affiliations.isEmpty()) {
            this.affiliationsCsv = null;
            return;
        }
        this.affiliationsCsv = affiliations.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(","));
    }
}
