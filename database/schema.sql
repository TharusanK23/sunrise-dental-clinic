-- =============================================================================
-- Sunrise Dental Clinic - Online Appointment & Patient Management System
-- CIS6003 Advanced Programming coursework
--
-- Full database schema: tables, constraints, triggers, a stored procedure, a
-- function and reporting views, plus seed data so the system is immediately
-- usable after import.
--
-- HOW TO RUN (see docs/SETUP.md for full details):
--   Option A (phpMyAdmin):  Import this file against a new/blank server.
--   Option B (command line, from the XAMPP mysql/bin folder):
--       mysql -u root < schema.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sunrise_dental_clinic_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sunrise_dental_clinic_db;

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- Drop existing objects so this script can be re-run idempotently.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_prevent_double_booking;
DROP TRIGGER IF EXISTS trg_sync_dentist_status;
DROP PROCEDURE IF EXISTS sp_daily_revenue_report;
DROP FUNCTION IF EXISTS fn_is_weekend_appointment;
DROP VIEW IF EXISTS vw_dentist_utilization;
DROP VIEW IF EXISTS vw_appointment_summary;

DROP TABLE IF EXISTS notification_logs;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS dentists;
DROP TABLE IF EXISTS treatment_types;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- Tables
-- -----------------------------------------------------------------------------

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'STAFF'))
) ENGINE = InnoDB;

CREATE TABLE patients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    address         VARCHAR(200) NOT NULL,
    contact_number  VARCHAR(20)  NOT NULL,
    email           VARCHAR(100),
    date_of_birth   DATE,
    created_at      DATETIME     NOT NULL
) ENGINE = InnoDB;

CREATE TABLE treatment_types (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    treatment_name    VARCHAR(50)    NOT NULL UNIQUE,
    consultation_fee  DECIMAL(10, 2) NOT NULL,
    description       VARCHAR(255),
    CONSTRAINT chk_treatment_types_fee CHECK (consultation_fee > 0)
) ENGINE = InnoDB;

CREATE TABLE dentists (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    specialization   VARCHAR(80)  NOT NULL,
    contact_number   VARCHAR(20),
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT chk_dentists_status CHECK (status IN ('AVAILABLE', 'ON_LEAVE', 'INACTIVE'))
) ENGINE = InnoDB;

CREATE TABLE appointments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_number   VARCHAR(20)  NOT NULL UNIQUE,
    patient_id           BIGINT       NOT NULL,
    dentist_id           BIGINT       NOT NULL,
    treatment_type_id    BIGINT       NOT NULL,
    appointment_date     DATE         NOT NULL,
    appointment_time     TIME         NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    notes                VARCHAR(255),
    created_by           BIGINT       NOT NULL,
    created_at           DATETIME     NOT NULL,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_dentist FOREIGN KEY (dentist_id) REFERENCES dentists (id),
    CONSTRAINT fk_appointments_treatment FOREIGN KEY (treatment_type_id) REFERENCES treatment_types (id),
    CONSTRAINT fk_appointments_user FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB;

CREATE TABLE bills (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number       VARCHAR(20)    NOT NULL UNIQUE,
    appointment_id    BIGINT         NOT NULL UNIQUE,
    consultation_fee  DECIMAL(10, 2) NOT NULL,
    subtotal          DECIMAL(10, 2) NOT NULL,
    surcharge_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax_amount        DECIMAL(10, 2) NOT NULL,
    total_amount      DECIMAL(10, 2) NOT NULL,
    pricing_strategy  VARCHAR(40),
    payment_status    VARCHAR(20)    NOT NULL DEFAULT 'UNPAID',
    payment_method    VARCHAR(30),
    generated_at      DATETIME       NOT NULL,
    CONSTRAINT fk_bills_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT chk_bills_payment_status CHECK (payment_status IN ('UNPAID', 'PAID'))
) ENGINE = InnoDB;

