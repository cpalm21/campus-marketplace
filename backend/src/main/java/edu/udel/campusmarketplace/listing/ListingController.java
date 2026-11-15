package edu.udel.campusmarketplace.listing;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	 * this endpoint would have complained about. The verified identity is not
	 * needed yet; what matters is that verify() throws when the token does not
	 * hold up.
	 *
	 * Nothing is stored yet, so the success case is 204 with an empty body:
	 * the request was accepted as well-formed and there is nothing to report.
	 * Returning 201 with a resource would be a lie the frontend could build
	 * on.
	 *
	 * The header is optional to Spring only so that a missing one fails as 401
	 * from the verifier rather than as a generic 400, matching
	 * {@link edu.udel.campusmarketplace.user.UserController}.
	 *
	 * TODO Resolve the user id, insert, and return 201 with the stored row and
	 * a Location header once ListingRepository exists.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void create(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody CreateListingRequest request) {

		tokenVerifier.verify(authorization);

		listingService.validate(request);
	}

}
