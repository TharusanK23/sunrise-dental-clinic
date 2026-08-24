package com.sunrise.dentalclinic.dto.response;

import com.sunrise.dentalclinic.entity.DentistStatus;

public record DentistResponse(Long id, String fullName, String specialization, String contactNumber, DentistStatus status) {
}
