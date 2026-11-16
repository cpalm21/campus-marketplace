package edu.udel.campusmarketplace.listing;

import java.net.URI;
import java.math.BigDecimal;
import java.util.UUID;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	 * it. The detail endpoint resolves the returned Location within the caller's
	 * university.
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

    @GetMapping
    public ListingPage browse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "Available") String status,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        var identity = tokenVerifier.verify(authorization);
        return listingService.browse(identity, new ListingFilters(categoryId, status, condition, q, minPrice, maxPrice, sort),
                limit, cursor, false);
    }

    @GetMapping("/{listingId}")
    public ListingResponse get(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId) {
        return listingService.get(tokenVerifier.verify(authorization), listingId);
    }

    @PutMapping("/{listingId}")
    public ListingResponse update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId, @RequestBody CreateListingRequest request) {
        return listingService.update(tokenVerifier.verify(authorization), listingId, request);
    }

    public record StatusRequest(String status) { }

    @PatchMapping("/{listingId}/status")
    public ListingResponse changeStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId, @RequestBody StatusRequest request) {
        return listingService.changeStatus(tokenVerifier.verify(authorization), listingId, request.status());
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId) {
        listingService.delete(tokenVerifier.verify(authorization), listingId);
    }
}
