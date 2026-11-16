package edu.udel.campusmarketplace.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.user.NewUser;
import edu.udel.campusmarketplace.user.User;
import edu.udel.campusmarketplace.user.UserRepository;

/** HTTP and real Postgres behavior; each test rolls back its own fixtures. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class ListingIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private ListingRepository listings;
    @Autowired private ListingService service;
    @MockitoBean private TokenVerifier tokens;

    private User seller;
    private User other;
    private User temple;
    private int category;
    private VerifiedUser identity;

    @BeforeEach
    void fixtures() {
        UUID udel = users.findUniversityIdByEmailDomain("udel.edu").orElseThrow();
        UUID templeId = users.findUniversityIdByEmailDomain("temple.edu").orElseThrow();
        String suffix = UUID.randomUUID().toString();
        seller = users.insert(new NewUser("seller-" + suffix, "seller-" + suffix + "@udel.edu", udel));
        other = users.insert(new NewUser("other-" + suffix, "other-" + suffix + "@udel.edu", udel));
        temple = users.insert(new NewUser("temple-" + suffix, "temple-" + suffix + "@temple.edu", templeId));
        category = jdbc.queryForObject("INSERT INTO categories(name) VALUES (?) RETURNING category_id",
                Integer.class, "test-" + suffix);
        identity = new VerifiedUser(seller.firebaseUid(), seller.email(), true);
        when(tokens.verify(any())).thenReturn(identity);
    }

    private String details(String name, String price) {
        return """
                {"categoryId":%d,"title":"%s","price":%s,"condition":"Good","description":"A desk"}
                """.formatted(category, name, price);
    }

    private ListingResponse create(String name) {
        return service.create(identity, new CreateListingRequest(name, BigDecimal.TEN, category, "Good", null, null));
    }

    @Test
    void createReadUpdateAndDeletePreservesHistory() throws Exception {
        String result = mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(details("Desk", "10.50")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.status").value("Available"))
                .andReturn().getResponse().getHeader("Location");
        UUID id = UUID.fromString(result.substring(result.lastIndexOf('/') + 1));

        mvc.perform(get(result)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Desk"));
        mvc.perform(put(result).contentType(MediaType.APPLICATION_JSON).content(details("New name", "0")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(0));
        jdbc.update("INSERT INTO messages(listing_id, sender_id, receiver_id, message) VALUES (?, ?, ?, 'Interested')",
                id, other.userId(), seller.userId());
        jdbc.update("INSERT INTO favorites(user_id, listing_id) VALUES (?, ?)", other.userId(), id);
        jdbc.update("""
                INSERT INTO transactions(listing_id, buyer_id, seller_id, amount, status)
                VALUES (?, ?, ?, 0, 'Cancelled')
                """, id, other.userId(), seller.userId());

        mvc.perform(delete(result)).andExpect(status().isNoContent());
        mvc.perform(get(result)).andExpect(status().isNotFound());
        mvc.perform(delete(result)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM listings WHERE listing_id = ?",
                Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM messages WHERE listing_id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transactions WHERE listing_id = ?", Integer.class, id)).isEqualTo(1);
        mvc.perform(get("/api/listings").param("categoryId", "" + category))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void campusIsolationAndOwnershipAreEnforced() throws Exception {
        ListingResponse listing = create("Desk");
        when(tokens.verify(any())).thenReturn(new VerifiedUser(other.firebaseUid(), other.email(), true));
        mvc.perform(get("/api/listings/" + listing.listingId())).andExpect(status().isOk());
        mvc.perform(delete("/api/listings/" + listing.listingId())).andExpect(status().isForbidden());
        mvc.perform(put("/api/listings/" + listing.listingId()).contentType(MediaType.APPLICATION_JSON)
                .content(details("Stolen", "12"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/listings/" + listing.listingId() + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Sold\"}")).andExpect(status().isForbidden());

        when(tokens.verify(any())).thenReturn(new VerifiedUser(temple.firebaseUid(), temple.email(), true));
        mvc.perform(get("/api/listings/" + listing.listingId())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/listings/" + listing.listingId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/listings").param("categoryId", "" + category))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void reservationReleaseSaleAndTerminalState() throws Exception {
        String url = "/api/listings/" + create("Desk").listingId();
        mvc.perform(patch(url + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Reserved\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Reserved"));
        mvc.perform(delete(url)).andExpect(status().isConflict());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(details("Changed", "1")))
                .andExpect(status().isConflict());
        mvc.perform(patch(url + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Available\"}"))
                .andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(details("Changed", "1")))
                .andExpect(status().isOk());
        mvc.perform(patch(url + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Sold\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(url + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Sold\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(url + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Available\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Sold listings cannot be reopened."));
    }

    @Test
    void paginationHasNoDuplicatesWhenTimestampsTie() throws Exception {
        ListingResponse first = create("First");
        ListingResponse second = create("Second");
        create("Third");
        jdbc.update("UPDATE listings SET created_at = timestamp '2026-01-01 00:00:00' WHERE category_id = ?", category);
        service.changeStatus(identity, second.listingId(), "Reserved");
        ListingPage page1 = service.browse(identity, new ListingFilters(category, "Available", null, null, null, null, "newest"), 1, null, false);
        ListingPage page2 = service.browse(identity, new ListingFilters(category, "Available", null, null, null, null, "newest"), 1, page1.nextCursor(), false);
        assertThat(page1.items()).hasSize(1);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.nextCursor()).isNull();
        assertThat(page1.items().getFirst().listingId()).isNotEqualTo(page2.items().getFirst().listingId());
        assertThat(List.of(page1.items().getFirst().listingId(), page2.items().getFirst().listingId()))
                .contains(first.listingId()).doesNotContain(second.listingId());
        mvc.perform(get("/api/listings").param("categoryId", "" + category).param("status", "Reserved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void unverifiedUsersCanBrowseButCannotWrite() throws Exception {
        String url = "/api/listings/" + create("Desk").listingId();
        when(tokens.verify(any())).thenReturn(new VerifiedUser(seller.firebaseUid(), seller.email(), false));
        mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).content(details("Desk", "10")))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url)).andExpect(status().isForbidden());
    }

    @Test
    void malformedInputsAndMissingTokenReturnClientErrors() throws Exception {
        mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).content(details("Desk", "1.001")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings").param("cursor", "garbage")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings").param("status", "Other")).andExpect(status().isBadRequest());
        when(tokens.verify(null)).thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing token."));
        mvc.perform(get("/api/listings")).andExpect(status().isUnauthorized());
    }

    @Test
    void searchCombinesTextConditionCategoryAndPriceFilters() throws Exception {
        service.create(identity, new CreateListingRequest("Blue lamp", new BigDecimal("12.50"), category,
                "Great", "Adjustable", "North Campus"));
        create("Other lamp");
        mvc.perform(get("/api/listings").param("categoryId", "" + category).param("q", "CAMPUS")
                .param("condition", "Great").param("minPrice", "12.50").param("maxPrice", "12.50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Blue lamp"));
        mvc.perform(get("/api/listings").param("categoryId", "" + category).param("q", "adjustable"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        String categoryName = jdbc.queryForObject("SELECT name FROM categories WHERE category_id = ?", String.class, category);
        mvc.perform(get("/api/listings").param("q", categoryName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void searchTreatsWildcardAndSqlSyntaxAsLiteralText() throws Exception {
        create("100% desk");
        create("100x desk");
        create("under_score");
        for (String query : List.of("100%", "under_")) {
            mvc.perform(get("/api/listings").param("categoryId", "" + category).param("q", query))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        }
        mvc.perform(get("/api/listings").param("categoryId", "" + category).param("q", "' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"newest", "priceAsc", "priceDesc"})
    void paginatesPriceAndTimeTiesWithoutMissingOrRepeatingRows(String sort) throws Exception {
        for (String price : List.of("10", "1", "20", "10")) {
            service.create(identity, new CreateListingRequest("Desk " + price, new BigDecimal(price), category, "Good", null, null));
        }
        jdbc.update("UPDATE listings SET created_at = timestamp '2026-01-01 00:00:00' WHERE category_id = ?", category);
        ListingFilters filters = new ListingFilters(category, "Available", null, null, null, null, sort);
        var seen = new HashSet<UUID>();
        var prices = new ArrayList<BigDecimal>();
        String cursor = null;
        do {
            ListingPage page = service.browse(identity, filters, 1, cursor, false);
            assertThat(page.items()).hasSize(1);
            var item = page.items().getFirst();
            assertThat(seen.add(item.listingId())).isTrue();
            prices.add(item.price());
            cursor = page.nextCursor();
            mvc.perform(get("/api/listings").param("categoryId", "" + category).param("sort", sort)
                    .param("limit", "1")).andExpect(status().isOk());
        } while (cursor != null);
        assertThat(seen).hasSize(4);
        if (sort.equals("priceAsc")) assertThat(prices).isSorted();
        if (sort.equals("priceDesc")) assertThat(prices).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void cursorCannotBeReusedWithChangedFiltersSortOrCampus() throws Exception {
        create("First"); create("Second");
        var filters = new ListingFilters(category, "Available", null, null, null, null, "newest");
        String cursor = service.browse(identity, filters, 1, null, false).nextCursor();
        mvc.perform(get("/api/listings").param("categoryId", "" + category).param("cursor", cursor).param("limit", "2"))
                .andExpect(status().isOk());
        for (ListingFilters changed : List.of(
                new ListingFilters(category, "Available", null, "Desk", null, null, "newest"),
                new ListingFilters(category, "Available", null, null, null, null, "priceAsc"))) {
            assertThatThrownBy(() -> service.browse(identity, changed, 1, cursor, false)).isInstanceOf(ResponseStatusException.class);
        }
        var templeIdentity = new VerifiedUser(temple.firebaseUid(), temple.email(), true);
        assertThatThrownBy(() -> service.browse(templeIdentity, filters, 1, cursor, false)).isInstanceOf(ResponseStatusException.class);
        mvc.perform(get("/api/users/me/listings").param("categoryId", "" + category).param("status", "Available")
                .param("cursor", cursor)).andExpect(status().isBadRequest());
    }

    @Test
    void sellerListingEndpointIncludesOnlyCallersNonDeletedListingsAcrossStatuses() throws Exception {
        create("Available");
        service.changeStatus(identity, create("Reserved").listingId(), "Reserved");
        service.changeStatus(identity, create("Sold").listingId(), "Sold");
        service.delete(identity, create("Deleted").listingId());
        service.create(new VerifiedUser(other.firebaseUid(), other.email(), true),
                new CreateListingRequest("Other seller", BigDecimal.TEN, category, "Good", null, null));
        mvc.perform(get("/api/users/me/listings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));
        mvc.perform(get("/api/users/me/listings").param("status", "Sold")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Sold"));
    }

    @Test
    void imageReplacementKeepsOrderClearsAndUpdatesTimestamp() throws Exception {
        UUID id = create("Desk").listingId();
        String url = "/api/listings/" + id + "/images";
        jdbc.update("UPDATE listings SET updated_at = timestamp '2020-01-01 00:00:00' WHERE listing_id = ?", id);
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageUrls\":[\"https://example.com/cover.jpg\",\"https://example.com/side.jpg\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].imageOrder").value(1)).andExpect(jsonPath("$[1].imageOrder").value(2));
        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value("https://example.com/cover.jpg"));
        UUID originalImageId = jdbc.queryForObject(
                "SELECT listing_image_id FROM listing_images WHERE listing_id = ? AND image_order = 1", UUID.class, id);
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageUrls\":[\"https://example.com/cover.jpg\",\"https://example.com/side.jpg\"]}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT listing_image_id FROM listing_images WHERE listing_id = ? AND image_order = 1",
                UUID.class, id)).isEqualTo(originalImageId);
        assertThat(jdbc.queryForObject("SELECT updated_at > timestamp '2020-01-01' FROM listings WHERE listing_id = ?",
                Boolean.class, id)).isTrue();
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void timestampsAreInterpretedAsUtcRegardlessOfJvmTimezone() {
        UUID id = create("Desk").listingId();
        jdbc.update("UPDATE listings SET created_at = timestamp '2026-01-01 12:34:56' WHERE listing_id = ?", id);
        assertThat(service.get(identity, id).createdAt()).isEqualTo(java.time.Instant.parse("2026-01-01T12:34:56Z"));
    }

    @Test
    void titleLimitCountsDatabaseCharactersRatherThanUtf16Units() {
        String title = "📚".repeat(50);
        ListingResponse listing = service.create(identity, new CreateListingRequest(title, BigDecimal.TEN, category,
                "Good", null, null));
        assertThat(jdbc.queryForObject("SELECT char_length(listing_name) FROM listings WHERE listing_id = ?",
                Integer.class, listing.listingId())).isEqualTo(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"imageUrls\":null}", "{}", "{\"imageUrls\":[null]}",
            "{\"imageUrls\":[\"javascript:alert(1)\"]}", "{\"imageUrls\":[\"http://example.com/a.jpg\"]}",
            "{\"imageUrls\":[\"https://user:pass@example.com/a.jpg\"]}",
            "{\"imageUrls\":[\"https://example.com/a.jpg#fragment\"]}",
            "{\"imageUrls\":[\"https://example.com/a.jpg\",\"https://example.com/a.jpg\"]}",
            "{\"imageUrls\":[\"https://example.com/1\",\"https://example.com/2\",\"https://example.com/3\",\"https://example.com/4\",\"https://example.com/5\"]}"
    })
    void invalidImagesDoNotReplaceExistingMetadata(String body) throws Exception {
        UUID id = create("Desk").listingId();
        String url = "/api/listings/" + id + "/images";
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[\"https://example.com/original.jpg\"]}"))
                .andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].imageUrl").value("https://example.com/original.jpg"));
    }

    @Test
    void imagesRespectOwnershipCampusVerificationAndLifecycle() throws Exception {
        UUID id = create("Desk").listingId();
        String url = "/api/listings/" + id + "/images";
        when(tokens.verify(any())).thenReturn(new VerifiedUser(other.firebaseUid(), other.email(), true));
        mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
                .andExpect(status().isForbidden());
        when(tokens.verify(any())).thenReturn(new VerifiedUser(temple.firebaseUid(), temple.email(), true));
        mvc.perform(get(url)).andExpect(status().isNotFound());
        when(tokens.verify(any())).thenReturn(new VerifiedUser(seller.firebaseUid(), seller.email(), false));
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
                .andExpect(status().isForbidden());
        when(tokens.verify(any())).thenReturn(identity);
        service.changeStatus(identity, id, "Reserved");
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
                .andExpect(status().isConflict());
        service.changeStatus(identity, id, "Sold");
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
                .andExpect(status().isConflict());
    }
}
