package edu.udel.campusmarketplace.favorite;

import java.time.Instant;

import edu.udel.campusmarketplace.listing.ListingResponse;

public record FavoriteResponse(ListingResponse listing, Instant savedAt) {
}
