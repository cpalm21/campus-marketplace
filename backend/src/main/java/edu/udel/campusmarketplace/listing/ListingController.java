package edu.udel.campusmarketplace.listing;

import java.net.URI;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udel.campusmarketplace.auth.TokenVerifier;

/**
 * Listing endpoints.
 *
 * Mapped at /api/listings rather than /api/newListing: POST already means
 * create, which leaves GET /api/listings and GET /api/listings/{id} free to
 * live on the same path later without inventing a verb for each one.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingController {

	private final ListingService listingService;
	private final TokenVerifier tokenVerifier;

	ListingController(ListingService listingService, TokenVerifier tokenVerifier) {
		this.listingService = listingService;
		this.tokenVerifier = tokenVerifier;
	}

	/**
	 * Creates a listing owned by the caller.
	 *
	 * The identity is established before the body is looked at, so an
	 * unauthenticated caller gets 401 and learns nothing about which fields
	 * this endpoint would have complained about. Ownership comes from the
	 * verified token and never from the body, which is why
	 * {@link CreateListingRequest} has no userId field to send.
	 *
	 * Responds 201 with the stored listing and a Location header pointing at
	 * it. The GET that header promises does not exist yet, which is the next
	 * thing this controller needs.
	 *
	 * The header is optional to Spring only so that a missing one fails as 401
	 * from the verifier rather than as a generic 400, matching
	 * {@link edu.udel.campusmarketplace.user.UserController}.
	 */
	@PostMapping
	public ResponseEntity<ListingResponse> create(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody CreateListingRequest request) {

		ListingResponse listing = listingService.create(tokenVerifier.verify(authorization), request);

		return ResponseEntity
				.created(URI.create("/api/listings/" + listing.listingId()))
				.body(listing);
	}

	/**
	 * An unreachable database is not the caller's fault, so it is 503 rather
	 * than 500, matching
	 * {@link edu.udel.campusmarketplace.reference.ReferenceDataController}.
	 * The message says nothing about the database itself.
	 */
	@ExceptionHandler(DataAccessException.class)
	ResponseEntity<Map<String, String>> databaseUnavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("message", "Listings are temporarily unavailable."));
	}

}
