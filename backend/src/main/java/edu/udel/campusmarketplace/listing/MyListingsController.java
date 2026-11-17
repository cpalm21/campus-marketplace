package edu.udel.campusmarketplace.listing;

import java.math.BigDecimal;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udel.campusmarketplace.auth.TokenVerifier;

@RestController
@RequestMapping("/api/users/me/listings")
public class MyListingsController {
    private final ListingService service;
    private final TokenVerifier tokens;

    MyListingsController(ListingService service, TokenVerifier tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @GetMapping
    public ListingPage browse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        var identity = tokens.verify(authorization);
        return service.browse(identity, new ListingFilters(categoryId, status, condition, q, minPrice, maxPrice, sort),
                limit, cursor, true);
    }
}
