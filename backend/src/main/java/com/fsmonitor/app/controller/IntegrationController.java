package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.service.IntegrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    @Autowired
    private IntegrationService integrationService;

    @GetMapping
    public ResponseEntity<List<Integration>> getAllIntegrations() {
        List<Integration> integrations = integrationService.getAllIntegrations();
        return ResponseEntity.ok(integrations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Integration> getIntegrationById(@PathVariable Long id) {
        return integrationService.getIntegrationById(id)
                .map(integration -> ResponseEntity.ok().body(integration))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integration> createIntegration(@Valid @RequestBody Integration integration) {
        Integration createdIntegration = integrationService.createIntegration(integration);
        return ResponseEntity.ok(createdIntegration);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integration> updateIntegration(@PathVariable Long id, @Valid @RequestBody Integration integrationDetails) {
        Integration updatedIntegration = integrationService.updateIntegration(id, integrationDetails);
        return ResponseEntity.ok(updatedIntegration);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        integrationService.deleteIntegration(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateIntegrationStatus(@PathVariable Long id, @RequestParam boolean isActive) {
        integrationService.updateIntegrationStatus(id, isActive);
        return ResponseEntity.ok().build();
    }
}
