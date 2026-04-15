-- Add folders_deleted_last_scan column to delete_services table
ALTER TABLE delete_services ADD COLUMN folders_deleted_last_scan INTEGER DEFAULT 0;
