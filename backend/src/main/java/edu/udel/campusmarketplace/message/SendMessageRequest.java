package edu.udel.campusmarketplace.message;

/**
 * A send-message request exactly as the browser sent it. Untrusted.
 *
 * Carries nothing but the text. There is deliberately no senderId, no
 * receiverId, and no listingId, for the same reason
 * {@link edu.udel.campusmarketplace.listing.CreateListingRequest} has no
 * userId: anything a caller could lie about must not be read from the body.
 *
 * The sender comes from the verified Firebase ID token. The listing comes from
 * the URL path. The receiver is never sent at all, because it is derivable: a
 * listing has exactly one seller, so the other end of a conversation is either
 * that seller or the student talking to them. Accepting a receiverId would let
 * a caller message someone about a listing they are not part of, and would
 * also let them aim a message at themselves, which the messages table forbids
 * with CHECK (sender_id &lt;&gt; receiver_id).
 *
 * {@link NewMessage} is the type that carries the identities, and it can only
 * be built once the token has been verified.
 */
public record SendMessageRequest(String text) {
}
