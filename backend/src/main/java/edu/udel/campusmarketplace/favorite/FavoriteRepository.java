package edu.udel.campusmarketplace.favorite;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.udel.campusmarketplace.listing.Listing;
import edu.udel.campusmarketplace.listing.ListingResponse;

@Repository
public class FavoriteRepository {
    private final JdbcTemplate jdbc;

    FavoriteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FavoriteResponse> browse(UUID userId, UUID universityId, int limit, FavoriteCursor cursor) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.listing_id, l.user_id, l.category_id, l.listing_name, l.location,
                       l.price, l.description, l.status, l.condition, l.created_at, l.updated_at,
                       f.created_at AS saved_at
                FROM public.favorites f
                JOIN public.listings l ON l.listing_id = f.listing_id
                JOIN public.users seller ON seller.user_id = l.user_id
                WHERE f.user_id = ? AND seller.university_id = ? AND l.deleted_at IS NULL
                """);
        List<Object> arguments = new ArrayList<>(List.of(userId, universityId));
        if (cursor != null) {
            sql.append(" AND (f.created_at, f.listing_id) < (?, ?)");
            arguments.add(LocalDateTime.ofInstant(cursor.savedAt(), ZoneOffset.UTC));
            arguments.add(cursor.listingId());
        }
        sql.append(" ORDER BY f.created_at DESC, f.listing_id DESC LIMIT ?");
        arguments.add(limit);
        return jdbc.query(sql.toString(), (row, index) -> new FavoriteResponse(ListingResponse.of(new Listing(
                row.getObject("listing_id", UUID.class), row.getObject("user_id", UUID.class),
                row.getInt("category_id"), row.getString("listing_name"), row.getBigDecimal("price"),
                row.getString("condition"), row.getString("description"), row.getString("location"),
                row.getString("status"), row.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                row.getObject("updated_at", LocalDateTime.class).toInstant(ZoneOffset.UTC))),
                row.getObject("saved_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                arguments.toArray());
    }

    public void save(UUID userId, UUID listingId) {
        // The primary key arbitrates duplicate and simultaneous requests without changing saved_at.
        jdbc.update("""
                INSERT INTO public.favorites(user_id, listing_id) VALUES (?, ?)
                ON CONFLICT (user_id, listing_id) DO NOTHING
                """, userId, listingId);
    }

    public void remove(UUID userId, UUID listingId) {
        jdbc.update("DELETE FROM public.favorites WHERE user_id = ? AND listing_id = ?", userId, listingId);
    }
}
