package edu.udel.campusmarketplace.message;

import java.util.UUID;

/**
 * A message ready to be stored.
 *
 * The counterpart to {@link SendMessageRequest}: that is what the browser
 * claimed, this is what the server decided, mirroring the
 * {@link edu.udel.campusmarketplace.listing.CreateListingRequest} and
 * {@link edu.udel.campusmarketplace.listing.NewListing} pair.
 *
 * Every id here was established server-side. senderId comes from the verified
 * token, listingId from the URL after the listing was confirmed to exist
 * within the caller's university, and receiverId was worked out from the
 * listing's seller. A caller cannot influence any of the three.
 *
 * sentAt and isRead are absent on purpose: the database owns both. sent_at
 * defaults to CURRENT_TIMESTAMP and is_read defaults to false, so inventing
 * either in Java would only risk disagreeing with the row that gets stored.
 * This follows {@link edu.udel.campusmarketplace.listing.NewListing}, which
 * leaves created_at to the database for the same reason.
 */
record NewMessage(UUID listingId, UUID senderId, UUID receiverId, String text) {
}
