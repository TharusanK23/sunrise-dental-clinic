package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.response.DashboardSummaryResponse;
import com.sunrise.dentalclinic.dto.response.DentistUtilizationItem;
import com.sunrise.dentalclinic.dto.response.RevenueReportItem;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    DashboardSummaryResponse dashboardSummary();
    List<RevenueReportItem> dailyRevenue(LocalDate from, LocalDate to);
    List<DentistUtilizationItem> dentistUtilization();
}
