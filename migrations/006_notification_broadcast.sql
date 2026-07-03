-- Library-wide notification broadcasts (Library Admin -> whole library or a single student).
-- NOTE: with spring.jpa.hibernate.ddl-auto=update these columns are also auto-added by
-- Hibernate on boot. This file is kept for production parity / manual DBs where ddl-auto is off.

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS broadcast_group_id  VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS dashboard_expires_at DATETIME   NULL;

CREATE INDEX IF NOT EXISTS idx_notif_broadcast_group ON notifications (broadcast_group_id);
