package org.opencivic.signalos.web;

import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.service.SignalConfirmationService;
import org.opencivic.signalos.web.dto.SignalConfirmationStateResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signals/{signalId}/confirmations")
public class SignalConfirmationController {
    private final SignalConfirmationService confirmationService;

    public SignalConfirmationController(SignalConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @GetMapping
    public SignalConfirmationStateResponse state(@PathVariable UUID signalId, Principal principal) {
        return confirmationService.state(signalId, principal.getName());
    }

    @PostMapping
    public SignalConfirmationStateResponse confirm(
        @PathVariable UUID signalId,
        @RequestBody(required = false) ConfirmRequest request,
        Principal principal
    ) {
        return confirmationService.confirm(signalId, request == null ? null : request.note(), principal.getName());
    }

    @DeleteMapping
    public SignalConfirmationStateResponse unconfirm(@PathVariable UUID signalId, Principal principal) {
        return confirmationService.unconfirm(signalId, principal.getName());
    }

    public record ConfirmRequest(@Size(max = 500) String note) {}
}
