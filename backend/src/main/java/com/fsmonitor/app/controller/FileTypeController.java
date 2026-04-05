package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.service.FileTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file-types")
public class FileTypeController {

    @Autowired
    private FileTypeService fileTypeService;

    @GetMapping
    public ResponseEntity<List<FileType>> getAllFileTypes() {
        List<FileType> fileTypes = fileTypeService.getAllFileTypes();
        return ResponseEntity.ok(fileTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileType> getFileTypeById(@PathVariable Long id) {
        return fileTypeService.getFileTypeById(id)
                .map(fileType -> ResponseEntity.ok().body(fileType))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createFileType(@Valid @RequestBody FileType fileType) {
        try {
            FileType createdFileType = fileTypeService.createFileType(fileType);
            return ResponseEntity.ok(createdFileType);
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FileType> updateFileType(@PathVariable Long id, @Valid @RequestBody FileType fileTypeDetails) {
        FileType updatedFileType = fileTypeService.updateFileType(id, fileTypeDetails);
        return ResponseEntity.ok(updatedFileType);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFileType(@PathVariable Long id) {
        fileTypeService.deleteFileType(id);
        return ResponseEntity.noContent().build();
    }
}
