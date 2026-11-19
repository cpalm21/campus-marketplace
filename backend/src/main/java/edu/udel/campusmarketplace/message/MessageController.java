package edu.udel.campusmarketplace.message;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.udel.campusmarketplace.auth.TokenVerifier;

/**
 * Message endpoints.
 *
 * Mapped under /api/listings/{listingId}/messages because a message is always
 * about a listing: messages.listing_id is NOT NULL, so there is no such thing
 * as a message that stands on its own. Nesting it says that in the URL and
 * means the listing never has to be repeated in the body.
 *
 * Reading threads will not live here. An inbox is a view of the signed-in
 * student rather than of any one listing, so it belongs on
 * /api/users/me/conversations, next to the existing
 * {@link edu.udel.campusmarketplace.listing.MyListingsController} and
 * {@link edu.udel.campusmarketplace.favorite.FavoriteController}. This class
 * stays the write side.
 */
@RestController
@RequestMapping("/api/listings/{listingId}/messages")
public class MessageController {

	private final MessageService messageService;
	private final TokenVerifier tokenVerifier;

	MessageController(MessageService messageService, TokenVerifier tokenVerifier) {
		this.messageService = messageService;
		this.tokenVerifier = tokenVerifier;
	}

	/**
	 * Sends a message about a listing.
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
	 * on. The same placeholder was used for POST /api/listings before its
	 * repository existed.
	 *
	 * listingId is bound but unused for now. It is in the signature already so
	 * that the route is the real one from the start, rather than a path that
	 * has to change once the lookup arrives.
	 *
	 * The header is optional to Spring only so that a missing one fails as 401
	 * from the verifier rather than as a generic 400, matching
	 * {@link edu.udel.campusmarketplace.user.UserController}.
	 *
	 * TODO Resolve the sender through CurrentUserService, confirm the listing
	 * exists within their university, derive the receiver from its seller,
	 * reject messaging your own listing, insert, and return 201 with the
	 * stored message once MessageRepository exists.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID listingId,
			@RequestBody SendMessageRequest request) {

		tokenVerifier.verify(authorization);

		messageService.validate(request);
	}

}
