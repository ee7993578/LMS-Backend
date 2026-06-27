-- Fee Management Module: run only if Hibernate ddl-auto is NOT update
-- If ddl-auto=update, Hibernate auto-creates these; this script is for manual runs.

ALTER TABLE student ADD COLUMN IF NOT EXISTS admission_number VARCHAR(100);
ALTER TABLE library  ADD COLUMN IF NOT EXISTS gst_number       VARCHAR(50);

CREATE TABLE IF NOT EXISTS fee_receipt (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_number  VARCHAR(50)    NOT NULL UNIQUE,
    fee_id          BIGINT         NOT NULL,
    student_id      BIGINT         NOT NULL,
    library_id      BIGINT         NOT NULL,
    proof_id        BIGINT,
    amount_paid     DOUBLE         NOT NULL DEFAULT 0,
    concession      DOUBLE         NOT NULL DEFAULT 0,
    late_fee        DOUBLE         NOT NULL DEFAULT 0,
    balance_after   DOUBLE         NOT NULL DEFAULT 0,
    payment_mode    VARCHAR(100),
    transaction_ref VARCHAR(255),
    payment_date    DATE,
    generated_at    DATETIME,
    CONSTRAINT fk_receipt_fee      FOREIGN KEY (fee_id)      REFERENCES fee(fee_id)         ON DELETE CASCADE,
    CONSTRAINT fk_receipt_student  FOREIGN KEY (student_id)  REFERENCES student(id)         ON DELETE CASCADE,
    CONSTRAINT fk_receipt_library  FOREIGN KEY (library_id)  REFERENCES library(id)         ON DELETE CASCADE,
    CONSTRAINT fk_receipt_proof    FOREIGN KEY (proof_id)    REFERENCES payment_proof(id)   ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS fee_audit_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    fee_id       BIGINT,
    student_id   BIGINT,
    library_id   BIGINT         NOT NULL,
    performed_by VARCHAR(200),
    action       VARCHAR(100),
    details      TEXT,
    performed_at DATETIME,
    CONSTRAINT fk_audit_fee      FOREIGN KEY (fee_id)     REFERENCES fee(fee_id)     ON DELETE SET NULL,
    CONSTRAINT fk_audit_student  FOREIGN KEY (student_id) REFERENCES student(id)     ON DELETE SET NULL,
    CONSTRAINT fk_audit_library  FOREIGN KEY (library_id) REFERENCES library(id)     ON DELETE CASCADE
);
