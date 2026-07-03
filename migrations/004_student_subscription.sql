-- StudentSubscription: first-class subscription entity for students.
-- NOTE: with spring.jpa.hibernate.ddl-auto=update this table/columns are also auto-created by
-- Hibernate on boot. This file is kept for production parity / manual DBs where ddl-auto is off.

CREATE TABLE IF NOT EXISTS student_subscription (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id               BIGINT NOT NULL,
    plan_id                  BIGINT,
    library_id               BIGINT,
    cycle_start              DATE NOT NULL,
    cycle_end                DATE NOT NULL,
    plan_duration_days       INT,
    plan_price               DOUBLE,
    payable                  DOUBLE,
    paid                     DOUBLE,
    balance                  DOUBLE,
    carry_forward_credit     DOUBLE DEFAULT 0,
    status                   VARCHAR(30) NOT NULL,
    change_type              VARCHAR(30) NOT NULL,
    previous_subscription_id BIGINT,
    created_at               DATETIME,
    created_by               VARCHAR(255),
    INDEX idx_stsub_student        (student_id),
    INDEX idx_stsub_library        (library_id),
    INDEX idx_stsub_status         (status),
    INDEX idx_stsub_student_status (student_id, status),
    INDEX idx_stsub_cycle_end      (cycle_end),
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id)    REFERENCES plan(id)    ON DELETE SET NULL,
    FOREIGN KEY (library_id) REFERENCES library(id) ON DELETE CASCADE
);

-- Link Fee invoices to the subscription cycle they belong to (nullable for old rows)
ALTER TABLE fee ADD COLUMN IF NOT EXISTS subscription_id BIGINT NULL;
ALTER TABLE fee ADD CONSTRAINT IF NOT EXISTS fk_fee_subscription
    FOREIGN KEY (subscription_id) REFERENCES student_subscription(id) ON DELETE SET NULL;

-- Library-level toggle for auto-releasing a seat when a subscription expires (default OFF)
ALTER TABLE library ADD COLUMN IF NOT EXISTS auto_release_seat_on_expiry BOOLEAN DEFAULT FALSE;

-- Drop the dead seat_allocation.end_date column (SeatExpiryScheduler removed; expiry now lives
-- entirely on student_subscription.cycle_end)
ALTER TABLE seat_allocation DROP COLUMN IF EXISTS end_date;
