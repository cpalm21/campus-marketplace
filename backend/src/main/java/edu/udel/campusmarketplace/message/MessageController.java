package edu.udel.campusmarketplace.message;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
	 * Sends a message to the seller of a listing.
	 *
	 * The identity is established before the body is looked at, so an
	 * unauthenticated caller gets 401 and learns nothing about which fields
	 * this endpoint would have complained about.
	 *
	 * Responds 201 with the stored message. There is no Location header, which
	 * is a deliberate difference from POST /api/listings: a single message has
	 * no endpoint of its own to point at, since messages are only ever read a
	 * thread at a time. Inventing GET /api/messages/{id} just to satisfy the
	 * header would add a route nothing calls.
	 *
	 * The header is optional to Spring only so that a missing one fails as 401
	 * from the verifier rather than as a generic 400, matching
	 * {@link edu.udel.campusmarketplace.user.UserController}.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MessageResponse send(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID listingId,
			@RequestBody SendMessageRequest request) {

		return messageService.send(tokenVerifier.verify(authorization), listingId, request);
	}

	/**
	 * Reads the caller's conversation about a listing, oldest message first.
	 *
	 * Lives on the same path as the POST because it answers the question that
	 * POST creates: having messaged a seller, a student needs to see the
	 * exchange. A listing id is enough to name it, since a buyer has at most
	 * one conversation per item.
	 *
	 * Returns a bare array rather than a paged object. Nothing is truncated
	 * that a caller could page past yet, so a nextCursor field would be a
	 * promise this endpoint cannot keep; the wrapper arrives with the cursor
	 * that justifies it.
	 */
	@GetMapping
	public List<MessageResponse> thread(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID listingId) {

		return messageService.thread(tokenVerifier.verify(authorization), listingId);
	}

}
