package edu.udel.campusmarketplace.user;

/**
 * Public view of a user. Mirrors the columns the frontend is allowed to see,
 * so it must never grow a credentials field.
 *
 * Both fields come from the verified ID token. The database-generated user_id
 * joins them once registration writes a row.
 */
public record UserResponse(String firebaseUid, String email) {
}
