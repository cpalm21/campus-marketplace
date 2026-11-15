package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Listing rules.
 *
 * Only the checks that need no database live here so far: every limit below is
 * a column definition in the listings table, so a request that fails one of
 * them could never have been stored. Rejecting it here means a plain 400 that
 * names the field, rather than a Postgres error to decipher.
 *
 * The browser checks most of this too, in ListingFormPage. That is for fast
 * feedback only. Anyone can POST to this endpoint directly, so the rule that
 * matters is the one on this side.
 *
 * TODO Resolve userId from the verified Firebase UID, confirm the category
 * exists, and insert. Those need ListingRepository, which does not exist yet.
 */
@Service
public class ListingService {

	/** listings.listing_name is VARCHAR(50). */
	private static final int TITLE_MAX = 50;

	/** listings.description is VARCHAR(500). */
	private static final int DESCRIPTION_MAX = 500;

	/** listings.location is VARCHAR(100). */
	private static final int LOCATION_MAX = 100;

	/**
	 * listings.price is DECIMAL(10,2): ten digits in total, two of them after
	 * the point, so the largest storable value is 99999999.99.
	 */
	private static final BigDecimal PRICE_MAX = new BigDecimal("99999999.99");
	private static final int PRICE_DECIMALS = 2;

	/**
	 * The exact values the listings.condition CHECK constraint allows.
	 *
	 * Hardcoded rather than read from the database because a CHECK constraint
	 * is schema, not data: changing this list means a migration either way.
	 * Categories are the opposite case and are looked up.
	 *
	 * Note that the sell form currently offers "Like new", which is not one of
	 * these and will be rejected. The form's list also omits Great and Poor.
	 * Fixing that is frontend work; these five are what the database accepts.
	 */
	private static final Set<String> CONDITIONS = Set.of("New", "Great", "Good", "Fair", "Poor");

	/** What a listing starts as. The column is NOT NULL with no default. */
	static final String INITIAL_STATUS = "Available";

	/**
	 * Rejects a request the listings table could not hold.
	 *
	 * Returns normally when the request is acceptable; the only signal is the
	 * exception, so a caller cannot accidentally ignore the result.
	 *
	 * @throws ResponseStatusException 400, naming the first field that failed
	 */
	public void validate(CreateListingRequest request) {
		if (request == null) {
			throw badRequest("A listing body is required.");
		}

		String title = trimmed(request.title());
		if (title.isEmpty()) {
			throw badRequest("A title is required.");
		}
		if (title.length() > TITLE_MAX) {
			throw badRequest("A title cannot be longer than " + TITLE_MAX + " characters.");
		}

		if (request.price() == null) {
			throw badRequest("A price is required.");
		}
		if (request.price().signum() < 0) {
			throw badRequest("A price cannot be negative.");
		}
		if (request.price().compareTo(PRICE_MAX) > 0) {
			throw badRequest("A price cannot be greater than " + PRICE_MAX + ".");
		}
		// stripTrailingZeros first so that 65.00 is treated as two decimals
		// rather than as the scale the caller happened to send.
		if (request.price().stripTrailingZeros().scale() > PRICE_DECIMALS) {
			throw badRequest("A price cannot have more than " + PRICE_DECIMALS + " decimal places.");
		}

		// Null and not 0: CreateListingRequest uses Integer precisely so that
		// an absent category is distinguishable from a sent one.
		if (request.categoryId() == null) {
			throw badRequest("A category is required.");
		}

		String condition = trimmed(request.condition());
		if (condition.isEmpty()) {
			throw badRequest("A condition is required.");
		}
		if (!CONDITIONS.contains(condition)) {
			throw badRequest("A condition must be one of " + CONDITIONS + ".");
		}

		// Description and location are nullable columns, so an absent one is
		// allowed. Only the length is enforced. The form's own 20-character
		// minimum is deliberately not repeated here: it is advice about
		// writing a good listing, not something the database cares about.
		if (trimmed(request.description()).length() > DESCRIPTION_MAX) {
			throw badRequest("A description cannot be longer than " + DESCRIPTION_MAX + " characters.");
		}
		if (trimmed(request.location()).length() > LOCATION_MAX) {
			throw badRequest("A location cannot be longer than " + LOCATION_MAX + " characters.");
		}
	}

	/** A null string counts as absent, which reads the same as an empty one. */
	private static String trimmed(String value) {
		return value == null ? "" : value.strip();
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

}
