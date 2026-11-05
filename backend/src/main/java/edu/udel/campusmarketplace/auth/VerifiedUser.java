package edu.udel.campusmarketplace.auth;

/**
 * Identity read out of a verified Firebase ID token.
 *
 * Everything here was signed by Firebase and checked before this record was
 * built, so it is safe to trust. That is the whole point of the type: a method
 * that takes a {@code VerifiedUser} cannot be handed an identity a caller
 * simply claimed in a request body.
 */
public record VerifiedUser(String firebaseUid, String email, boolean emailVerified) {
}
