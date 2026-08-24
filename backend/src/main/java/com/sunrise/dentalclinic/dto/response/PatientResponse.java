package com.sunrise.dentalclinic.dto.response;

import java.time.LocalDate;

public record PatientResponse(Long id, String fullName, String address, String contactNumber, String email, LocalDate dateOfBirth) {
}
