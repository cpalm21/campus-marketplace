package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a listing.
 *
 * userId is deliberately absent. The caller of POST /api/listings is the owner
 * by construction, so echoing their internal id back tells them nothing they
 * did not already know and starts leaking a key the frontend has no use for.
 * A future GET that shows somebody else's listing needs a seller name, not a
 * user id, so it wants its own type rather than this one widened.
 *
 * categoryId rather than a category name: the frontend already holds the
 * id-to-name pairs from GET /api/categories, so sending the name as well would
 * be a second copy of the same fact.
 */
public record ListingResponse(
		UUID listingId,
		int categoryId,
		String title,
		BigDecimal price,
		String condition,
		String description,
		String location,
		String status,
		Instant createdAt) {

	static ListingResponse of(Listing listing) {
		return new ListingResponse(
				listing.listingId(),
				listing.categoryId(),
				listing.title(),
				listing.price(),
				listing.condition(),
				listing.description(),
				listing.location(),
				listing.status(),
				listing.createdAt());
	}
}
