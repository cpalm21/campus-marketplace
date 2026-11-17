package edu.udel.campusmarketplace.listing;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
			rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
			rs.getObject("updated_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));

    private static final String COLUMNS = """
            l.listing_id, l.user_id, l.category_id, l.listing_name, l.location,
            l.price, l.description, l.status, l.condition, l.created_at, l.updated_at
            """;

	private final JdbcTemplate jdbcTemplate;

	ListingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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

    public Optional<Listing> find(UUID listingId, UUID universityId, boolean lock) {
        return jdbcTemplate.query("SELECT " + COLUMNS + """
                 FROM public.listings l JOIN public.users u ON u.user_id = l.user_id
                 WHERE l.listing_id = ? AND u.university_id = ? AND l.deleted_at IS NULL
                """ + (lock ? " FOR UPDATE OF l" : ""), ROW_MAPPER, listingId, universityId).stream().findFirst();
    }

    public List<Listing> browse(UUID universityId, UUID ownerId, ListingFilters filters, int limit, ListingCursor cursor) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + """
                 FROM public.listings l JOIN public.users u ON u.user_id = l.user_id
                 JOIN public.categories c ON c.category_id = l.category_id
                 WHERE u.university_id = ? AND l.deleted_at IS NULL
                """);
        List<Object> args = new ArrayList<>(List.of(universityId));
        if (ownerId != null) { sql.append(" AND l.user_id = ?"); args.add(ownerId); }
        if (filters.categoryId() != null) { sql.append(" AND l.category_id = ?"); args.add(filters.categoryId()); }
        if (filters.status() != null) { sql.append(" AND l.status = ?"); args.add(filters.status()); }
        if (filters.condition() != null) { sql.append(" AND l.condition = ?"); args.add(filters.condition()); }
        if (filters.minPrice() != null) { sql.append(" AND l.price >= ?"); args.add(filters.minPrice()); }
        if (filters.maxPrice() != null) { sql.append(" AND l.price <= ?"); args.add(filters.maxPrice()); }
        if (filters.q() != null) {
            // LIKE metacharacters are literal user text; values are always bound parameters.
            sql.append(" AND concat_ws(' ', l.listing_name, l.description, l.location, c.name) ILIKE ?");
            args.add("%" + filters.q().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (cursor != null) {
            if (!filters.sort().equals("newest")) {
                String comparison = filters.sort().equals("priceAsc") ? ">" : "<";
                sql.append(" AND (l.price " + comparison + " ? OR (l.price = ? AND (l.created_at, l.listing_id) < (?, ?)))");
                args.add(cursor.price()); args.add(cursor.price());
            } else {
                sql.append(" AND (l.created_at, l.listing_id) < (?, ?)");
            }
            args.add(LocalDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC));
            args.add(cursor.listingId());
        }
        sql.append(switch (filters.sort()) {
            case "priceAsc" -> " ORDER BY l.price ASC, l.created_at DESC, l.listing_id DESC";
            case "priceDesc" -> " ORDER BY l.price DESC, l.created_at DESC, l.listing_id DESC";
            default -> " ORDER BY l.created_at DESC, l.listing_id DESC";
        });
        sql.append(" LIMIT ?"); args.add(limit);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public Listing update(UUID listingId, NewListing details) {
        return jdbcTemplate.queryForObject("""
                UPDATE public.listings AS l SET category_id = ?, listing_name = ?, location = ?,
                    price = ?, description = ?, condition = ?, updated_at = clock_timestamp()
                WHERE listing_id = ? AND deleted_at IS NULL RETURNING
                """ + COLUMNS, ROW_MAPPER, details.categoryId(), details.title(), details.location(),
                details.price(), details.description(), details.condition(), listingId);
    }

    public Listing changeStatus(UUID listingId, String status) {
        return jdbcTemplate.queryForObject("""
                UPDATE public.listings AS l SET status = ?, updated_at = clock_timestamp()
                WHERE listing_id = ? AND deleted_at IS NULL RETURNING
                """ + COLUMNS, ROW_MAPPER, status, listingId);
    }

    public void delete(UUID listingId) {
        jdbcTemplate.update("""
                UPDATE public.listings SET deleted_at = clock_timestamp(), updated_at = clock_timestamp()
                WHERE listing_id = ? AND deleted_at IS NULL
                """, listingId);
    }
}
