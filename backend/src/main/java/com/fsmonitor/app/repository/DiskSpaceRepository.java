package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.DiskSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface DiskSpaceRepository extends JpaRepository<DiskSpace, Long> {
    Optional<DiskSpace> findByStorageConfigId(Long storageConfigId);
    
    @Modifying
    @Transactional
    void deleteByStorageConfigId(Long storageConfigId);
}
