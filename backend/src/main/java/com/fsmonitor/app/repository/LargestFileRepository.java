package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.LargestFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LargestFileRepository extends JpaRepository<LargestFile, Long> {
    List<LargestFile> findByStorageConfigIdOrderBySizeBytesDesc(Long storageConfigId);
    
    @Modifying
    @Transactional
    void deleteByStorageConfigId(Long storageConfigId);
}
