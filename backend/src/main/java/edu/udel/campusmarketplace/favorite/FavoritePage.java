package edu.udel.campusmarketplace.favorite;

import java.util.List;

public record FavoritePage(List<FavoriteResponse> items, String nextCursor) {
}
