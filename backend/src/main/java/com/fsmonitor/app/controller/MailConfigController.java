package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.MailConfigResponse;
import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.service.MailConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mail-config")
@PreAuthorize("hasRole('ADMIN')")
public class MailConfigController {

    private final MailConfigService mailConfigService;

    public MailConfigController(MailConfigService mailConfigService) {
        this.mailConfigService = mailConfigService;
    }

    @GetMapping
    public ResponseEntity<MailConfigResponse> getCurrentMailConfig() {
        Optional<MailConfig> mailConfig = mailConfigService.getCurrentMailConfig();
        return mailConfig.map(config -> ResponseEntity.ok(MailConfigResponse.from(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MailConfigResponse> saveMailConfig(@RequestBody MailConfig mailConfig) {
        return ResponseEntity.ok(MailConfigResponse.from(mailConfigService.saveMailConfig(mailConfig)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MailConfigResponse> updateMailConfig(@PathVariable Long id, @RequestBody MailConfig mailConfig) {
        mailConfig.setId(id);
        return ResponseEntity.ok(MailConfigResponse.from(mailConfigService.saveMailConfig(mailConfig)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMailConfig(@PathVariable Long id) {
        mailConfigService.deleteMailConfig(id);
        return ResponseEntity.noContent().build();
    }
}