CREATE TABLE notification_logs (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_number    VARCHAR(20)  NOT NULL,
    channel               VARCHAR(20)  NOT NULL,
    message               VARCHAR(500) NOT NULL,
    sent_at               DATETIME     NOT NULL
) ENGINE = InnoDB;

CREATE INDEX idx_appointments_date ON appointments (appointment_date);
CREATE INDEX idx_appointments_dentist ON appointments (dentist_id, status);
CREATE INDEX idx_notification_logs_appointment ON notification_logs (appointment_number);

-- -----------------------------------------------------------------------------
-- Function: fn_is_weekend_appointment
-- Encapsulates the "is this a Saturday/Sunday slot" business rule once so it
-- can be reused by any report or ad-hoc query, not only by the Java
-- WeekendSurchargePricingStrategy.
-- -----------------------------------------------------------------------------
DELIMITER $$
CREATE FUNCTION fn_is_weekend_appointment(p_appointment_date DATE)
    RETURNS TINYINT(1)
    DETERMINISTIC
BEGIN
    DECLARE dow INT;
    SET dow = DAYOFWEEK(p_appointment_date); -- 1=Sunday ... 7=Saturday
    RETURN dow = 1 OR dow = 7;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Trigger: trg_prevent_double_booking
-- Database-level safety net (in addition to the application-level check in
-- AppointmentServiceImpl) guaranteeing a dentist can never be double-booked
-- for the same date/time slot, even if a row were inserted by a client other
-- than the REST API.
-- -----------------------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_prevent_double_booking
    BEFORE INSERT
    ON appointments
    FOR EACH ROW
BEGIN
    DECLARE conflict_count INT;

    SELECT COUNT(*)
    INTO conflict_count
    FROM appointments a
    WHERE a.dentist_id = NEW.dentist_id
      AND a.status IN ('PENDING', 'CONFIRMED')
      AND a.appointment_date = NEW.appointment_date
      AND a.appointment_time = NEW.appointment_time;

    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Double booking rejected: this dentist already has an appointment at this date/time.';
    END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Trigger: trg_sync_dentist_status
-- A defence-in-depth example: if an appointment is cancelled while a dentist
-- had been marked ON_LEAVE purely because of a scheduling conflict tool
-- (not used by the current app logic, but demonstrates that business rules
-- can be enforced at the DB layer independent of the application). Here it
-- logs an audit row whenever an appointment status flips to COMPLETED or
-- CANCELLED, guaranteeing an audit trail exists even for direct DB writes.
-- -----------------------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_sync_dentist_status
    AFTER UPDATE
    ON appointments
    FOR EACH ROW
BEGIN
    IF NEW.status IN ('COMPLETED', 'CANCELLED') AND OLD.status <> NEW.status THEN
        INSERT INTO notification_logs (appointment_number, channel, message, sent_at)
        VALUES (NEW.appointment_number, 'AUDIT',
                CONCAT('DB TRIGGER: appointment ', NEW.appointment_number, ' status changed to ', NEW.status),
                NOW());
    END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Stored procedure: sp_daily_revenue_report
-- Used by ReportServiceImpl.dailyRevenue() to power the admin "Revenue
-- Report" screen: total billed revenue and appointment count per calendar
-- day in range.
-- -----------------------------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE sp_daily_revenue_report(IN p_start_date DATE, IN p_end_date DATE)
BEGIN
    SELECT DATE(b.generated_at)      AS report_date,
           SUM(b.total_amount)       AS total_revenue,
           COUNT(*)                  AS appointment_count
    FROM bills b
    WHERE DATE(b.generated_at) BETWEEN p_start_date AND p_end_date
    GROUP BY DATE(b.generated_at)
    ORDER BY report_date;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Views
-- -----------------------------------------------------------------------------

-- Used by ReportServiceImpl.dentistUtilization() to power the "Dentist Utilisation" report.
CREATE VIEW vw_dentist_utilization AS
SELECT d.id,
       d.full_name,
       d.specialization,
       COUNT(a.id) AS times_booked
