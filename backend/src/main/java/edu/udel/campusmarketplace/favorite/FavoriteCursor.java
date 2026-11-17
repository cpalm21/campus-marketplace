package edu.udel.campusmarketplace.favorite;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

record FavoriteCursor(Instant savedAt, UUID listingId) {
    static FavoriteCursor parse(String value) {
        if (value == null) return null;
        try {
            if (value.length() > 200) throw new IllegalArgumentException();
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            Instant savedAt = Instant.parse(parts[0]);
            int year = savedAt.atOffset(java.time.ZoneOffset.UTC).getYear();
            if (year < 1 || year > 9999) throw new IllegalArgumentException();
            return new FavoriteCursor(savedAt, UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid favorites cursor.");
        }
    }

    static String encode(FavoriteResponse favorite) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (favorite.savedAt() + "|" + favorite.listing().listingId()).getBytes(StandardCharsets.UTF_8));
    }
}
