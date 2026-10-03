package org.opencivic.signalos.web;

import org.opencivic.signalos.service.FederationManifestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The federation manifest.
 *
 * <p>Public and unauthenticated on purpose: a peer has to be able to read the contract before it can
 * decide whether to ask for a token. The manifest carries no data, so there is nothing to protect.
 */
@RestController
@RequestMapping("/api/federation")
public class FederationManifestController {

    private final FederationManifestService manifestService;
    private final String instanceName;

    public FederationManifestController(
        FederationManifestService manifestService,
        @Value("${app.instance-name:open-civic-signal-os}") String instanceName
    ) {
        this.manifestService = manifestService;
        this.instanceName = instanceName;
    }

    @GetMapping("/manifest")
    public FederationManifestService.FederationManifest manifest() {
        return manifestService.manifest(instanceName);
    }

    /**
     * Whether this instance can read a peer's contract version.
     *
     * <p>Exposed so a consumer can ask rather than infer. An unrecognised version is a refusal, not a
     * best-effort parse.
     */
    @GetMapping("/compatibility")
    public CompatibilityResponse compatibility(@RequestParam String peerContractVersion) {
        boolean readable = manifestService.canRead(peerContractVersion);
        return new CompatibilityResponse(
            peerContractVersion,
            FederationManifestService.CONTRACT_VERSION,
            readable,
            readable
                ? "This instance can read contract " + peerContractVersion + "."
                : "This instance cannot read contract " + peerContractVersion
                    + ". Refusing rather than guessing: a best-effort parse of an unrecognised shape "
                    + "would produce data that looks valid and is not."
        );
    }

    public record CompatibilityResponse(
        String peerContractVersion,
        String localContractVersion,
        boolean readable,
        String explanation
    ) {}
}