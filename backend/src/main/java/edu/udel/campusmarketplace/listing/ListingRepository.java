package edu.udel.campusmarketplace.listing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes listings.
 */
@Repository
public class ListingRepository {

	private static final RowMapper<Listing> ROW_MAPPER = (rs, rowNum) -> new Listing(
			rs.getObject("listing_id", UUID.class),
			rs.getObject("user_id", UUID.class),
			rs.getInt("category_id"),
			rs.getString("listing_name"),
			rs.getBigDecimal("price"),
			rs.getString("condition"),
			rs.getString("description"),
			rs.getString("location"),
			rs.getString("status"),
			rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	ListingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Finds the user id behind a Firebase UID.
	 *
	 * Only the id is selected, because that is all a listing needs: it is what
	 * listings.user_id points at. UserRepository.findByFirebaseUid returns the
	 * whole row and is the right call when the profile itself is wanted.
	 *
	 * @return the user id, or empty when nobody has registered with that UID
	 */
	public Optional<UUID> findUserIdByFirebaseUid(String firebaseUid) {
		String sql = """
				SELECT user_id
				FROM users
				WHERE firebase_uid = ?
				""";

		// query rather than queryForObject: a Firebase account whose profile
		// row was never created is an ordinary outcome, not the exception
		// queryForObject would throw.
		return jdbcTemplate
				.query(sql, (rs, rowNum) -> rs.getObject("user_id", UUID.class), firebaseUid)
				.stream()
				.findFirst();
	}

	/**
	 * Whether a category with this id exists.
	 *
	 * Asked before inserting so that an unknown category comes back as a 400
	 * naming the problem. The foreign key would refuse the row anyway, but its
	 * exception does not say which key broke, and listings has two.
	 */
	public boolean categoryExists(int categoryId) {
		String sql = """
				SELECT 1
				FROM categories
				WHERE category_id = ?
				""";

		return !jdbcTemplate.query(sql, (rs, rowNum) -> 1, categoryId).isEmpty();
	}

	/**
	 * Inserts a listing and returns the stored row.
	 *
	 * RETURNING hands back listing_id, created_at and updated_at in the same
	 * round trip, so the database generates all three and we never invent a
	 * UUID or a timestamp in Java.
	 *
	 * The column list is explicit and in schema order. Relying on positional
	 * defaults would break the next time a column is added.
	 */
	public Listing insert(NewListing newListing) {
		String sql = """
				INSERT INTO listings (
					user_id, category_id, listing_name, location,
					price, description, status, condition)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING listing_id, user_id, category_id, listing_name, location,
					created_at, updated_at, price, description, status, condition
				""";

		return jdbcTemplate.queryForObject(
				sql,
				ROW_MAPPER,
				newListing.userId(),
				newListing.categoryId(),
				newListing.title(),
				newListing.location(),
				newListing.price(),
				newListing.description(),
				newListing.status(),
				newListing.condition());
	}

}
