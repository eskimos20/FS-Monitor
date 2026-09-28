package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.ServiceResponse;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;
import com.fsmonitor.app.service.ConnectionTestService;
import com.fsmonitor.app.service.CredentialsTestService;
import com.fsmonitor.app.service.ServiceMonitoringService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceMonitoringService serviceMonitoringService;
    private final ConnectionTestService connectionTestService;
    private final CredentialsTestService credentialsTestService;

    public ServiceController(ServiceMonitoringService serviceMonitoringService,
                             ConnectionTestService connectionTestService,
                             CredentialsTestService credentialsTestService) {
        this.serviceMonitoringService = serviceMonitoringService;
        this.connectionTestService = connectionTestService;
        this.credentialsTestService = credentialsTestService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        return ResponseEntity.ok(serviceMonitoringService.getAllServices().stream()
                .map(ServiceResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        Service service = serviceMonitoringService.getServiceById(id);
        return service != null
                ? ResponseEntity.ok(ServiceResponse.from(service))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(@RequestBody Service service) {
        // Set default values if not provided
        if (service.getIsActive() == null) {
            service.setIsActive(true);
        }
        if (service.getCheckIntervalMinutes() == null) {
            service.setCheckIntervalMinutes(5);
        }

        return ResponseEntity.ok(ServiceResponse.from(serviceMonitoringService.saveService(service)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(@PathVariable Long id, @RequestBody Service serviceDetails) {
        Service existingService = serviceMonitoringService.getServiceById(id);
        if (existingService == null) {
            return ResponseEntity.notFound().build();
        }

        // Update fields
        existingService.setName(serviceDetails.getName());
        existingService.setType(serviceDetails.getType());
        existingService.setCheckMethod(serviceDetails.getCheckMethod());
        existingService.setHost(serviceDetails.getHost());
        existingService.setPort(serviceDetails.getPort());
        existingService.setPath(serviceDetails.getPath());
        existingService.setSharePath(serviceDetails.getSharePath());
        existingService.setIsActive(serviceDetails.getIsActive());
        existingService.setCheckIntervalMinutes(serviceDetails.getCheckIntervalMinutes());
        existingService.setScheduleEnabled(serviceDetails.getScheduleEnabled());
        existingService.setActiveDays(serviceDetails.getActiveDays());
        existingService.setActiveStartHour(serviceDetails.getActiveStartHour());
        existingService.setActiveEndHour(serviceDetails.getActiveEndHour());
        existingService.setUseCredentials(serviceDetails.getUseCredentials());
        existingService.setUsername(serviceDetails.getUsername());

        // Credentials are write-only: only overwrite when a new value is sent
        if (serviceDetails.getPassword() != null && !serviceDetails.getPassword().isBlank()) {
            existingService.setPassword(serviceDetails.getPassword());
        }
        if (serviceDetails.getPrivateKey() != null && !serviceDetails.getPrivateKey().isBlank()) {
            existingService.setPrivateKey(serviceDetails.getPrivateKey());
        }

        return ResponseEntity.ok(ServiceResponse.from(serviceMonitoringService.saveService(existingService)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        if (serviceMonitoringService.getServiceById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        serviceMonitoringService.deleteService(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> request) {
        String host = (String) request.get("host");
        Integer port = request.get("port") != null ? ((Number) request.get("port")).intValue() : null;
        String path = (String) request.get("path");

        if (host == null || host.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Host is required"));
        }

        Map<String, Object> result = connectionTestService.testConnection(host, port, path);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testCredentials(@RequestBody Service service) {
        if (service.getHost() == null || service.getHost().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Host is required"));
        }

        if (!Boolean.TRUE.equals(service.getUseCredentials()) || service.getUsername() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Credentials are required"));
        }

        Map<String, Object> result = credentialsTestService.testCredentials(service);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/types")
    public ResponseEntity<ServiceType[]> getServiceTypes() {
        return ResponseEntity.ok(ServiceType.values());
    }
}
