package edu.udel.campusmarketplace.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udel.campusmarketplace.auth.TokenVerifier;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
public class ListingImageController {
    public record ImageRequest(List<String> imageUrls) { }

    private final ListingImageService service;
    private final TokenVerifier tokens;

    ListingImageController(ListingImageService service, TokenVerifier tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @GetMapping
    public List<ListingImage> get(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId) {
        return service.get(tokens.verify(authorization), listingId);
    }

    @PutMapping
    public List<ListingImage> replace(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID listingId, @RequestBody ImageRequest request) {
        return service.replace(tokens.verify(authorization), listingId, request.imageUrls());
    }
}
