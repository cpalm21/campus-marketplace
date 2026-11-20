package edu.udel.campusmarketplace.message;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.listing.Listing;
import edu.udel.campusmarketplace.listing.ListingRepository;
import edu.udel.campusmarketplace.user.CurrentUserService;
import edu.udel.campusmarketplace.user.User;

/**
 * Messaging rules.
 *
 * Takes a {@link VerifiedUser} rather than a Firebase UID string so that an
 * unverified identity cannot reach these methods by mistake, following
 * {@link edu.udel.campusmarketplace.listing.ListingService}.
 *
 * The browser checks the same two field rules in messagesService.ts, which
 * rejects blank text and anything over 2,000 characters. That is for fast
 * feedback only. Anyone can POST to this endpoint directly, so the rule that
 * matters is the one on this side.
 */
@Service
public class MessageService {

	/**
	 * The longest message we accept.
	 *
	 * Unlike every limit in ListingService, this one is not a column width:
	 * messages.message is TEXT, so Postgres would store a message of any size.
	 * It is a policy limit, picked to match the 2,000 the frontend already
	 * enforces so that the two sides agree, and to stop a single row from
	 * being large enough to make a thread expensive to read.
	 *
	 * Because it is policy rather than schema, changing it needs no migration.
	 */
	private static final int TEXT_MAX = 2000;

	private final MessageRepository messageRepository;
	private final ListingRepository listingRepository;
	private final CurrentUserService users;

	MessageService(MessageRepository messageRepository, ListingRepository listingRepository,
			CurrentUserService users) {
		this.messageRepository = messageRepository;
		this.listingRepository = listingRepository;
		this.users = users;
	}

	/**
	 * Sends a message to the seller of a listing.
	 *
	 * This is the buyer's side of a conversation only. A seller replying needs
	 * to aim at one particular student, which a listing id cannot express
	 * because a seller may be talking to several people about the same item.
	 * That path arrives with the conversation endpoints; a seller who posts
	 * here is told they cannot message themselves, which is accurate.
	 *
	 * The order of the checks is deliberate, following
	 * {@link edu.udel.campusmarketplace.listing.ListingService#create}. Who
	 * the caller is settles first, so an ineligible student is told that
	 * rather than being handed a list of complaints about their text. The
	 * field rule comes next, since it needs no database and a malformed
	 * request should cost no queries. The listing lookup is last, because it
	 * is the only check that has to touch another table.
	 *
	 * @return the stored message, as the sender is allowed to see it
	 * @throws ResponseStatusException 400 for text the endpoint will not
	 *     store, 403 when the caller is not an eligible student or is the
	 *     seller, 404 when the listing is not visible to them, 409 when the
	 *     listing is sold
	 */
	@Transactional
	public MessageResponse send(VerifiedUser identity, UUID listingId, SendMessageRequest request) {
		// Sending a message is changing marketplace data, so this asks for a
		// verified email as well as a registered profile. require also pins
		// the caller to a participating university, which is what makes the
		// listing lookup below a campus boundary rather than a plain id match.
		User sender = users.require(identity, true);

		validate(request);

		// Scoped to the sender's university, so a listing at another school is
		// 404 and not 403: a student should not be able to confirm that an
		// id exists somewhere they cannot see. Soft-deleted listings are
		// excluded by find itself.
		Listing listing = listingRepository.find(listingId, sender.universityId(), false)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found."));

		// messages has CHECK (sender_id <> receiver_id), so this would fail as
		// a 409 from the constraint. Checking it here makes it a 403 that says
		// what the caller actually did wrong.
		if (listing.userId().equals(sender.userId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"You cannot message yourself about your own listing.");
		}

		// Sold is terminal for a listing, so there is nothing left to arrange.
		// Existing threads stay readable: the messages are history, which is
		// why removing a listing only sets deleted_at.
		if (listing.status().equals("Sold")) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This item has sold, so it can no longer be messaged about.");
		}

		// The receiver is the seller, read from the listing we just loaded.
		// It is never taken from the request body, for the same reason
		// SendMessageRequest has no senderId.
		NewMessage newMessage = new NewMessage(
				listing.listingId(), sender.userId(), listing.userId(), trimmed(request.text()));

		return MessageResponse.of(messageRepository.insert(newMessage), sender.userId());
	}

	/**
	 * Rejects a message we will not store.
	 *
	 * Returns normally when the request is acceptable; the only signal is the
	 * exception, so a caller cannot accidentally ignore the result. This
	 * follows ListingService.validate.
	 *
	 * Counts code points rather than chars so that an emoji counts as one
	 * character to the student typing it, the way the textarea counts it,
	 * rather than as the two chars Java stores it in.
	 *
	 * @throws ResponseStatusException 400, naming what was wrong
	 */
	void validate(SendMessageRequest request) {
		if (request == null) {
			throw badRequest("A message body is required.");
		}

		String text = trimmed(request.text());
		if (text.isEmpty()) {
			throw badRequest("A message cannot be empty.");
		}
		if (text.codePointCount(0, text.length()) > TEXT_MAX) {
			throw badRequest("A message cannot be longer than " + TEXT_MAX + " characters.");
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
