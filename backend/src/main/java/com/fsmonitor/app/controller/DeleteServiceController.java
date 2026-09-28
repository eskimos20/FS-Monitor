package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.DeleteServiceResponse;
import com.fsmonitor.app.entity.DeleteService;
import com.fsmonitor.app.service.DeleteMonitoringService;
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

@RestController
@RequestMapping("/api/delete-services")
public class DeleteServiceController {

    private final DeleteMonitoringService deleteMonitoringService;

    public DeleteServiceController(DeleteMonitoringService deleteMonitoringService) {
        this.deleteMonitoringService = deleteMonitoringService;
    }

    @GetMapping
    public List<DeleteServiceResponse> getAllDeleteServices() {
        return deleteMonitoringService.findAll().stream()
                .map(DeleteServiceResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeleteServiceResponse> getDeleteService(@PathVariable Long id) {
        return deleteMonitoringService.findById(id)
                .map(service -> ResponseEntity.ok(DeleteServiceResponse.from(service)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DeleteServiceResponse createDeleteService(@RequestBody DeleteService deleteService) {
        return DeleteServiceResponse.from(deleteMonitoringService.create(deleteService));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeleteServiceResponse> updateDeleteService(@PathVariable Long id, @RequestBody DeleteService deleteService) {
        return deleteMonitoringService.update(id, deleteService)
                .map(service -> ResponseEntity.ok(DeleteServiceResponse.from(service)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeleteService(@PathVariable Long id) {
        return deleteMonitoringService.delete(id)
                ? ResponseEntity.ok().<Void>build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeleteServiceResponse> toggleDeleteService(@PathVariable Long id) {
        return deleteMonitoringService.toggle(id)
                .map(service -> ResponseEntity.ok(DeleteServiceResponse.from(service)))
                .orElse(ResponseEntity.notFound().build());
    }
}
