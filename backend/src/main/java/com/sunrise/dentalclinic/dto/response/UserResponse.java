package com.sunrise.dentalclinic.dto.response;

import com.sunrise.dentalclinic.entity.Role;

public record UserResponse(Long id, String username, String fullName, String email, Role role) {
}
