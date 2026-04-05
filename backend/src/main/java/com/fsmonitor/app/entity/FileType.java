package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "file_types")
public class FileType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(unique = true)
    private String extension;

    @NotBlank
    @Size(max = 100)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public FileType() {}

    public FileType(String extension, String description) {
        this.extension = extension;
        this.description = description;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
