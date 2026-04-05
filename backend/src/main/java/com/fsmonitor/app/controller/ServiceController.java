package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;
import com.fsmonitor.app.service.ServiceMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceMonitoringService serviceMonitoringService;

    @GetMapping
    public ResponseEntity<List<Service>> getAllServices() {
        List<Service> services = serviceMonitoringService.getAllServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable Long id) {
        Service service = serviceMonitoringService.getServiceById(id);
        if (service != null) {
            return ResponseEntity.ok(service);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Service> createService(@RequestBody Service service) {
        // Set default values if not provided
        if (service.getIsActive() == null) {
            service.setIsActive(true);
        }
        if (service.getCheckIntervalMinutes() == null) {
            service.setCheckIntervalMinutes(5);
        }
        
        Service savedService = serviceMonitoringService.saveService(service);
        return ResponseEntity.ok(savedService);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Service> updateService(@PathVariable Long id, @RequestBody Service serviceDetails) {
        Service existingService = serviceMonitoringService.getServiceById(id);
        if (existingService == null) {
            return ResponseEntity.notFound().build();
        }

        // Update fields
        existingService.setName(serviceDetails.getName());
        existingService.setType(serviceDetails.getType());
        existingService.setHost(serviceDetails.getHost());
        existingService.setPort(serviceDetails.getPort());
        existingService.setPath(serviceDetails.getPath());
        existingService.setIsActive(serviceDetails.getIsActive());
        existingService.setCheckIntervalMinutes(serviceDetails.getCheckIntervalMinutes());

        Service updatedService = serviceMonitoringService.saveService(existingService);
        return ResponseEntity.ok(updatedService);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        Service existingService = serviceMonitoringService.getServiceById(id);
        if (existingService == null) {
            return ResponseEntity.notFound().build();
        }

        serviceMonitoringService.deleteService(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/types")
    public ResponseEntity<ServiceType[]> getServiceTypes() {
        return ResponseEntity.ok(ServiceType.values());
    }
}
