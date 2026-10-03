package org.opencivic.signalos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * The stable citation for a signal handed to an institution.
 *
 * <p>Extracted so the export and the handoff cannot disagree. The export writes this reference into
 * the file a city ingests; the handoff records it as the key the community will quote back. If the
 * two computed it differently, a community would hand over a ticket under one reference and then be
 * unable to match anything the city said about it.
 *
 * <p>Deterministic by construction: derived from the signal id, so regenerating an export does not
 * invent new references and orphan the city's history.
 */
public final class InstitutionalTicketReference {

    private static final String PREFIX = "OCS-";
    private static final int HASH_CHARS = 16;

    private InstitutionalTicketReference() {}

    public static String forSignal(UUID signalId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signalId.toString().getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(hash)
                .substring(0, HASH_CHARS)
                .toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed by the platform spec; if it is missing the JVM is broken.
            throw new IllegalStateException("SHA-256 unavailable, cannot build a stable ticket reference", ex);
        }
    }
}