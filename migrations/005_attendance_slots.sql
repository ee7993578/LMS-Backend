-- AttendanceSlot: one row per punch-in/punch-out session within a day.
-- Fixes the "timer resets on re-punch" bug: attendance.total_study_minutes is the sum
-- of every slot's duration_minutes for that day, so punching in again just opens the
-- next slot -- the day's running total is never reset back to zero.
-- NOTE: with spring.jpa.hibernate.ddl-auto=update this table is also auto-created by
-- Hibernate on boot. This file is kept for production parity / manual DBs where ddl-auto is off.

CREATE TABLE IF NOT EXISTS attendance_slot (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_id       BIGINT NOT NULL,
    slot_number         INT NOT NULL,
    punch_in            DATETIME NOT NULL,
    punch_out           DATETIME NULL,
    duration_minutes    BIGINT NULL,
    auto_closed         BOOLEAN DEFAULT FALSE,

    INDEX idx_slot_attendance (attendance_id),
    FOREIGN KEY (attendance_id) REFERENCES attendance(id) ON DELETE CASCADE
);

-- Backfill: give every existing attendance row (created before slots existed) a single
-- Slot 1 built from its shiftStart/shiftEnd, so history keeps showing correctly.
INSERT INTO attendance_slot (attendance_id, slot_number, punch_in, punch_out, duration_minutes, auto_closed)
SELECT a.id, 1, a.shift_start, a.shift_end, a.total_study_minutes, FALSE
FROM attendance a
WHERE a.shift_start IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM attendance_slot s WHERE s.attendance_id = a.id);
