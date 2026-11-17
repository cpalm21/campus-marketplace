package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A row in the listings table.
 *
 * Distinct from {@link ListingResponse}: this is everything we store, while
 * that is only what the frontend is allowed to see. Distinct from
 * {@link NewListing} because listingId, createdAt and updatedAt do not exist
 * until the insert returns them.
 *
 * title holds listing_name. The column and the sell form disagree on the
 * word, and the form's is the one the rest of the code uses.
 */
public record Listing(
		UUID listingId,
		UUID userId,
		int categoryId,
		String title,
		BigDecimal price,
		String condition,
		String description,
		String location,
		String status,
		Instant createdAt,
		Instant updatedAt) {
}
