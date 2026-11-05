package edu.udel.campusmarketplace.user;

import java.util.UUID;

/**
 * A user about to be inserted.
 *
 * Separate from {@link User} because userId and createdAt do not exist yet:
 * the database generates both, through gen_random_uuid() and
 * CURRENT_TIMESTAMP. Having a type without them means a caller cannot pass a
 * UUID it invented, and cannot forget one either.
 */
public record NewUser(String firebaseUid, String email, UUID universityId) {
}
