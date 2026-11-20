package edu.udel.campusmarketplace.message;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes messages.
 */
@Repository
public class MessageRepository {

	/**
	 * sent_at is read defensively because the column is still nullable: the
	 * migration that makes it NOT NULL has not been applied yet. Once it has,
	 * the null branch is unreachable and can go, matching
	 * {@link edu.udel.campusmarketplace.listing.ListingRepository}, which
	 * reads created_at without a guard because its migration already ran.
	 */
	private static final RowMapper<Message> ROW_MAPPER = (rs, rowNum) -> new Message(
			rs.getObject("message_id", UUID.class),
			rs.getObject("listing_id", UUID.class),
			rs.getObject("sender_id", UUID.class),
			rs.getObject("receiver_id", UUID.class),
			rs.getString("message"),
			rs.getObject("sent_at", LocalDateTime.class) == null
					? null
					: rs.getObject("sent_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
			rs.getBoolean("is_read"));

	private final JdbcTemplate jdbcTemplate;

	MessageRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Inserts a message and returns the stored row.
	 *
	 * RETURNING hands back message_id, sent_at and is_read in the same round
	 * trip, so the database generates all three and we never invent a UUID, a
	 * timestamp, or a read flag in Java. Same reasoning as
	 * {@link edu.udel.campusmarketplace.listing.ListingRepository#insert}.
	 *
	 * The column list is explicit and in schema order. Relying on positional
	 * defaults would break the next time a column is added.
	 */
	public Message insert(NewMessage newMessage) {
		String sql = """
				INSERT INTO public.messages (listing_id, sender_id, receiver_id, message)
				VALUES (?, ?, ?, ?)
				RETURNING message_id, listing_id, sender_id, receiver_id, message, sent_at, is_read
				""";

		return jdbcTemplate.queryForObject(
				sql,
				ROW_MAPPER,
				newMessage.listingId(),
				newMessage.senderId(),
				newMessage.receiverId(),
				newMessage.text());
	}

}
