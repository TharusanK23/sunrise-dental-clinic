package com.sunrise.dentalclinic.dto.response;

import com.sunrise.dentalclinic.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillResponse(
        Long id,
        String billNumber,
        AppointmentResponse appointment,
        BigDecimal consultationFee,
        BigDecimal subtotal,
        BigDecimal surchargeAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String pricingStrategy,
        PaymentStatus paymentStatus,
        String paymentMethod,
        LocalDateTime generatedAt
) {
}
