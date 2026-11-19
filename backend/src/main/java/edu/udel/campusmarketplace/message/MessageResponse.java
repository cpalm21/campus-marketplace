package edu.udel.campusmarketplace.message;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a message.
 *
 * Internal user IDs are deliberately absent, following
 * {@link edu.udel.campusmarketplace.listing.ListingResponse}. A chat bubble
 * does not need to know who anyone is; it needs to know which side to sit on.
 *
 * Hence isMine rather than a senderId. It is computed from the viewer, so the
 * same stored row becomes isMine=true for the sender and isMine=false for the
 * receiver. That also means a MessageResponse is only meaningful for the one
 * person it was built for, which is why {@link #of} demands a viewer id and
 * there is no no-argument version.
 *
 * The frontend's ChatMessage type in messagesService.ts already has exactly
 * this shape, so a page can render these without a translation step.
 *
 * isRead reports the receiver's state, not the viewer's. For a message you
 * sent it answers "have they seen it"; for one you received it answers
 * "have I opened this thread". There is no separate flag for each side,
 * because messages.is_read is a single column.
 */
record MessageResponse(UUID messageId, String text, Instant sentAt, boolean isMine, boolean isRead) {

	/**
	 * Builds the view of a message as one particular student sees it.
	 *
	 * @param viewerId the users.user_id of whoever is reading, which must be
	 *     one of the message's two participants. The service establishes that
	 *     before calling this; nothing here re-checks it, since a viewer who
	 *     is neither participant should never have been handed the row at all.
	 */
	static MessageResponse of(Message message, UUID viewerId) {
		return new MessageResponse(
				message.messageId(),
				message.text(),
				message.sentAt(),
				message.senderId().equals(viewerId),
				message.isRead());
	}
}
