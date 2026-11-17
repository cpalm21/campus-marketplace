package edu.udel.campusmarketplace.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ListingImageRepository {
    private final JdbcTemplate jdbc;

    ListingImageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ListingImage> find(UUID listingId) {
        return jdbc.query("""
                SELECT listing_image_id, image_url, image_order FROM public.listing_images
                WHERE listing_id = ? ORDER BY image_order, listing_image_id
                """, (row, index) -> new ListingImage(row.getObject("listing_image_id", UUID.class),
                row.getString("image_url"), row.getInt("image_order")), listingId);
    }

    public void replace(UUID listingId, List<String> urls) {
        jdbc.update("DELETE FROM public.listing_images WHERE listing_id = ?", listingId);
        for (int index = 0; index < urls.size(); index++) {
            jdbc.update("INSERT INTO public.listing_images(listing_id, image_url, image_order) VALUES (?, ?, ?)",
                    listingId, urls.get(index), index + 1);
        }
        jdbc.update("UPDATE public.listings SET updated_at = clock_timestamp() WHERE listing_id = ?", listingId);
    }
}
