package com.sunrise.dentalclinic.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTreatmentTypeRequest(
        @NotBlank(message = "Treatment name is required") String treatmentName,
        @NotNull(message = "Consultation fee is required")
        @DecimalMin(value = "0.01", message = "Consultation fee must be greater than zero")
        BigDecimal consultationFee,
        String description
) {
}
