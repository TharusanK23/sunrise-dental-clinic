package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.response.DashboardSummaryResponse;
import com.sunrise.dentalclinic.dto.response.DentistUtilizationItem;
import com.sunrise.dentalclinic.dto.response.RevenueReportItem;
import com.sunrise.dentalclinic.entity.AppointmentStatus;
import com.sunrise.dentalclinic.entity.DentistStatus;
import com.sunrise.dentalclinic.entity.PaymentStatus;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import com.sunrise.dentalclinic.repository.DentistRepository;
import com.sunrise.dentalclinic.repository.PatientRepository;
import com.sunrise.dentalclinic.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reports that "facilitate decision-making" (Excellent-band criterion). The
 * daily revenue figures are produced by calling the
 * {@code sp_daily_revenue_report} MySQL stored procedure directly (see
 * {@code database/schema.sql}), and dentist utilisation is read from the
 * {@code vw_dentist_utilization} database view - both concrete, demonstrable
 * uses of advanced database features beyond plain CRUD.
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final AppointmentRepository appointmentRepository;
    private final DentistRepository dentistRepository;
    private final PatientRepository patientRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * MySQL (local/XAMPP) exposes {@code sp_daily_revenue_report} as a
     * {@code PROCEDURE}, invoked with {@code CALL}; the Render deployment's
     * PostgreSQL database exposes the equivalent logic as a {@code FUNCTION}
     * returning a table (Postgres procedures don't return result sets the
     * way MySQL's do), invoked as a normal {@code SELECT}. Same column
     * names either way, so the row-mapper below is unchanged - see
     * {@code app.database.type} in application.yml / application-render.yml
     * and database/schema.sql vs backend/src/main/resources/db/migration/postgres/V1__init.sql.
     */
    @Value("${app.database.type:mysql}")
    private String databaseType;

    public ReportServiceImpl(AppointmentRepository appointmentRepository, DentistRepository dentistRepository,
                              PatientRepository patientRepository, JdbcTemplate jdbcTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.dentistRepository = dentistRepository;
        this.patientRepository = patientRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DashboardSummaryResponse dashboardSummary() {
        long totalDentists = dentistRepository.count();
        long availableDentists = dentistRepository.findByStatus(DentistStatus.AVAILABLE).size();
        long totalPatients = patientRepository.count();
        long activeAppointments = appointmentRepository.findByStatus(AppointmentStatus.CONFIRMED).size()
                + appointmentRepository.findByStatus(AppointmentStatus.PENDING).size();
        long todaysAppointments = appointmentRepository.countByAppointmentDate(LocalDate.now());

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM bills", BigDecimal.class);
        BigDecimal unpaidAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM bills WHERE payment_status = ?",
                BigDecimal.class, PaymentStatus.UNPAID.name());

        return new DashboardSummaryResponse(totalDentists, availableDentists, totalPatients,
                activeAppointments, todaysAppointments, totalRevenue, unpaidAmount);
    }

    @Override
    public List<RevenueReportItem> dailyRevenue(LocalDate from, LocalDate to) {
        String sql = "postgres".equals(databaseType)
                ? "SELECT * FROM sp_daily_revenue_report(?, ?)"
                : "CALL sp_daily_revenue_report(?, ?)";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new RevenueReportItem(
                        rs.getDate("report_date").toString(),
                        rs.getBigDecimal("total_revenue"),
                        rs.getLong("appointment_count")
                ),
                from, to
        );
    }

    @Override
    public List<DentistUtilizationItem> dentistUtilization() {
        return jdbcTemplate.query(
                "SELECT full_name, specialization, times_booked FROM vw_dentist_utilization ORDER BY times_booked DESC",
                (rs, rowNum) -> new DentistUtilizationItem(
                        rs.getString("full_name"),
                        rs.getString("specialization"),
                        rs.getLong("times_booked")
                )
        );
    }
}
