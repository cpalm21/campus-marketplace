package edu.udel.campusmarketplace.auth;

/**
 * Credentials posted to the login endpoint.
 *
 * Field names must match the JSON keys the frontend sends.
 */
public record LoginRequest(String email, String password) {
}
