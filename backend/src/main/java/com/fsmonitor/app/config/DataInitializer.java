package com.fsmonitor.app.config;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.entity.Role;
import com.fsmonitor.app.entity.RoleName;
import com.fsmonitor.app.entity.User;
import com.fsmonitor.app.repository.FileTypeRepository;
import com.fsmonitor.app.repository.RoleRepository;
import com.fsmonitor.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileTypeRepository fileTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           FileTypeRepository fileTypeRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${fsmonitor.admin-password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.fileTypeRepository = fileTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeDefaultAdmin();
        initializeFileTypes();
    }

    private void initializeRoles() {
        if (!roleRepository.existsByName(RoleName.ROLE_ADMIN)) {
            roleRepository.save(new Role(RoleName.ROLE_ADMIN));
        }

        if (!roleRepository.existsByName(RoleName.ROLE_USER)) {
            roleRepository.save(new Role(RoleName.ROLE_USER));
        }
    }

    private void initializeDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@fsmonitor.local");

            boolean usingDefaultPassword = adminPassword == null || adminPassword.isBlank();
            admin.setPassword(passwordEncoder.encode(usingDefaultPassword ? "password" : adminPassword));
            admin.setPasswordChanged(false);

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Admin role not found"));

            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);

            if (usingDefaultPassword) {
                logger.warn("Created default admin account (admin / password). " +
                        "The password must be changed at first login. " +
                        "Set FS_MONITOR_ADMIN_PASSWORD to choose the initial password.");
            } else {
                logger.info("Created admin account with password from FS_MONITOR_ADMIN_PASSWORD");
            }
        }
    }

    private void initializeFileTypes() {
        if (fileTypeRepository.count() == 0) {
            fileTypeRepository.saveAll(List.of(
                new FileType(".pdf", "PDF Document"),
                new FileType(".txt", "Text File"),
                new FileType(".pad", "PAD File"),
                new FileType(".doc", "Word Document"),
                new FileType(".docx", "Word Document"),
                new FileType(".xls", "Excel Spreadsheet"),
                new FileType(".xlsx", "Excel Spreadsheet"),
                new FileType(".csv", "CSV File"),
                new FileType(".xml", "XML File"),
                new FileType(".json", "JSON File")
            ));
        }
    }
}
