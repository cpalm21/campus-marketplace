package edu.udel.campusmarketplace.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.listing.CreateListingRequest;
import edu.udel.campusmarketplace.listing.ListingResponse;
import edu.udel.campusmarketplace.listing.ListingService;
import edu.udel.campusmarketplace.user.NewUser;
import edu.udel.campusmarketplace.user.User;
import edu.udel.campusmarketplace.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class FavoriteIntegrationTests {
    private static final String URL = "/api/users/me/favorites";
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private ListingService listings;
    @Autowired private FavoriteService favorites;
    @MockitoBean private TokenVerifier tokens;

    private User seller;
    private User buyer;
    private User temple;
    private VerifiedUser sellerIdentity;
    private VerifiedUser buyerIdentity;
    private int category;

    @BeforeEach
    void fixtures() {
        String suffix = UUID.randomUUID().toString();
        UUID udel = users.findUniversityIdByEmailDomain("udel.edu").orElseThrow();
        UUID templeId = users.findUniversityIdByEmailDomain("temple.edu").orElseThrow();
        seller = users.insert(new NewUser("seller-" + suffix, "seller-" + suffix + "@udel.edu", udel));
        buyer = users.insert(new NewUser("buyer-" + suffix, "buyer-" + suffix + "@udel.edu", udel));
        temple = users.insert(new NewUser("temple-" + suffix, "temple-" + suffix + "@temple.edu", templeId));
        sellerIdentity = new VerifiedUser(seller.firebaseUid(), seller.email(), true);
        buyerIdentity = new VerifiedUser(buyer.firebaseUid(), buyer.email(), true);
        category = jdbc.queryForObject("SELECT category_id FROM categories WHERE name = 'Furniture'", Integer.class);
        when(tokens.verify(any())).thenReturn(buyerIdentity);
    }

    private ListingResponse create(String name) {
        return listings.create(sellerIdentity, new CreateListingRequest(name, BigDecimal.TEN, category, "Good", null, null));
    }

    @Test
    void saveAndRemoveAreIdempotentAndPreserveOriginalSaveTime() throws Exception {
        ListingResponse listing = create("Desk");
        String url = URL + "/" + listing.listingId();
        mvc.perform(put(url)).andExpect(status().isNoContent());
        var savedAt = jdbc.queryForObject("SELECT created_at FROM favorites WHERE user_id = ? AND listing_id = ?",
                java.sql.Timestamp.class, buyer.userId(), listing.listingId());
        mvc.perform(put(url)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT created_at FROM favorites WHERE user_id = ? AND listing_id = ?",
                java.sql.Timestamp.class, buyer.userId(), listing.listingId())).isEqualTo(savedAt);
        mvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].listing.listingId").value(listing.listingId().toString()))
                .andExpect(jsonPath("$.items[0].savedAt").isNotEmpty());
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/" + UUID.randomUUID())).andExpect(status().isNoContent());
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        assertThat(listings.get(sellerIdentity, listing.listingId()).title()).isEqualTo("Desk");
    }

    @Test
    void favoritesArePrivateAndCannotBeRemovedByAnotherUser() throws Exception {
        ListingResponse listing = create("Desk");
        favorites.save(buyerIdentity, listing.listingId());
        when(tokens.verify(any())).thenReturn(sellerIdentity);
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(delete(URL + "/" + listing.listingId())).andExpect(status().isNoContent());
        assertThat(favorites.browse(buyerIdentity, 20, null).items()).hasSize(1);
        mvc.perform(put(URL + "/" + listing.listingId())).andExpect(status().isForbidden());
    }

    @Test
    void cannotSaveCrossCampusMissingOrDeletedListings() throws Exception {
        ListingResponse listing = create("Desk");
        when(tokens.verify(any())).thenReturn(new VerifiedUser(temple.firebaseUid(), temple.email(), true));
        mvc.perform(put(URL + "/" + listing.listingId())).andExpect(status().isNotFound());
        when(tokens.verify(any())).thenReturn(buyerIdentity);
        mvc.perform(put(URL + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
        listings.delete(sellerIdentity, listing.listingId());
        mvc.perform(put(URL + "/" + listing.listingId())).andExpect(status().isNotFound());
    }

    @Test
    void deletedListingsAreHiddenButStaleFavoritesCanStillBeRemoved() throws Exception {
        ListingResponse listing = create("Desk");
        favorites.save(buyerIdentity, listing.listingId());
        listings.delete(sellerIdentity, listing.listingId());
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM favorites WHERE user_id = ?", Integer.class, buyer.userId()))
                .isEqualTo(1);
        mvc.perform(delete(URL + "/" + listing.listingId())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM favorites WHERE user_id = ?", Integer.class, buyer.userId()))
                .isZero();
    }

    @Test
    void savedListingsRetainCurrentStatusButNewSavesOfSoldListingsAreRejected() throws Exception {
        ListingResponse listing = create("Desk");
        listings.changeStatus(sellerIdentity, listing.listingId(), "Reserved");
        mvc.perform(put(URL + "/" + listing.listingId())).andExpect(status().isNoContent());
        listings.changeStatus(sellerIdentity, listing.listingId(), "Sold");
        mvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].listing.status").value("Sold"));
        mvc.perform(put(URL + "/" + listing.listingId())).andExpect(status().isConflict());
        mvc.perform(delete(URL + "/" + listing.listingId())).andExpect(status().isNoContent());
    }

    @Test
    void legacyCrossCampusFavoritesAreHiddenButRemovable() throws Exception {
        var templeIdentity = new VerifiedUser(temple.firebaseUid(), temple.email(), true);
        var listing = listings.create(templeIdentity, new CreateListingRequest("Temple desk", BigDecimal.TEN, category, "Good", null, null));
        jdbc.update("INSERT INTO favorites(user_id, listing_id) VALUES (?, ?)", buyer.userId(), listing.listingId());
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(delete(URL + "/" + listing.listingId())).andExpect(status().isNoContent());
    }

    @Test
    void paginationUsesSaveTimeAndDoesNotRepeatTiedRows() throws Exception {
        var first = create("First");
        var second = create("Second");
        var third = create("Third");
        for (var listing : List.of(first, second, third)) favorites.save(buyerIdentity, listing.listingId());
        jdbc.update("UPDATE favorites SET created_at = timestamp '2026-01-01 00:00:00' WHERE user_id = ?", buyer.userId());
        FavoritePage page = favorites.browse(buyerIdentity, 1, null);
        var seen = new HashSet<UUID>();
        String cursor;
        do {
            assertThat(page.items()).hasSize(1);
            assertThat(seen.add(page.items().getFirst().listing().listingId())).isTrue();
            cursor = page.nextCursor();
            if (cursor != null) {
                mvc.perform(get(URL).param("limit", "1").param("cursor", cursor)).andExpect(status().isOk());
                page = favorites.browse(buyerIdentity, 1, cursor);
            }
        } while (cursor != null);
        assertThat(seen).containsExactlyInAnyOrder(first.listingId(), second.listingId(), third.listingId());
        // Listing creation order must not override save order.
        jdbc.update("UPDATE favorites SET created_at = timestamp '2030-01-01 00:00:00' WHERE user_id = ? AND listing_id = ?",
                buyer.userId(), first.listingId());
        assertThat(favorites.browse(buyerIdentity, 1, null).items().getFirst().listing().listingId()).isEqualTo(first.listingId());
    }

    @Test
    void verificationAndRequestValidationAreEnforced() throws Exception {
        String url = URL + "/" + create("Desk").listingId();
        when(tokens.verify(any())).thenReturn(new VerifiedUser(buyer.firebaseUid(), buyer.email(), false));
        mvc.perform(get(URL)).andExpect(status().isOk());
        mvc.perform(put(url)).andExpect(status().isForbidden());
        mvc.perform(delete(url)).andExpect(status().isForbidden());
        mvc.perform(get(URL).param("limit", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("limit", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("cursor", "!bad")).andExpect(status().isBadRequest());
        mvc.perform(put(URL + "/invalid-uuid")).andExpect(status().isBadRequest());
        when(tokens.verify(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token."));
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(url)).andExpect(status().isUnauthorized());
        mvc.perform(delete(url)).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSavesCreateExactlyOneFavorite() throws Exception {
        ListingResponse listing = create("Concurrent desk");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var save = (java.util.concurrent.Callable<Void>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                favorites.save(buyerIdentity, listing.listingId());
                return null;
            };
            var first = executor.submit(save);
            var second = executor.submit(save);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM favorites WHERE user_id = ? AND listing_id = ?",
                    Integer.class, buyer.userId(), listing.listingId())).isEqualTo(1);
        } finally {
            start.countDown();
            jdbc.update("DELETE FROM favorites WHERE listing_id = ?", listing.listingId());
            jdbc.update("DELETE FROM listings WHERE listing_id = ?", listing.listingId());
            for (var user : List.of(seller, buyer, temple)) jdbc.update("DELETE FROM users WHERE user_id = ?", user.userId());
        }
    }
}
