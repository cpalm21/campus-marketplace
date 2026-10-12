package edu.udel.campusmarketplace.user;

import java.util.UUID;

/**
 * Public view of a user. Mirrors the columns the frontend is allowed to see,
 * so it must never grow a credentials field.
 */
public record UserResponse(
		UUID userId,
		String email,
		String firstName,
		String lastName) {
}
