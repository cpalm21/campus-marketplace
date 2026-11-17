package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A listing about to be inserted: validated, and tied to a real user.
 *
 * The difference from {@link CreateListingRequest} is the whole point. That
 * type is what a caller sent and may be nonsense; this one can only be built
 * by {@link ListingService} after every field has been checked and after
 * userId has been resolved from a verified Firebase token. A method taking a
 * NewListing cannot be handed an unvalidated listing, or one belonging to
 * somebody else.
 *
 * No listingId or createdAt, following {@link
 * edu.udel.campusmarketplace.user.NewUser}: the database generates both,
 * through gen_random_uuid() and CURRENT_TIMESTAMP, so there is no field here
 * for Java to invent a value in.
 *
 * status is present rather than left to the column default because the column
 * has none: listings.status is NOT NULL with a CHECK constraint, so the value
 * has to come from somewhere. The service supplies 'Available'.
 */
public record NewListing(
		UUID userId,
		int categoryId,
		String title,
		BigDecimal price,
		String condition,
		String description,
		String location,
		String status) {
}
