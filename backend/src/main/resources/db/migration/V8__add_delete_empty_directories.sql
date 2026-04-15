-- Add delete_empty_directories column to delete_services table
ALTER TABLE delete_services ADD COLUMN delete_empty_directories BOOLEAN DEFAULT FALSE;
