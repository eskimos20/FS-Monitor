package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.repository.FileTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FileTypeService {

    @Autowired
    private FileTypeRepository fileTypeRepository;

    @Transactional
    public List<FileType> getAllFileTypes() {
        return fileTypeRepository.findByIsActiveTrueOrderByExtension();
    }

    @Transactional
    public Optional<FileType> getFileTypeById(Long id) {
        return fileTypeRepository.findById(id);
    }

    @Transactional
    public FileType createFileType(FileType fileType) {
        if (fileTypeRepository.existsByExtension(fileType.getExtension())) {
            throw new RuntimeException("File type with extension " + fileType.getExtension() + " already exists");
        }
        return fileTypeRepository.save(fileType);
    }

    @Transactional
    public FileType updateFileType(Long id, FileType fileTypeDetails) {
        return fileTypeRepository.findById(id)
                .map(fileType -> {
                    fileType.setExtension(fileTypeDetails.getExtension());
                    fileType.setDescription(fileTypeDetails.getDescription());
                    fileType.setIsActive(fileTypeDetails.getIsActive());
                    return fileTypeRepository.save(fileType);
                })
                .orElseThrow(() -> new RuntimeException("File type not found with id: " + id));
    }

    @Transactional
    public void deleteFileType(Long id) {
        FileType fileType = fileTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File type not found with id: " + id));
        fileType.setIsActive(false);
        fileTypeRepository.save(fileType);
    }
}
