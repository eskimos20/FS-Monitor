package com.fsmonitor.app.config;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.entity.Role;
import com.fsmonitor.app.entity.RoleName;
import com.fsmonitor.app.entity.User;
import com.fsmonitor.app.repository.FileTypeRepository;
import com.fsmonitor.app.repository.RoleRepository;
import com.fsmonitor.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FileTypeRepository fileTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeDefaultAdmin();
        initializeFileTypes();
    }

    private void initializeRoles() {
        if (!roleRepository.existsByName(RoleName.ROLE_ADMIN)) {
            Role adminRole = new Role(RoleName.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }

        if (!roleRepository.existsByName(RoleName.ROLE_USER)) {
            Role userRole = new Role(RoleName.ROLE_USER);
            roleRepository.save(userRole);
        }
    }

    private void initializeDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@fsmonitor.com");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setPasswordChanged(false);

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
            userRepository.save(admin);
        }
    }

    private void initializeFileTypes() {
        if (fileTypeRepository.count() == 0) {
            FileType pdf = new FileType(".pdf", "PDF Document");
            FileType txt = new FileType(".txt", "Text File");
            FileType pad = new FileType(".pad", "PAD File");
            FileType doc = new FileType(".doc", "Word Document");
            FileType docx = new FileType(".docx", "Word Document");
            FileType xls = new FileType(".xls", "Excel Spreadsheet");
            FileType xlsx = new FileType(".xlsx", "Excel Spreadsheet");
            FileType csv = new FileType(".csv", "CSV File");
            FileType xml = new FileType(".xml", "XML File");
            FileType json = new FileType(".json", "JSON File");

            fileTypeRepository.saveAll(Arrays.asList(pdf, txt, pad, doc, docx, xls, xlsx, csv, xml, json));
        }
    }
}
