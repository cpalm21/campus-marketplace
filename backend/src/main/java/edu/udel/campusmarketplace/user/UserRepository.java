package edu.udel.campusmarketplace.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes users.
 *
 * Firebase owns credentials; this repository only stores marketplace profiles.
 */
@Repository
public class UserRepository {

	private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
			rs.getObject("user_id", UUID.class),
			rs.getString("firebase_uid"),
			rs.getString("email"),
			rs.getObject("university_id", UUID.class),
			rs.getTimestamp("created_at") == null
					? null
					: rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	UserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Finds the user with this Firebase UID.
	 *
	 * @return the user, or empty when nobody has registered with that UID
	 */
	public Optional<User> findByFirebaseUid(String firebaseUid) {
		String sql = """
				SELECT user_id, firebase_uid, email, university_id, created_at
				FROM users
				WHERE firebase_uid = ?
				""";

		// query rather than queryForObject: an absent user is an ordinary
		// outcome here, not the exception queryForObject would throw.
		return jdbcTemplate.query(sql, ROW_MAPPER, firebaseUid).stream().findFirst();
	}

	/**
	 * Finds the university that owns an email domain.
	 *
	 * Used by registration and by the shared participating-profile access check.
	 *
	 * Matching on the domain rather than the display name keeps the mapping in
	 * data: adding a university is an insert, not a code change, and a name is
	 * easy to retype slightly differently.
	 *
	 * @param emailDomain the part after the '@', for example "udel.edu"
	 * @return the university's id, or empty when no university claims it
	 */
	public Optional<UUID> findUniversityIdByEmailDomain(String emailDomain) {
		String sql = """
				SELECT university_id
				FROM universities
				WHERE lower(email_domain) = lower(?)
				""";

		return jdbcTemplate
				.query(sql, (rs, rowNum) -> rs.getObject("university_id", UUID.class), emailDomain)
				.stream()
				.findFirst();
	}

	/**
	 * Inserts a new user and returns the stored row.
	 *
	 * RETURNING hands back user_id and created_at in the same round trip, so
	 * the database generates both and we never invent a UUID in Java.
	 */
	public User insert(NewUser newUser) {
		String sql = """
				INSERT INTO users (firebase_uid, email, university_id)
				VALUES (?, ?, ?)
				ON CONFLICT (firebase_uid) DO UPDATE SET firebase_uid = EXCLUDED.firebase_uid
				RETURNING user_id, firebase_uid, email, university_id, created_at
				""";

		return jdbcTemplate.queryForObject(
				sql,
				ROW_MAPPER,
				newUser.firebaseUid(),
				newUser.email(),
				newUser.universityId());
	}

}
