package edu.udel.campusmarketplace.message;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the messages table.
 *
 * Distinct from {@link MessageResponse}: this is everything we store, while
 * that is only what the frontend is allowed to see. Same split as
 * {@link edu.udel.campusmarketplace.listing.Listing} and
 * {@link edu.udel.campusmarketplace.listing.ListingResponse}.
 *
 * The field is named text while the column is messages.message, because
 * message.message() reads as nonsense and the frontend's own ChatMessage type
 * already calls it text. Renaming at this boundary is established here:
 * Listing.title maps to listings.listing_name the same way.
 *
 * Both senderId and receiverId are kept even though a conversation only ever
 * has two participants, because which of them is "me" depends on who is
 * asking. Deciding that is {@link MessageResponse}'s job, not this record's.
 */
record Message(
		UUID messageId,
		UUID listingId,
		UUID senderId,
		UUID receiverId,
		String text,
		Instant sentAt,
		boolean isRead) {
}
