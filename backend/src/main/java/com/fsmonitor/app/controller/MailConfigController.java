package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.service.MailConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/mail-config")
@PreAuthorize("hasRole('ADMIN')")
public class MailConfigController {
    
    @Autowired
    private MailConfigService mailConfigService;
    
    @GetMapping
    public ResponseEntity<MailConfig> getCurrentMailConfig() {
        Optional<MailConfig> mailConfig = mailConfigService.getCurrentMailConfig();
        return mailConfig.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<MailConfig> saveMailConfig(@RequestBody MailConfig mailConfig) {
        MailConfig savedConfig = mailConfigService.saveMailConfig(mailConfig);
        return ResponseEntity.ok(savedConfig);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MailConfig> updateMailConfig(@PathVariable Long id, @RequestBody MailConfig mailConfig) {
        mailConfig.setId(id);
        MailConfig updatedConfig = mailConfigService.saveMailConfig(mailConfig);
        return ResponseEntity.ok(updatedConfig);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMailConfig(@PathVariable Long id) {
        mailConfigService.deleteMailConfig(id);
        return ResponseEntity.noContent().build();
    }
}
