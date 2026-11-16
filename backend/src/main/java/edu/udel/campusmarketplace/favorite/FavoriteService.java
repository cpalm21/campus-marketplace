package edu.udel.campusmarketplace.favorite;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.listing.ListingRepository;
import edu.udel.campusmarketplace.listing.Listing;
import edu.udel.campusmarketplace.user.CurrentUserService;
import edu.udel.campusmarketplace.user.User;

@Service
public class FavoriteService {
    private final CurrentUserService users;
    private final ListingRepository listings;
    private final FavoriteRepository favorites;

    FavoriteService(CurrentUserService users, ListingRepository listings, FavoriteRepository favorites) {
        this.users = users;
        this.listings = listings;
        this.favorites = favorites;
    }

    public FavoritePage browse(VerifiedUser identity, int limit, String cursor) {
        User user = users.require(identity, false);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100.");
        }
        List<FavoriteResponse> rows = favorites.browse(user.userId(), user.universityId(),
                limit + 1, FavoriteCursor.parse(cursor));
        boolean hasMore = rows.size() > limit;
        List<FavoriteResponse> items = List.copyOf(rows.subList(0, Math.min(limit, rows.size())));
        return new FavoritePage(items, hasMore ? FavoriteCursor.encode(items.getLast()) : null);
    }

    @Transactional
    public void save(VerifiedUser identity, UUID listingId) {
        User user = users.require(identity, true);
        // Use the same row lock as listing mutations so removal or sale cannot pass this check concurrently.
        Listing listing = listings.find(listingId, user.universityId(), true).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found."));
        if (listing.userId().equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot save your own listing.");
        }
        if (listing.status().equals("Sold")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sold listings cannot be saved.");
        }
        favorites.save(user.userId(), listingId);
    }

    public void remove(VerifiedUser identity, UUID listingId) {
        User user = users.require(identity, true);
        // Do not require a visible listing: the caller must be able to remove stale favorites too.
        favorites.remove(user.userId(), listingId);
    }
}
