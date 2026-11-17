package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a listing.
 *
 * Internal seller IDs are deliberately absent. The same public contract is
 * used for create, search, detail, and favorites; ownership stays server-side.
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

	public static ListingResponse of(Listing listing) {
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
