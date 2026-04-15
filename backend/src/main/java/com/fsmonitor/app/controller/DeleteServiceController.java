package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.DeleteService;
import com.fsmonitor.app.repository.DeleteServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delete-services")
@CrossOrigin(origins = "*")
public class DeleteServiceController {

    private static final Logger logger = LoggerFactory.getLogger(DeleteServiceController.class);

    @Autowired
    private DeleteServiceRepository deleteServiceRepository;

    @GetMapping
    public List<DeleteService> getAllDeleteServices() {
        return deleteServiceRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeleteService> getDeleteService(@PathVariable Long id) {
        return deleteServiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DeleteService createDeleteService(@RequestBody DeleteService deleteService) {
        logger.info("Creating delete service: {}", deleteService.getName());
        return deleteServiceRepository.save(deleteService);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeleteService> updateDeleteService(@PathVariable Long id, @RequestBody DeleteService deleteService) {
        return deleteServiceRepository.findById(id)
                .map(existing -> {
                    existing.setName(deleteService.getName());
                    existing.setPath(deleteService.getPath());
                    existing.setFileTypes(deleteService.getFileTypes());
                    existing.setCleanupEnabled(deleteService.getCleanupEnabled());
                    existing.setCleanupIntervalValue(deleteService.getCleanupIntervalValue());
                    existing.setCleanupIntervalUnit(deleteService.getCleanupIntervalUnit());
                    existing.setDeleteAgeValue(deleteService.getDeleteAgeValue());
                    existing.setDeleteAgeUnit(deleteService.getDeleteAgeUnit());
                    existing.setRecursive(deleteService.getRecursive());
                    existing.setDeleteEmptyDirectories(deleteService.getDeleteEmptyDirectories());
                    
                    logger.info("Updating delete service: {}", existing.getName());
                    return ResponseEntity.ok(deleteServiceRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeleteService(@PathVariable Long id) {
        return deleteServiceRepository.findById(id)
                .map(service -> {
                    logger.info("Deleting delete service: {}", service.getName());
                    deleteServiceRepository.delete(service);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<DeleteService> toggleDeleteService(@PathVariable Long id) {
        return deleteServiceRepository.findById(id)
                .map(service -> {
                    service.setCleanupEnabled(!service.getCleanupEnabled());
                    logger.info("Toggling delete service cleanup: {} to {}", service.getName(), service.getCleanupEnabled());
                    return ResponseEntity.ok(deleteServiceRepository.save(service));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
