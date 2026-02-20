package org.opencivic.signalos.web;

import org.opencivic.signalos.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestEmailController {

    private final EmailService emailService;

    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/email")
    public String testEmail(@RequestParam String to) {
        emailService.sendVerificationCode(to, "Savatar", "987654");
        return "Command sent to: " + to + ". Monitoring logs for Gmail API response...";
    }
}
