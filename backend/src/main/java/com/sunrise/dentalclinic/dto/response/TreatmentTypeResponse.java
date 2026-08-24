package com.sunrise.dentalclinic.dto.response;

import java.math.BigDecimal;

public record TreatmentTypeResponse(Long id, String treatmentName, BigDecimal consultationFee, String description) {
}
