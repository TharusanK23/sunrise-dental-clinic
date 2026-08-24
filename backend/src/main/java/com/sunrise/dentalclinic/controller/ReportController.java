package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.response.DashboardSummaryResponse;
import com.sunrise.dentalclinic.dto.response.DentistUtilizationItem;
import com.sunrise.dentalclinic.dto.response.RevenueReportItem;
import com.sunrise.dentalclinic.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Decision-support reports (dashboard KPIs, daily revenue via stored procedure, dentist utilisation via a database view). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public DashboardSummaryResponse dashboard() {
        return reportService.dashboardSummary();
    }

    @GetMapping("/revenue")
    public List<RevenueReportItem> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.dailyRevenue(from, to);
    }

    @GetMapping("/dentist-utilization")
    public List<DentistUtilizationItem> dentistUtilization() {
        return reportService.dentistUtilization();
    }
}
