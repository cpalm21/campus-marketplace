package edu.udel.campusmarketplace.listing;

import java.util.List;

public record ListingPage(List<ListingResponse> items, String nextCursor) {
}
