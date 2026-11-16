package edu.udel.campusmarketplace.user;

/**
 * Public view of a user. Mirrors the columns the frontend is allowed to see,
 * so it must never grow a credentials field.
 *
 * Both fields come from the stored profile. Listing ownership is resolved
 * from the verified Firebase identity on the server.
 */
public record UserResponse(String firebaseUid, String email) {
}
