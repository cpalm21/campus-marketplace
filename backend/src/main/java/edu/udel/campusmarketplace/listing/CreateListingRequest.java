package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;

/**
 * A create-listing request exactly as the browser sent it. Untrusted.
 *
 * Deliberately has no userId field. Identity comes from the verified Firebase
 * ID token, so a caller cannot claim to be another student by putting their id
 * in the body. {@link NewListing} is the type that carries a user id, and it
 * can only be built once the token has been verified.
 *
 * Every field is a reference type, including categoryId, so a missing value
 * arrives as null rather than as a plausible-looking 0. The service tells the
 * difference; a primitive int could not.
 *
 * categoryId is a number and not a category name because listings.category_id
 * is an INT referencing categories(category_id). The browser gets both the id
 * and the name from GET /api/categories and sends back the id, so a spelling
 * difference cannot break a submission.
 */
public record CreateListingRequest(
		String title,
		BigDecimal price,
		Integer categoryId,
		String condition,
		String description,
		String location) {
}
