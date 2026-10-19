package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.IntegrationCredentialCipher;

/**
 * The cipher exists because the relay shipped unable to authenticate: it stored a bot token through
 * the webhook hash, so the provider received a SHA-256 digest and answered 401.
 *
 * <p>These tests are about the properties that make that class of bug visible rather than silent.
 */
class IntegrationCredentialCipherTest {

    private static final String KEY = "a-test-key-that-is-not-a-real-secret";

    @Test
    void aStoredTokenIsNotReadableFromTheRow() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher(KEY);
        String token = "1234567890:AAF-fake-bot-token-for-the-relay";

        String stored = cipher.encrypt(token);

        assertThat(stored).doesNotContain(token);
        assertThat(stored).startsWith("v1:");
    }

    @Test
    void theOriginalTokenComesBack() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher(KEY);
        assertThat(cipher.decrypt(cipher.encrypt("1234567890:AAF-token"))).isEqualTo("1234567890:AAF-token");
    }

    @Test
    void theSameTokenEncryptsDifferentlyEachTime() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher(KEY);

        // A fresh IV per row, so two communities with the same token are not visibly the same and a
        // repeated ciphertext does not give an observer a equality oracle.
        assertThat(cipher.encrypt("same-token")).isNotEqualTo(cipher.encrypt("same-token"));
    }

    @Test
    void aModifiedRowFailsInsteadOfDecryptingToGarbage() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher(KEY);
        String stored = cipher.encrypt("1234567890:AAF-token");
        String tampered = stored.substring(0, stored.length() - 4) + "AAAA";

        // GCM is authenticated. Without that, a corrupted row would look like a provider outage and
        // send someone hunting the wrong problem.
        assertThatThrownBy(() -> cipher.decrypt(tampered))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("modified or the key differs");
    }

    @Test
    void aDifferentKeyCannotReadIt() {
        String stored = new IntegrationCredentialCipher(KEY).encrypt("1234567890:AAF-token");

        assertThatThrownBy(() -> new IntegrationCredentialCipher("a-different-key").decrypt(stored))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withoutAKeyThePlatformRefusesRatherThanStoringInTheClear() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher("");

        assertThat(cipher.isConfigured()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt("1234567890:AAF-token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INTEGRATION_CREDENTIAL_KEY");
    }

    @Test
    void aHexKeyOfTheRightLengthIsUsedDirectly() {
        String hexKey = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        String stored = new IntegrationCredentialCipher(hexKey).encrypt("token");

        // Same key material, so the operator who generated it can read the row back.
        assertThat(new IntegrationCredentialCipher(hexKey).decrypt(stored)).isEqualTo("token");
    }

    @Test
    void anUnrecognisedStoredFormatIsRefused() {
        IntegrationCredentialCipher cipher = new IntegrationCredentialCipher(KEY);

        assertThatThrownBy(() -> cipher.decrypt("not-a-ciphertext"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not a recognised format");
        assertThat(cipher.decrypt(null)).isNull();
    }
}