package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_integrations")
public class CommunityIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityIntegrationChannel channel;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetUri;

    /**
     * Hash of a webhook signing secret. Null for channels whose credential has to be sent to a
     * provider, because a hash of a token is not the token.
     */
    @Column(length = 255)
    private String secretHash;

    /** AES-GCM ciphertext of a bot token. Never exposed through the API. */
    @Column(columnDefinition = "TEXT")
    private String credentialCiphertext;

    /**
     * The sending identity a provider needs in the path, currently the WhatsApp phone-number id.
     *
     * <p>Not a destination: the recipient stays in {@code targetUri}. WhatsApp's Cloud API puts the
     * business number in the URL and the resident's number in the body, so one field cannot be both.
     */
    @Column(length = 64)
    private String providerResourceId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastAttemptAt;

    private LocalDateTime lastSuccessAt;

    @Column(nullable = false)
    private int consecutiveFailures = 0;

    @Column(nullable = false)
    private boolean autoRetry = true;

    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public CommunityIntegrationChannel getChannel() { return channel; }
    public void setChannel(CommunityIntegrationChannel channel) { this.channel = channel; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTargetUri() { return targetUri; }
    public void setTargetUri(String targetUri) { this.targetUri = targetUri; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }
    public String getCredentialCiphertext() { return credentialCiphertext; }
    public void setCredentialCiphertext(String credentialCiphertext) { this.credentialCiphertext = credentialCiphertext; }
    public String getProviderResourceId() { return providerResourceId; }
    public void setProviderResourceId(String providerResourceId) { this.providerResourceId = providerResourceId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public LocalDateTime getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(LocalDateTime lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public boolean isAutoRetry() { return autoRetry; }
    public void setAutoRetry(boolean autoRetry) { this.autoRetry = autoRetry; }
}