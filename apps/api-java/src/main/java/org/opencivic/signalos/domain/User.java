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
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
