-- ============================================================================
-- Fix: "Data truncated for column 'status'" when saving TRIAL / TRIAL_READ_ONLY /
-- EXPIRED_READ_ONLY / DELETED etc.
--
-- ROOT CAUSE: when Hibernate first created the `library` and `subscription` tables,
-- it mapped the Status enum to a MySQL ENUM(...) column containing only the values
-- that existed back then (PENDING, ACTIVE, INACTIVE, GRACE, EXPIRED, EXCEEDED).
-- spring.jpa.hibernate.ddl-auto=update does NOT widen an existing ENUM's allowed
-- values, so inserting a new value like 'TRIAL' fails with SQL error 1265.
--
-- FIX: convert both columns to VARCHAR, which has no fixed value list. The Java
-- model now also declares columnDefinition = "varchar(40)" so this stays correct
-- going forward even if ddl-auto recreates anything.
--
-- HOW TO RUN: connect to your MySQL database for this app and run this file once,
-- BEFORE starting the backend with the new code. Safe to run multiple times.
-- ============================================================================

ALTER TABLE library
  MODIFY COLUMN status VARCHAR(40) NULL;

ALTER TABLE subscription
  MODIFY COLUMN status VARCHAR(40) NULL;

-- Optional sanity check — should show varchar(40) for both, not enum(...)
-- SHOW COLUMNS FROM library LIKE 'status';
-- SHOW COLUMNS FROM subscription LIKE 'status';
