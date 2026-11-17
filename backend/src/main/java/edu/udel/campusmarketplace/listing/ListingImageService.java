package edu.udel.campusmarketplace.listing;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.user.CurrentUserService;
import edu.udel.campusmarketplace.user.User;

@Service
public class ListingImageService {
    private final CurrentUserService users;
    private final ListingRepository listings;
    private final ListingImageRepository images;

    ListingImageService(CurrentUserService users, ListingRepository listings, ListingImageRepository images) {
        this.users = users;
        this.listings = listings;
        this.images = images;
    }

    public List<ListingImage> get(VerifiedUser identity, UUID listingId) {
        find(listingId, users.require(identity, false), false);
        return images.find(listingId);
    }

    @Transactional
    public List<ListingImage> replace(VerifiedUser identity, UUID listingId, List<String> urls) {
        User user = users.require(identity, true);
        Listing listing = find(listingId, user, true);
        if (!listing.userId().equals(user.userId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the seller can change listing images.");
        ListingService.requireAvailable(listing);
        if (urls == null || urls.size() > 4) throw invalid();
        List<String> normalized = urls.stream().map(ListingImageService::validateUrl).toList();
        if (new HashSet<>(normalized).size() != normalized.size()) throw invalid();
        List<ListingImage> existing = images.find(listingId);
        if (existing.stream().map(ListingImage::imageUrl).toList().equals(normalized)) return existing;
        images.replace(listingId, normalized);
        return images.find(listingId);
    }

    private Listing find(UUID id, User user, boolean lock) {
        return listings.find(id, user.universityId(), lock).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found."));
    }

    private static String validateUrl(String value) {
        if (value == null) throw invalid();
        String url = value.strip();
        if (url.length() > 500) throw invalid();
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) throw invalid();
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        // These are stored links; the backend never fetches arbitrary URLs.
        return url;
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Supply up to four distinct HTTPS image URLs, each at most 500 characters, without credentials or fragments.");
    }
}
