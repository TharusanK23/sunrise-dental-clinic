package com.sunrise.dentalclinic.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalDentists,
        long availableDentists,
        long totalPatients,
        long activeAppointments,
        long todaysAppointments,
        BigDecimal totalRevenue,
        BigDecimal unpaidAmount
) {
}
