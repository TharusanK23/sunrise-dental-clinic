-- =============================================================================
-- Sunrise Dental Clinic - PostgreSQL schema for the live Render deployment.
--
-- This is a faithful PostgreSQL port of database/schema.sql (the MySQL
-- schema used for local/XAMPP development) - same tables, constraints,
-- triggers, stored procedure and views, translated to PostgreSQL/PL-pgSQL
-- syntax. Applied automatically on first boot by Flyway (see
-- application-render.yml: spring.flyway.locations) against the empty
-- PostgreSQL database Render provisions, so no manual import step is
-- needed. See docs/RENDER_DEPLOY.md for the full deployment story.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tables
-- -----------------------------------------------------------------------------

CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'STAFF'))
);

CREATE TABLE patients (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    address         VARCHAR(200) NOT NULL,
    contact_number  VARCHAR(20)  NOT NULL,
    email           VARCHAR(100),
    date_of_birth   DATE,
    created_at      TIMESTAMP    NOT NULL
);

CREATE TABLE treatment_types (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    treatment_name    VARCHAR(50)    NOT NULL UNIQUE,
    consultation_fee  DECIMAL(10, 2) NOT NULL,
    description       VARCHAR(255),
    CONSTRAINT chk_treatment_types_fee CHECK (consultation_fee > 0)
);

CREATE TABLE dentists (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    specialization   VARCHAR(80)  NOT NULL,
    contact_number   VARCHAR(20),
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT chk_dentists_status CHECK (status IN ('AVAILABLE', 'ON_LEAVE', 'INACTIVE'))
);

