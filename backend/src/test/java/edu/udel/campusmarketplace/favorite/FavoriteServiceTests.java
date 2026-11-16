package edu.udel.campusmarketplace.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;
import edu.udel.campusmarketplace.listing.ListingRepository;
import edu.udel.campusmarketplace.listing.ListingResponse;
import edu.udel.campusmarketplace.listing.Listing;
import edu.udel.campusmarketplace.user.CurrentUserService;
import edu.udel.campusmarketplace.user.User;
import edu.udel.campusmarketplace.user.UserRepository;

class FavoriteServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final ListingRepository listings = mock(ListingRepository.class);
    private final FavoriteRepository favorites = mock(FavoriteRepository.class);
    private final FavoriteService service = new FavoriteService(new CurrentUserService(users), listings, favorites);
    private final UUID userId = UUID.randomUUID();
    private final UUID universityId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();
    private final VerifiedUser identity = new VerifiedUser("buyer", "buyer@udel.edu", true);

    @BeforeEach
    void fixtures() {
        when(users.findByFirebaseUid("buyer")).thenReturn(Optional.of(
                new User(userId, "buyer", "buyer@udel.edu", universityId, Instant.now())));
        when(users.findUniversityIdByEmailDomain("udel.edu")).thenReturn(Optional.of(universityId));
    }

    private Listing listing(UUID seller, String status) {
        return new Listing(listingId, seller, 1, "Desk", BigDecimal.TEN, "Good", null, null,
                status, Instant.now(), Instant.now());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Available", "Reserved"})
    void canSaveAnUnsoldListing(String status) {
        when(listings.find(listingId, universityId, true)).thenReturn(Optional.of(listing(UUID.randomUUID(), status)));
        service.save(identity, listingId);
        verify(favorites).save(userId, listingId);
    }

    @Test
    void cannotSaveOwnListing() {
        when(listings.find(listingId, universityId, true)).thenReturn(Optional.of(listing(userId, "Available")));
        assertStatus(403, () -> service.save(identity, listingId));
        verifyNoInteractions(favorites);
    }

    @Test
    void cannotSaveSoldOrInvisibleListing() {
        when(listings.find(listingId, universityId, true)).thenReturn(Optional.of(listing(UUID.randomUUID(), "Sold")));
        assertStatus(409, () -> service.save(identity, listingId));
        when(listings.find(listingId, universityId, true)).thenReturn(Optional.empty());
        assertStatus(404, () -> service.save(identity, listingId));
        verifyNoInteractions(favorites);
    }

    @Test
    void unverifiedUsersCannotSaveOrRemove() {
        var unverified = new VerifiedUser("buyer", "buyer@udel.edu", false);
        assertStatus(403, () -> service.save(unverified, listingId));
        assertStatus(403, () -> service.remove(unverified, listingId));
        verifyNoInteractions(listings, favorites, users);
    }

    @Test
    void mismatchedEmailAndRevokedUniversityCannotReadFavorites() {
        assertStatus(403, () -> service.browse(new VerifiedUser("buyer", "other@udel.edu", true), 20, null));
        when(users.findUniversityIdByEmailDomain("udel.edu")).thenReturn(Optional.empty());
        assertStatus(403, () -> service.browse(identity, 20, null));
        verifyNoInteractions(favorites);
    }

    @Test
    void unregisteredUsersCannotReadFavorites() {
        assertStatus(403, () -> service.browse(new VerifiedUser("unknown", "unknown@udel.edu", true), 20, null));
        verifyNoInteractions(favorites);
    }

    @Test
    void removesOnlyCallersFavoriteWithoutRequiringVisibleListing() {
        service.remove(identity, listingId);
        verify(favorites).remove(userId, listingId);
        verifyNoInteractions(listings);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE})
    void rejectsUnboundedPageSizes(int limit) {
        assertStatus(400, () -> service.browse(identity, limit, null));
        verifyNoInteractions(favorites);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "!", "eA", "MjAyNi0wMS0wMXw="})
    void rejectsInvalidCursor(String cursor) {
        assertStatus(400, () -> service.browse(identity, 20, cursor));
        verifyNoInteractions(favorites);
    }

    @Test
    void cursorUsesSavedTimeAndLastReturnedListing() {
        var favorite = new FavoriteResponse(ListingResponse.of(listing(UUID.randomUUID(), "Available")), Instant.now());
        when(favorites.browse(userId, universityId, 2, null)).thenReturn(List.of(favorite, favorite));
        FavoritePage page = service.browse(identity, 1, null);
        assertThat(page.items()).containsExactly(favorite);
        assertThat(FavoriteCursor.parse(page.nextCursor())).isEqualTo(new FavoriteCursor(favorite.savedAt(), listingId));
        when(favorites.browse(userId, universityId, 3, null)).thenReturn(List.of(favorite));
        assertThat(service.browse(identity, 2, null).nextCursor()).isNull();
    }

    private static void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(status));
    }
}
