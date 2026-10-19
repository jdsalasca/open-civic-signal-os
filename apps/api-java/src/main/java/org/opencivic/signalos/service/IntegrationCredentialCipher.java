package org.opencivic.signalos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts provider credentials at rest, so a community's bot token is not readable in a database
 * dump.
 *
 * <p>This exists because of a real defect, not a hypothetical. The relay originally stored every
 * channel's secret through {@code hashSecret}, which is correct for a webhook signing secret that
 * never leaves the platform but wrong for a bot token that has to be sent to Telegram or WhatsApp.
 * The row then held a SHA-256 digest that the provider would reject with a 401. The connector's own
 * tests could not catch it because they constructed the entity directly with a plaintext token.
 *
 * <p>Storing the token in the clear would work and would be simpler. It would also put a
 * credential that can post to a community's group into every database dump, backup and read-only
 * replica, in a column named {@code secret_hash} where nobody would think to look for it. AES-GCM
 * is in the JDK, so this costs no dependency.
 *
 * <p>Format: {@code v1:<base64 iv>:<base64 ciphertext>}. The version prefix is what makes a future
 * key rotation possible without guessing: an old row still says {@code v1}.
 *
 * <p>GCM is authenticated, so a tampered ciphertext fails to decrypt rather than decrypting to
 * garbage. That matters here because a silently corrupted token would look like a provider outage.
 */
@Component
public class IntegrationCredentialCipher {

    private static final String VERSION = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom random = new SecureRandom();
    private final String configuredKey;
    private final SecretKeySpec key;

    public IntegrationCredentialCipher(
        @Value("${application.integration-credential-key:}") String configuredKey
    ) {
        this.configuredKey = configuredKey == null ? "" : configuredKey.trim();
        this.key = this.configuredKey.isEmpty() ? null : deriveKey(this.configuredKey);
    }

    /**
     * Whether a credential can be protected at all.
     *
     * <p>Checked before storing rather than discovered at delivery time: refusing at configuration is
     * a clear error for whoever set the integration up, whereas failing at delivery means the token
     * is already stored wrongly and the community finds out when the bulletin fails to send.
     */
    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        if (key == null) {
            throw new IllegalStateException(
                "No integration credential key is configured, so a bot token cannot be stored safely. "
                    + "Set INTEGRATION_CREDENTIAL_KEY before creating messaging integrations.");
        }
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Refusing to encrypt an empty credential.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getEncoder();
            return VERSION + ":" + encoder.encodeToString(iv) + ":" + encoder.encodeToString(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt the integration credential.", ex);
        }
    }

    public String decrypt(String payload) {
        if (key == null) {
            throw new IllegalStateException(
                "No integration credential key is configured, so a stored bot token cannot be read.");
        }
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String[] parts = payload.split(":", 3);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new IllegalStateException("Stored credential is not a recognised format.");
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] iv = decoder.decode(parts[1]);
            byte[] ciphertext = decoder.decode(parts[2]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // GCM's authentication tag is what makes this throw on a modified row. The message says so
            // because "could not decrypt" with no cause sends people hunting for encoding bugs.
            throw new IllegalStateException(
                "Stored credential could not be decrypted; it was modified or the key differs.", ex);
        }
    }

    /**
     * Accepts any non-empty passphrase and derives a 256-bit key from it.
     *
     * <p>A hex string of the right length is used directly, so an operator who generated a real key
     * gets the key they generated. Anything else is hashed, which means a short passphrase produces a
     * full-length key instead of failing at startup on a length rule nobody would remember.
     */
    private SecretKeySpec deriveKey(String value) {
        byte[] material;
        if (value.length() == 64 && isHex(value)) {
            material = hexToBytes(value);
        } else {
            try {
                material = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new IllegalStateException("Could not derive the integration credential key.", ex);
            }
        }
        return new SecretKeySpec(material, "AES");
    }

    private boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private byte[] hexToBytes(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
