package com.sunrise.dentalclinic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for an Admin editing an existing staff account. Deliberately
 * scoped to just full name and email - username, password and role are not
 * editable through this endpoint (see docs/ASSIGNMENT_REPORT.md for the
 * rationale).
 */
public record UpdateUserRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email
) {
}