CREATE TABLE appointments (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_number   VARCHAR(20)  NOT NULL UNIQUE,
    patient_id           BIGINT       NOT NULL,
    dentist_id           BIGINT       NOT NULL,
    treatment_type_id    BIGINT       NOT NULL,
    appointment_date     DATE         NOT NULL,
    appointment_time     TIME         NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    notes                VARCHAR(255),
    created_by           BIGINT       NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_dentist FOREIGN KEY (dentist_id) REFERENCES dentists (id),
    CONSTRAINT fk_appointments_treatment FOREIGN KEY (treatment_type_id) REFERENCES treatment_types (id),
    CONSTRAINT fk_appointments_user FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE bills (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
    generated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT fk_bills_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT chk_bills_payment_status CHECK (payment_status IN ('UNPAID', 'PAID'))
);

CREATE TABLE notification_logs (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_number    VARCHAR(20)  NOT NULL,
    channel               VARCHAR(20)  NOT NULL,
    message               VARCHAR(500) NOT NULL,
    sent_at               TIMESTAMP    NOT NULL
);

CREATE INDEX idx_appointments_date ON appointments (appointment_date);
CREATE INDEX idx_appointments_dentist ON appointments (dentist_id, status);
CREATE INDEX idx_notification_logs_appointment ON notification_logs (appointment_number);

-- -----------------------------------------------------------------------------
-- Function: fn_is_weekend_appointment
-- PostgreSQL port of the MySQL function of the same name in schema.sql.
-- Encapsulates the "is this a Saturday/Sunday slot" rule once, independent
-- of the Java WeekendSurchargePricingStrategy's own copy of the same logic.
-- -----------------------------------------------------------------------------
CREATE FUNCTION fn_is_weekend_appointment(p_appointment_date DATE)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
    IMMUTABLE
AS $$
DECLARE
    dow INT;
BEGIN
    dow := EXTRACT(DOW FROM p_appointment_date); -- 0 = Sunday ... 6 = Saturday
    RETURN dow = 0 OR dow = 6;
END;
$$;

-- -----------------------------------------------------------------------------
-- Trigger: trg_prevent_double_booking
-- PostgreSQL port of the MySQL trigger of the same name - a database-level
-- safety net (in addition to the application-level check in
-- AppointmentServiceImpl) guaranteeing a dentist can never be double-booked
-- for the same date/time slot.
-- -----------------------------------------------------------------------------
CREATE FUNCTION trg_fn_prevent_double_booking() RETURNS TRIGGER AS $$
DECLARE
    conflict_count INT;
BEGIN
    SELECT COUNT(*)
    INTO conflict_count
    FROM appointments a
    WHERE a.dentist_id = NEW.dentist_id
      AND a.status IN ('PENDING', 'CONFIRMED')
      AND a.appointment_date = NEW.appointment_date
      AND a.appointment_time = NEW.appointment_time;

    IF conflict_count > 0 THEN
        RAISE EXCEPTION 'Double booking rejected: this dentist already has an appointment at this date/time.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_double_booking
    BEFORE INSERT ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_prevent_double_booking();

-- -----------------------------------------------------------------------------
-- Trigger: trg_sync_dentist_status
-- PostgreSQL port of the MySQL trigger of the same name - logs an audit row
-- whenever an appointment status flips to COMPLETED or CANCELLED, so an
-- audit trail exists even for a direct database write.
-- -----------------------------------------------------------------------------
CREATE FUNCTION trg_fn_sync_dentist_status() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('COMPLETED', 'CANCELLED') AND OLD.status <> NEW.status THEN
        INSERT INTO notification_logs (appointment_number, channel, message, sent_at)
        VALUES (NEW.appointment_number, 'AUDIT',
                'DB TRIGGER: appointment ' || NEW.appointment_number || ' status changed to ' || NEW.status,
                NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_dentist_status
    AFTER UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_sync_dentist_status();

-- -----------------------------------------------------------------------------
-- Function: sp_daily_revenue_report
-- PostgreSQL port of the MySQL PROCEDURE of the same name. PostgreSQL
-- procedures cannot return a result set the way a MySQL procedure can, so
-- this is a set-returning FUNCTION instead - called from Java as
-- "SELECT * FROM sp_daily_revenue_report(?, ?)" rather than MySQL's
-- "CALL sp_daily_revenue_report(?, ?)" (see ReportServiceImpl.dailyRevenue(),
-- branched on app.database.type). Column names match exactly, so the same
-- RowMapper works unchanged either way.
-- -----------------------------------------------------------------------------
CREATE FUNCTION sp_daily_revenue_report(p_start_date DATE, p_end_date DATE)
    RETURNS TABLE (
        report_date       DATE,
        total_revenue     DECIMAL(10, 2),
        appointment_count BIGINT
    )
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT DATE(b.generated_at)      AS report_date,
           SUM(b.total_amount)       AS total_revenue,
           COUNT(*)                  AS appointment_count
    FROM bills b
    WHERE DATE(b.generated_at) BETWEEN p_start_date AND p_end_date
    GROUP BY DATE(b.generated_at)
    ORDER BY DATE(b.generated_at);
END;
$$;

-- -----------------------------------------------------------------------------
-- Views
-- -----------------------------------------------------------------------------

CREATE VIEW vw_dentist_utilization AS
SELECT d.id,
       d.full_name,
       d.specialization,
       COUNT(a.id) AS times_booked
FROM dentists d
         LEFT JOIN appointments a ON a.dentist_id = d.id AND a.status <> 'CANCELLED'
GROUP BY d.id, d.full_name, d.specialization;

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
-- Seed data (identical to database/schema.sql - same BCrypt hashes, same
-- demo accounts and reference data, so the live deployment behaves exactly
-- like the local one).
-- -----------------------------------------------------------------------------

INSERT INTO users (username, password, full_name, email, role, enabled, created_at) VALUES
    ('admin',   '$2b$10$zwgNB3RDVPq3eEv9bFZLrOddSszZKXOckdK4aciQd30BItu86UXc6', 'System Administrator', 'admin@sunrisedentalclinic.lk',   'ADMIN', TRUE, NOW()),
    ('kirisha', '$2b$10$0mPGnP8.YvU6ndFbRu6ZFO6JSy13UZli4gvAxg17ZrH70Qys8sLdS', 'Kirisha N', 'kirisha@sunrisedentalclinic.lk', 'STAFF', TRUE, NOW());

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
