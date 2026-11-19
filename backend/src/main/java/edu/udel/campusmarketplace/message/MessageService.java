package edu.udel.campusmarketplace.message;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Messaging rules.
 *
 * Only the field rules live here so far. The identity, listing, and
 * participation checks need a repository, so they arrive with it; see the TODO
 * on {@link MessageController}.
 *
 * The browser checks the same two rules in messagesService.ts, which rejects
 * blank text and anything over 2,000 characters. That is for fast feedback
 * only. Anyone can POST to this endpoint directly, so the rule that matters is
 * the one on this side. Same reasoning as
 * {@link edu.udel.campusmarketplace.listing.ListingService}.
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
