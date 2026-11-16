package edu.udel.campusmarketplace.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.favorite.FavoriteService;
import edu.udel.campusmarketplace.user.NewUser;
import edu.udel.campusmarketplace.user.User;
import edu.udel.campusmarketplace.user.UserRepository;

/** Real competing transactions: a mutation must recheck state after a conflicting row lock commits. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class ListingConcurrencyIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private ListingService listings;
    @Autowired private ListingImageService images;
    @Autowired private FavoriteService favorites;
    @Autowired private PlatformTransactionManager transactions;

    private User seller;
    private User buyer;
    private VerifiedUser sellerIdentity;
    private VerifiedUser buyerIdentity;
    private UUID listingId;
    private int category;

    @BeforeEach
    void fixtures() {
        UUID university = users.findUniversityIdByEmailDomain("udel.edu").orElseThrow();
        String suffix = UUID.randomUUID().toString();
        seller = users.insert(new NewUser("seller-" + suffix, "seller-" + suffix + "@udel.edu", university));
        buyer = users.insert(new NewUser("buyer-" + suffix, "buyer-" + suffix + "@udel.edu", university));
        sellerIdentity = new VerifiedUser(seller.firebaseUid(), seller.email(), true);
        buyerIdentity = new VerifiedUser(buyer.firebaseUid(), buyer.email(), true);
        category = jdbc.queryForObject("SELECT category_id FROM categories WHERE name = 'Furniture'", Integer.class);
        listingId = listings.create(sellerIdentity, new CreateListingRequest("Desk", BigDecimal.TEN, category,
                "Good", null, null)).listingId();
    }

    @AfterEach
    void cleanup() {
        if (listingId != null) jdbc.update("DELETE FROM listings WHERE listing_id = ?", listingId);
        for (User user : new User[]{seller, buyer}) {
            if (user != null) jdbc.update("DELETE FROM users WHERE user_id = ?", user.userId());
        }
    }

    @Test
    void salePreventsAWaitingFavoriteInsert() throws Exception {
        assertWaitingMutation(() -> favorites.save(buyerIdentity, listingId), "status = 'Sold'", 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM favorites WHERE listing_id = ?", Integer.class, listingId)).isZero();
    }

    @Test
    void salePreventsAWaitingEdit() throws Exception {
        assertWaitingMutation(() -> listings.update(sellerIdentity, listingId,
                new CreateListingRequest("Changed", BigDecimal.ONE, category, "Good", null, null)), "status = 'Sold'", 409);
        assertThat(listings.get(sellerIdentity, listingId).title()).isEqualTo("Desk");
    }

    @Test
    void deletionPreventsAWaitingImageReplacement() throws Exception {
        assertWaitingMutation(() -> images.replace(sellerIdentity, listingId, List.of("https://example.com/desk.jpg")),
                "deleted_at = clock_timestamp()", 404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_images WHERE listing_id = ?", Integer.class, listingId)).isZero();
    }

    private void assertWaitingMutation(Runnable mutation, String committedChange, int status) throws Exception {
        var future = new AtomicReference<Future<?>>();
        try (var executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                jdbc.queryForObject("SELECT listing_id FROM listings WHERE listing_id = ? FOR UPDATE", UUID.class, listingId);
                int blockerPid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                future.set(executor.submit(mutation));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean blocked = false;
                while (!blocked && System.nanoTime() < deadline) {
                    blocked = Boolean.TRUE.equals(jdbc.queryForObject("""
                            SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid)))
                            """, Boolean.class, blockerPid));
                    if (!blocked) {
                        try { Thread.sleep(20); }
                        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                    }
                }
                assertThat(blocked).as("competing mutation waits for the listing lock").isTrue();
                // Only fixed expressions from the tests enter this SQL.
                jdbc.update("UPDATE listings SET " + committedChange + " WHERE listing_id = ?", listingId);
            });
            assertThatThrownBy(() -> future.get().get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> assertThat(((ResponseStatusException) error.getCause()).getStatusCode().value()).isEqualTo(status));
        }
    }
}
