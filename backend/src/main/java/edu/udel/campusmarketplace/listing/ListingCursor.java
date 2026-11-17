package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Keyset position bound to the campus, seller scope, filters, and sorting. */
record ListingCursor(Instant createdAt, UUID listingId, BigDecimal price) {
    static String scope(UUID universityId, UUID ownerId, ListingFilters filters) {
        try {
            byte[] bytes = (universityId + ":" + ownerId + ":" + filters).getBytes(StandardCharsets.UTF_8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static ListingCursor parse(String value, String scope) {
        if (value == null) return null;
        try {
            if (value.length() > 300) throw new IllegalArgumentException();
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 5 || !parts[0].equals("1") || !parts[1].equals(scope)) throw new IllegalArgumentException();
            Instant createdAt = Instant.parse(parts[2]);
            int year = createdAt.atOffset(ZoneOffset.UTC).getYear();
            BigDecimal price = new BigDecimal(parts[4]);
            if (year < 1 || year > 9999 || price.signum() < 0 || price.compareTo(new BigDecimal("99999999.99")) > 0
                    || price.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException();
            return new ListingCursor(createdAt, UUID.fromString(parts[3]), price);
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor or changed listing filters.");
        }
    }

    static String encode(Listing listing, String scope) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("1|" + scope + "|" + listing.createdAt() + "|" + listing.listingId() + "|" + listing.price())
                        .getBytes(StandardCharsets.UTF_8));
    }
}
