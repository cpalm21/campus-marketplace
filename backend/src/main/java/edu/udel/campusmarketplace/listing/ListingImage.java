package edu.udel.campusmarketplace.listing;

import java.util.UUID;

public record ListingImage(UUID listingImageId, String imageUrl, int imageOrder) {
}
