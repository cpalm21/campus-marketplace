package edu.udel.campusmarketplace.user;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the users table.
 *
 * Distinct from {@link UserResponse}: this is everything we store, while that
 * is only what the frontend is allowed to see.
 *
 * firstName and lastName are absent because the signup form does not collect
 * them yet. Add them here when it does.
 */
public record User(
		UUID userId,
		String firebaseUid,
		String email,
		UUID universityId,
		Instant createdAt) {
}
