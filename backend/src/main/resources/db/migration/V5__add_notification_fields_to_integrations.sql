ALTER TABLE integrations ADD COLUMN notification_sent BOOLEAN DEFAULT FALSE;
ALTER TABLE integrations ADD COLUMN notification_sent_at TIMESTAMP;
