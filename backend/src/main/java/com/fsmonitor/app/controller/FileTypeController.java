package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.FileTypeResponse;
import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.service.FileTypeService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/file-types")
public class FileTypeController {

    private final FileTypeService fileTypeService;

    public FileTypeController(FileTypeService fileTypeService) {
        this.fileTypeService = fileTypeService;
    }

    @GetMapping
    public ResponseEntity<List<FileTypeResponse>> getAllFileTypes() {
        return ResponseEntity.ok(fileTypeService.getAllFileTypes().stream()
                .map(FileTypeResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileTypeResponse> getFileTypeById(@PathVariable Long id) {
        return fileTypeService.getFileTypeById(id)
                .map(fileType -> ResponseEntity.ok(FileTypeResponse.from(fileType)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createFileType(@Valid @RequestBody FileType fileType) {
        try {
            return ResponseEntity.ok(FileTypeResponse.from(fileTypeService.createFileType(fileType)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FileTypeResponse> updateFileType(@PathVariable Long id, @Valid @RequestBody FileType fileTypeDetails) {
        return ResponseEntity.ok(FileTypeResponse.from(fileTypeService.updateFileType(id, fileTypeDetails)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFileType(@PathVariable Long id) {
        fileTypeService.deleteFileType(id);
        return ResponseEntity.noContent().build();
    }
}
