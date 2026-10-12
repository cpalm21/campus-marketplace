package edu.udel.campusmarketplace.auth;

/**
 * Placeholder login result. Echoes back the email so the caller can see the
 * request was parsed; never echo the password.
 */
public record LoginResponse(String status, String email) {
}