FROM dentists d
         LEFT JOIN appointments a ON a.dentist_id = d.id AND a.status <> 'CANCELLED'
GROUP BY d.id, d.full_name, d.specialization;

-- A consolidated, denormalised read model joining every table together -
-- handy for ad-hoc reporting/export straight out of phpMyAdmin without
-- re-writing the same five-table join each time.
CREATE VIEW vw_appointment_summary AS
SELECT a.appointment_number,
       p.full_name                          AS patient_name,
       p.contact_number,
       d.full_name                          AS dentist_name,
       d.specialization,
       tt.treatment_name,
       a.appointment_date,
       a.appointment_time,
       a.status                             AS appointment_status,
       b.bill_number,
       b.total_amount,
       b.payment_status,
       u.username                           AS booked_by
FROM appointments a
         JOIN patients p ON p.id = a.patient_id
         JOIN dentists d ON d.id = a.dentist_id
         JOIN treatment_types tt ON tt.id = a.treatment_type_id
         JOIN users u ON u.id = a.created_by
         LEFT JOIN bills b ON b.appointment_id = a.id;

-- -----------------------------------------------------------------------------
-- Seed data
-- -----------------------------------------------------------------------------

-- Default staff accounts. Passwords are BCrypt-hashed (never store plain text).
--   admin   / admin123      (ADMIN  - full access incl. dentist & staff management, all reports)
--   kirisha / Kirisha@123   (STAFF  - day-to-day appointment booking & billing operations)
INSERT INTO users (username, password, full_name, email, role, enabled, created_at) VALUES
    ('admin',   '$2b$10$zwgNB3RDVPq3eEv9bFZLrOddSszZKXOckdK4aciQd30BItu86UXc6', 'System Administrator', 'admin@sunrisedentalclinic.lk',   'ADMIN', 1, NOW()),
    ('kirisha', '$2b$10$0mPGnP8.YvU6ndFbRu6ZFO6JSy13UZli4gvAxg17ZrH70Qys8sLdS', 'Kirisha N', 'kirisha@sunrisedentalclinic.lk', 'STAFF', 1, NOW());

INSERT INTO treatment_types (treatment_name, consultation_fee, description) VALUES
    ('Consultation',   2500.00, 'General dental check-up and consultation.'),
    ('Scaling & Polishing', 4500.00, 'Professional teeth cleaning to remove plaque and tartar.'),
    ('Filling',        6000.00, 'Composite or amalgam filling for a single cavity.'),
    ('Root Canal Treatment', 18000.00, 'Root canal therapy for an infected or damaged tooth.'),
    ('Tooth Extraction', 5000.00, 'Simple extraction of a single tooth.'),
    ('Teeth Whitening', 12000.00, 'Cosmetic in-clinic teeth whitening session.');

INSERT INTO dentists (full_name, specialization, contact_number, status) VALUES
    ('Dr. Priyanka Rajapaksa', 'General Dentistry',      '0771112233', 'AVAILABLE'),
    ('Dr. Nuwan Gunasekara',   'Orthodontics',            '0772223344', 'AVAILABLE'),
    ('Dr. Ishara Bandara',     'Periodontics',            '0773334455', 'AVAILABLE'),
    ('Dr. Chathura Wijesinghe','Oral & Maxillofacial Surgery', '0774445566', 'AVAILABLE'),
    ('Dr. Manisha Fernando',   'Pediatric Dentistry',     '0775556677', 'ON_LEAVE');

INSERT INTO patients (full_name, address, contact_number, email, date_of_birth, created_at) VALUES
    ('Kasun Fernando',    '12 Galle Road, Colombo 03', '0771234567', 'kasun.fernando@example.com', '1990-04-12', NOW()),
    ('Dilani Jayasuriya',  '45 Kandy Road, Kadawatha',  '0719876543', 'dilani.j@example.com',        '1985-11-02', NOW()),
    ('Ruwan Silva',        '8 Temple Lane, Negombo',    '0754567890', 'ruwan.silva@example.com',     '1978-07-23', NOW());
