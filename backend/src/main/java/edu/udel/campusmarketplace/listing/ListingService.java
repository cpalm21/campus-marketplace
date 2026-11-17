package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.user.CurrentUserService;
import edu.udel.campusmarketplace.user.User;

/**
 * Listing rules.
 *
 * Takes a {@link VerifiedUser} rather than a Firebase UID string so that an
 * unverified identity cannot reach this method by mistake, following
 * {@link edu.udel.campusmarketplace.user.UserService}.
 *
 * The browser checks most of the field rules too, in ListingFormPage. That is
 * for fast feedback only. Anyone can POST to this endpoint directly, so the
 * rule that matters is the one on this side.
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
	 * Categories are the opposite case, which is why they are looked up.
	 *
	 * Note that the sell form currently offers "Like new", which is not one of
	 * these and will be rejected. The form's list also omits Great and Poor.
	 * Fixing that is frontend work; these five are what the database accepts.
	 */
	private static final Set<String> CONDITIONS = Set.of("New", "Great", "Good", "Fair", "Poor");

	/** What a listing starts as. The column is NOT NULL with no default. */
	private static final String INITIAL_STATUS = "Available";

	private final ListingRepository listingRepository;
    private final CurrentUserService users;

	ListingService(ListingRepository listingRepository, CurrentUserService users) {
		this.listingRepository = listingRepository;
        this.users = users;
	}

	/**
	 * Stores a listing owned by the caller.
	 *
	 * The order of the checks is deliberate. Who the caller is settles first,
	 * so an unverified student is told that rather than being handed a list of
	 * complaints about their fields. Field rules come next, since they need no
	 * database and a malformed request should cost no queries. Then the
	 * profile lookup, whose id is needed either way, and last the category,
	 * which is the only check a constraint would otherwise catch.
	 *
	 * @return the stored listing, as the frontend is allowed to see it
	 * @throws ResponseStatusException 400 for a field the table could not
	 *     hold or a category that does not exist, 403 when the caller's email
	 *     is unverified or they have no profile row
	 */
	@Transactional
	public ListingResponse create(VerifiedUser verifiedUser, CreateListingRequest request) {
		// Selling to other students is the point, so a listing must trace back
		// to a mailbox someone has proved they own. logIn in authService
		// blocks an unverified user too, but that check only protects the
		// browser: this endpoint can be called directly.
		//
		// The claim is read from the ID token rather than from Firebase, so it
		// is as old as the token, up to an hour. Someone who verifies mid
		// session stays blocked until their token refreshes; signing out and
		// back in clears it immediately, which is what the message says.
		// Asking FirebaseAuth for the live value would cost a network call on
		// every listing created.
		if (!verifiedUser.emailVerified()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Please verify your UD email before listing an item. "
							+ "If you just verified it, sign out and sign in again.");
		}

		validate(request);

		// A Firebase account with no users row. The frontend retries
		// registration on every sign-in, so this means both that call and the
		// one at signup failed; signing out and back in runs the retry again.
		// Not 404: the caller is authenticated, they simply have no profile
		// here, which is what 403 says.
		UUID userId = users.require(verifiedUser, false).userId();

		if (!listingRepository.categoryExists(request.categoryId())) {
			throw badRequest("That category does not exist.");
		}

		return ListingResponse.of(listingRepository.insert(toNewListing(userId, request)));
	}

	/**
	 * Rejects a request the listings table could not hold.
	 *
	 * Every limit below is a column definition, so a request that fails one of
	 * them could never have been stored. Rejecting it here means a plain 400
	 * that names the field, rather than a Postgres error to decipher.
	 *
	 * Returns normally when the request is acceptable; the only signal is the
	 * exception, so a caller cannot accidentally ignore the result.
	 *
	 * @throws ResponseStatusException 400, naming the first field that failed
	 */
	private void validate(CreateListingRequest request) {
		if (request == null) {
			throw badRequest("A listing body is required.");
		}

		String title = trimmed(request.title());
		if (title.isEmpty()) {
			throw badRequest("A title is required.");
		}
		if (title.codePointCount(0, title.length()) > TITLE_MAX) {
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
		if (trimmed(request.description()).codePointCount(0, trimmed(request.description()).length()) > DESCRIPTION_MAX) {
			throw badRequest("A description cannot be longer than " + DESCRIPTION_MAX + " characters.");
		}
		if (trimmed(request.location()).codePointCount(0, trimmed(request.location()).length()) > LOCATION_MAX) {
			throw badRequest("A location cannot be longer than " + LOCATION_MAX + " characters.");
		}
	}

	/**
	 * Builds the row to insert from a request that has already been validated.
	 *
	 * Stores trimmed text, so a title padded with spaces cannot sneak past the
	 * 50-character limit and cannot sort oddly later. A description or
	 * location that is blank once trimmed becomes null rather than an empty
	 * string, since the columns are nullable and "absent" should have one
	 * representation rather than two.
	 */
	private static NewListing toNewListing(UUID userId, CreateListingRequest request) {
		return new NewListing(
				userId,
				request.categoryId(),
				trimmed(request.title()),
				request.price(),
				trimmed(request.condition()),
				nullIfBlank(request.description()),
				nullIfBlank(request.location()),
				INITIAL_STATUS);
	}

	/** A null string counts as absent, which reads the same as an empty one. */
	private static String trimmed(String value) {
		return value == null ? "" : value.strip();
	}

	private static String nullIfBlank(String value) {
		String trimmed = trimmed(value);
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

    public ListingPage browse(VerifiedUser identity, ListingFilters filters, int limit, String cursor, boolean mine) {
        User user = users.require(identity, false);
        if (limit < 1 || limit > 100) throw badRequest("Limit must be between 1 and 100.");
        UUID ownerId = mine ? user.userId() : null;
        String scope = ListingCursor.scope(user.universityId(), ownerId, filters);
        List<Listing> rows = listingRepository.browse(user.universityId(), ownerId, filters, limit + 1,
                ListingCursor.parse(cursor, scope));
        boolean more = rows.size() > limit;
        List<ListingResponse> items = rows.stream().limit(limit).map(ListingResponse::of).toList();
        return new ListingPage(items, more ? ListingCursor.encode(rows.get(limit - 1), scope) : null);
    }

    public ListingResponse get(VerifiedUser identity, UUID listingId) {
        User user = users.require(identity, false);
        return ListingResponse.of(find(listingId, user, false));
    }

    @Transactional
    public ListingResponse update(VerifiedUser identity, UUID listingId, CreateListingRequest request) {
        User user = users.require(identity, true);
        requireAvailable(owned(listingId, user));
        validate(request);
        if (!listingRepository.categoryExists(request.categoryId())) throw badRequest("That category does not exist.");
        return ListingResponse.of(listingRepository.update(listingId, toNewListing(user.userId(), request)));
    }

    @Transactional
    public ListingResponse changeStatus(VerifiedUser identity, UUID listingId, String status) {
        User user = users.require(identity, true);
        Listing listing = owned(listingId, user);
        if (status == null || !Set.of("Available", "Reserved", "Sold").contains(status)) throw badRequest("Invalid listing status.");
        if (listing.status().equals(status)) return ListingResponse.of(listing);
        if (listing.status().equals("Sold")) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sold listings cannot be reopened.");
        return ListingResponse.of(listingRepository.changeStatus(listingId, status));
    }

    @Transactional
    public void delete(VerifiedUser identity, UUID listingId) {
        User user = users.require(identity, true);
        requireAvailable(owned(listingId, user));
        listingRepository.delete(listingId);
    }

    private Listing find(UUID listingId, User user, boolean lock) {
        return listingRepository.find(listingId, user.universityId(), lock).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found."));
    }

    private Listing owned(UUID listingId, User user) {
        Listing listing = find(listingId, user, true);
        if (!listing.userId().equals(user.userId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the seller can change this listing.");
        return listing;
    }

    static void requireAvailable(Listing listing) {
        if (!listing.status().equals("Available")) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only available listings can be edited or deleted.");
    }
}
