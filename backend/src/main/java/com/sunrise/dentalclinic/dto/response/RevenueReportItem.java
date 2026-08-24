package com.sunrise.dentalclinic.dto.response;

import java.math.BigDecimal;

public record RevenueReportItem(String periodLabel, BigDecimal totalRevenue, long appointmentCount) {
}
