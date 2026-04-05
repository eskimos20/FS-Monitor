package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileTypeRepository extends JpaRepository<FileType, Long> {
    Optional<FileType> findByExtension(String extension);
    List<FileType> findByIsActiveTrue();
    List<FileType> findByIsActiveTrueOrderByExtension();
    boolean existsByExtension(String extension);
}
