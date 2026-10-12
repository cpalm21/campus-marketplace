package edu.udel.campusmarketplace.user;

/**
 * Profile details posted when a user finishes signing up.
 *
 * Email and Firebase UID are deliberately absent: once token verification is
 * wired up they come from the verified ID token, not from the request body,
 * so a caller cannot claim an identity that is not theirs.
 */
public record RegisterRequest(String firstName, String lastName) {
}
