CREATE TABLE delete_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    file_types VARCHAR(500),
    cleanup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cleanup_interval_value INT NOT NULL DEFAULT 1,
    cleanup_interval_unit VARCHAR(20) NOT NULL DEFAULT 'HOURS',
    delete_age_value INT NOT NULL DEFAULT 30,
    delete_age_unit VARCHAR(20) NOT NULL DEFAULT 'DAYS',
    recursive BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_cleanup TIMESTAMP,
    files_deleted_count BIGINT DEFAULT 0
);
