package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validated query values; the repository only interpolates a fixed sort allowlist. */
public record ListingFilters(Integer categoryId, String status, String condition, String q,
        BigDecimal minPrice, BigDecimal maxPrice, String sort) {
    public ListingFilters {
        if (categoryId != null && categoryId < 1) throw invalid("Category must be positive.");
        if (status != null && !Set.of("Available", "Reserved", "Sold").contains(status)) throw invalid("Invalid listing status.");
        if (condition != null && !Set.of("New", "Great", "Good", "Fair", "Poor").contains(condition)) throw invalid("Invalid condition.");
        q = q == null || q.isBlank() ? null : q.strip();
        if (q != null && q.length() > 100) throw invalid("Search text cannot exceed 100 characters.");
        minPrice = price(minPrice);
        maxPrice = price(maxPrice);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw invalid("Minimum price exceeds maximum price.");
        sort = sort == null ? "newest" : sort;
        if (!Set.of("newest", "priceAsc", "priceDesc").contains(sort)) throw invalid("Invalid listing sort.");
    }

    private static BigDecimal price(BigDecimal value) {
        if (value == null) return null;
        if (value.signum() < 0 || value.compareTo(new BigDecimal("99999999.99")) > 0
                || value.stripTrailingZeros().scale() > 2) throw invalid("Invalid price filter.");
        return value.setScale(2);
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
