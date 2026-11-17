package edu.udel.campusmarketplace.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;

class ListingFiltersTests {
    static Stream<Object[]> invalidFilters() {
        return Stream.of(
                new Object[]{0, null, null, null, null, null, "newest"},
                new Object[]{null, "invalid", null, null, null, null, "newest"},
                new Object[]{null, null, "Like new", null, null, null, "newest"},
                new Object[]{null, null, null, "x".repeat(101), null, null, "newest"},
                new Object[]{null, null, null, null, new BigDecimal("-1"), null, "newest"},
                new Object[]{null, null, null, null, new BigDecimal("1.001"), null, "newest"},
                new Object[]{null, null, null, null, null, new BigDecimal("100000000"), "newest"},
                new Object[]{null, null, null, null, BigDecimal.TEN, BigDecimal.ONE, "newest"},
                new Object[]{null, null, null, null, null, null, "price; DROP TABLE listings"});
    }

    @ParameterizedTest
    @MethodSource("invalidFilters")
    void rejectsInvalidQueries(Integer category, String status, String condition, String q,
            BigDecimal min, BigDecimal max, String sort) {
        assertThatThrownBy(() -> new ListingFilters(category, status, condition, q, min, max, sort))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void normalizesEquivalentQueriesForCursorScope() {
        var filters = new ListingFilters(null, null, null, "  desk  ", new BigDecimal("1.0"), null, null);
        assertThat(filters.q()).isEqualTo("desk");
        assertThat(filters.minPrice()).isEqualTo(new BigDecimal("1.00"));
        assertThat(filters.sort()).isEqualTo("newest");
    }
}
