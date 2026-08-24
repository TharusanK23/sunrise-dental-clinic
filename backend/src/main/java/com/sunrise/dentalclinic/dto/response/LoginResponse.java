package com.sunrise.dentalclinic.dto.response;

public record LoginResponse(UserResponse user, long expiresInSeconds) {
}
