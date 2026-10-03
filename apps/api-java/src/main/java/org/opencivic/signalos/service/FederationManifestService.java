package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import org.opencivic.signalos.domain.CommunityOpenDataExportType;
import org.springframework.stereotype.Service;

/**
 * The federation manifest: what this instance can serve another instance, and in what shape.
 *
 * <p>City-to-city compatibility fails in a specific way. Two instances running different versions
 * will each assume the other speaks their dialect, and the mismatch surfaces as a parse error deep
 * in an importer rather than as a clear "we do not speak the same version". A manifest makes the
 * contract explicit and checkable before anything is exchanged.
 *
 * <p>Three things this deliberately does not do:
 *
 * <ul>
 *   <li><b>It does not claim compatibility it cannot verify.</b> The manifest states the contract
 *       version this instance speaks. Whether another instance speaks it is that instance's claim,
 *       and a consumer checks rather than assumes.
 *   <li><b>It does not expose anything a token would not.</b> The manifest lists capabilities, not
 *       data. Reading an export still requires a scoped token, exactly as before.
 *   <li><b>It does not promise stability it cannot keep.</b> The version is a number that changes
 *       when the shape changes, and the manifest says what changed.
 * </ul>
 */
@Service
public class FederationManifestService {

    /**
     * The contract version this instance speaks.
     *
     * <p>Bumped when the shape of a federated export changes in a way a consumer would notice.
     * A consumer that does not recognise this number should refuse rather than guess.
     */
    public static final String CONTRACT_VERSION = "v1";

    /** The versions this instance can still read, so an older peer is not cut off immediately. */
    private static final List<String> SUPPORTED_CONTRACT_VERSIONS = List.of("v1");

    private final CommunityOpenDataService openDataService;

    public FederationManifestService(CommunityOpenDataService openDataService) {
        this.openDataService = openDataService;
    }

    public record FederatedDataset(
        String exportType,
        String scope,
        String description,
        List<String> formats,
        String contractVersion
    ) {}

    public record FederationManifest(
        String instanceName,
        String contractVersion,
        List<String> supportedContractVersions,
        List<FederatedDataset> datasets,
        String authentication,
        String rateLimitHeader,
        String interpretation,
        LocalDateTime generatedAt
    ) {}

    /**
     * The manifest for this instance.
     *
     * <p>Public and unauthenticated on purpose: a peer has to be able to read the contract before it
     * can decide whether to ask for a token. The manifest carries no data, so there is nothing to
     * protect.
     */
    public FederationManifest manifest(String instanceName) {
        List<FederatedDataset> datasets = openDataService.exportDefinitions().stream()
            .map(definition -> new FederatedDataset(
                definition.exportType(),
                definition.requiredScope(),
                definition.description(),
                definition.availableFormats(),
                CONTRACT_VERSION
            ))
            .toList();

        return new FederationManifest(
            instanceName == null || instanceName.isBlank() ? "open-civic-signal-os" : instanceName.trim(),
            CONTRACT_VERSION,
            SUPPORTED_CONTRACT_VERSIONS,
            datasets,
            "X-Api-Token header carrying a scoped open-data token issued by the serving community.",
            "X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset",
            interpretation(),
            LocalDateTime.now()
        );
    }

    /**
     * Says what a consumer must do and what this manifest does not promise.
     *
     * <p>A peer reading "contractVersion v1" could wrongly conclude that any v1 peer is compatible.
     * The version says what shape this instance serves; it does not say another instance serves the
     * same one, and a consumer has to check.
     */
    private String interpretation() {
        return "This describes what this instance can serve another instance, and in what shape. "
            + "It does NOT promise that another instance speaks the same contract: a consumer must read "
            + "the peer's own manifest and compare contractVersion before exchanging anything, because "
            + "two instances running different versions will each assume the other speaks their dialect "
            + "and the mismatch surfaces as a parse error rather than as a clear refusal. "
            + "The manifest carries no data; reading an export still requires a scoped token issued by "
            + "the serving community. A consumer that does not recognise contractVersion should refuse "
            + "rather than guess.";
    }

    /**
     * Whether this instance can read a peer's contract version.
     *
     * <p>Exposed so a consumer can ask rather than infer. An unrecognised version is a refusal, not a
     * best-effort parse.
     */
    public boolean canRead(String peerContractVersion) {
        return peerContractVersion != null
            && SUPPORTED_CONTRACT_VERSIONS.contains(peerContractVersion.trim());
    }

    /** The export types this instance serves, for a consumer building a request. */
    public List<CommunityOpenDataExportType> servedExportTypes() {
        return openDataService.exportDefinitions().stream()
            .map(definition -> CommunityOpenDataExportType.valueOf(definition.exportType()))
            .toList();
    }
}