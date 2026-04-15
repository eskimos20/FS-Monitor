-- Rename files_deleted_count to files_deleted_last_scan
ALTER TABLE delete_services RENAME COLUMN files_deleted_count TO files_deleted_last_scan;
