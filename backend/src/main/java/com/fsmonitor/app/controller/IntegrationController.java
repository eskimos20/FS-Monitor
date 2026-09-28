package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.IntegrationResponse;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.service.IntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationResponse>> getAllIntegrations() {
        return ResponseEntity.ok(integrationService.getAllIntegrations().stream()
                .map(IntegrationResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntegrationResponse> getIntegrationById(@PathVariable Long id) {
        return integrationService.getIntegrationById(id)
                .map(integration -> ResponseEntity.ok(IntegrationResponse.from(integration)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationResponse> createIntegration(@Valid @RequestBody Integration integration) {
        return ResponseEntity.ok(IntegrationResponse.from(integrationService.createIntegration(integration)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrationResponse> updateIntegration(@PathVariable Long id, @Valid @RequestBody Integration integrationDetails) {
        return ResponseEntity.ok(IntegrationResponse.from(integrationService.updateIntegration(id, integrationDetails)));
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
