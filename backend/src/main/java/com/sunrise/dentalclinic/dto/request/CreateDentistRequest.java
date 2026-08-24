package com.sunrise.dentalclinic.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDentistRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Specialization is required") String specialization,
        String contactNumber
) {
}
