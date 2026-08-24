package com.sunrise.dentalclinic.dto.response;

/**
 * Returned on successful login. The signed JWT is delivered two ways: as an
 * HttpOnly cookie (what the browser client actually relies on - safe from
 * XSS since JavaScript cannot read it) and, here, as {@code token} in the
 * response body, so a non-browser API client (Postman, curl, Swagger's
 * "Authorize" button) can carry it explicitly as
 * {@code Authorization: Bearer <token>} - {@link com.sunrise.dentalclinic.security.JwtAuthenticationFilter}
 * accepts either.
 */
public record LoginResponse(UserResponse user, String token, long expiresInSeconds) {
}
