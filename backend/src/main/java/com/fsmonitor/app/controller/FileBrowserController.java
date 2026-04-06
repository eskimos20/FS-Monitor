package com.fsmonitor.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file-browser")
@CrossOrigin(origins = "*")
public class FileBrowserController {
    private static final Logger logger = LoggerFactory.getLogger(FileBrowserController.class);

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDirectory(@RequestParam(required = false) String path) {
        logger.debug("Listing directory: {}", path);
        
        if (path == null || path.trim().isEmpty() || path.equals("null") || path.equals("undefined")) {
            path = "/";
        }
        
        try {
            Path normalizedPath = Paths.get(path).normalize();
            
            if (normalizedPath.toString().contains("..")) {
                logger.warn("Invalid path attempt: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid path"));
            }
            
            File directory = normalizedPath.toFile();
            
            if (!directory.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Directory does not exist"));
            }
            
            if (!directory.isDirectory()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not a directory"));
            }
            
            if (!directory.canRead()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot read directory"));
            }
            
            File[] files = directory.listFiles();
            if (files == null) {
                return ResponseEntity.ok(Map.of(
                    "currentPath", normalizedPath.toString(),
                    "parentPath", normalizedPath.getParent() != null ? normalizedPath.getParent().toString() : null,
                    "directories", Collections.emptyList(),
                    "files", Collections.emptyList()
                ));
            }
            
            List<Map<String, Object>> directories = Arrays.stream(files)
                .filter(File::isDirectory)
                .filter(File::canRead)
                .map(this::createDirectoryInfo)
                .sorted(Comparator.comparing(m -> m.get("name").toString()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> filesList = Arrays.stream(files)
                .filter(File::isFile)
                .filter(File::canRead)
                .map(this::createFileInfo)
                .sorted(Comparator.comparing(m -> m.get("name").toString()))
                .collect(Collectors.toList());
            
            logger.debug("Found {} directories and {} files in {}", directories.size(), filesList.size(), path);
            
            return ResponseEntity.ok(Map.of(
                "currentPath", normalizedPath.toString(),
                "parentPath", normalizedPath.getParent() != null ? normalizedPath.getParent().toString() : null,
                "directories", directories,
                "files", filesList
            ));
            
        } catch (Exception e) {
            logger.error("Error listing directory: {}", path, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to list directory: " + e.getMessage()));
        }
    }
    
    @GetMapping("/roots")
    public ResponseEntity<List<Map<String, Object>>> getRootDirectories() {
        logger.debug("Getting root directories");
        
        try {
            List<Map<String, Object>> roots = new ArrayList<>();
            
            File[] rootFiles = File.listRoots();
            for (File root : rootFiles) {
                Map<String, Object> rootInfo = new HashMap<>();
                rootInfo.put("name", root.toString());
                rootInfo.put("path", root.toString());
                rootInfo.put("type", "root");
                roots.add(rootInfo);
            }
            
            if (System.getProperty("os.name").toLowerCase().contains("linux") || 
                System.getProperty("os.name").toLowerCase().contains("mac")) {
                
                String[] commonPaths = {"/home", "/opt", "/var", "/tmp", "/usr"};
                for (String path : commonPaths) {
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                        Map<String, Object> dirInfo = new HashMap<>();
                        dirInfo.put("name", path);
                        dirInfo.put("path", path);
                        dirInfo.put("type", "directory");
                        roots.add(dirInfo);
                    }
                }
            }
            
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                String[] commonPaths = {"C:\\Users", "C:\\Program Files", "D:\\"};
                for (String path : commonPaths) {
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                        Map<String, Object> dirInfo = new HashMap<>();
                        dirInfo.put("name", path);
                        dirInfo.put("path", path);
                        dirInfo.put("type", "directory");
                        roots.add(dirInfo);
                    }
                }
            }
            
            logger.debug("Returning {} root directories", roots.size());
            return ResponseEntity.ok(roots);
            
        } catch (Exception e) {
            logger.error("Error getting root directories", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
    
    private Map<String, Object> createDirectoryInfo(File dir) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", dir.getName());
        info.put("path", dir.getAbsolutePath());
        info.put("type", "directory");
        info.put("canRead", dir.canRead());
        info.put("lastModified", dir.lastModified());
        return info;
    }
    
    private Map<String, Object> createFileInfo(File file) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", file.getName());
        info.put("path", file.getAbsolutePath());
        info.put("type", "file");
        info.put("size", file.length());
        info.put("lastModified", file.lastModified());
        return info;
    }
}
