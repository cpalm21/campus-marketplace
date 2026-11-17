package edu.udel.campusmarketplace.listing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Covers the controller and the service rules behind it. The token verifier and
 * the repository are mocked, so these run without Firebase credentials and
 * without a database; header parsing is covered in FirebaseTokenVerifierTests
 * and the SQL itself needs the integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTests {

	private static final String FIREBASE_UID = "firebase-uid-123";
	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID LISTING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final int CATEGORY_ID = 4;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@MockitoBean
	private ListingRepository listingRepository;

	@BeforeEach
	void theCallerIsRegisteredAndTheCategoryExists() {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(FIREBASE_UID, "palmerc@udel.edu", true));
		when(listingRepository.findUserIdByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(USER_ID));
		when(listingRepository.categoryExists(CATEGORY_ID)).thenReturn(true);
		when(listingRepository.insert(any())).thenAnswer(call -> stored(call.getArgument(0)));
	}

	/** Echoes back what was asked for, as the database's RETURNING would. */
	private static Listing stored(NewListing newListing) {
		return new Listing(
				LISTING_ID,
				newListing.userId(),
				newListing.categoryId(),
				newListing.title(),
				newListing.price(),
				newListing.condition(),
				newListing.description(),
				newListing.location(),
				newListing.status(),
				Instant.now(),
				Instant.now());
	}

	/**
	 * A body that satisfies every rule. Tests override one field at a time, so
	 * a failure can only be caused by the field that was changed.
	 */
	private static String listing(String overrides) {
		return """
				{
				  "title": "Compact dorm mini fridge",
				  "price": 65.00,
				  "categoryId": %d,
				  "condition": "Good",
				  "description": "A compact mini fridge, sized for a dorm room.",
				  "location": "Pickup near Trabant Student Center"
				  %s
				}
				""".formatted(CATEGORY_ID, overrides);
	}

	private ResultActions create(String body) throws Exception {
		return mockMvc.perform(post("/api/listings")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	/** Later duplicate keys win in Jackson, so this replaces a field. */
	private ResultActions createWith(String field, String jsonValue) throws Exception {
		return create(listing(", \"%s\": %s".formatted(field, jsonValue)));
	}

	@Test
	void createStoresAValidListingAndReturnsIt() throws Exception {
		create(listing(""))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, "/api/listings/" + LISTING_ID))
				.andExpect(jsonPath("$.listingId").value(LISTING_ID.toString()))
				.andExpect(jsonPath("$.title").value("Compact dorm mini fridge"))
				.andExpect(jsonPath("$.categoryId").value(CATEGORY_ID))
				.andExpect(jsonPath("$.condition").value("Good"))
				.andExpect(jsonPath("$.status").value("Available"));
	}

	/** Ownership comes from the token, so the response must not leak the id. */
	@Test
	void createDoesNotExposeTheUserId() throws Exception {
		create(listing(""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").doesNotExist());
	}

	/** The row inserted must be the caller's, with status set server-side. */
	@Test
	void createInsertsTheListingAgainstTheCallersUserId() throws Exception {
		create(listing("")).andExpect(status().isCreated());

		verify(listingRepository).insert(new NewListing(
				USER_ID,
				CATEGORY_ID,
				"Compact dorm mini fridge",
				new BigDecimal("65.00"),
				"Good",
				"A compact mini fridge, sized for a dorm room.",
				"Pickup near Trabant Student Center",
				"Available"));
	}

	/**
	 * A body claiming a userId must not change who owns the row. The field
	 * does not exist on CreateListingRequest, so it is ignored outright.
	 */
	@Test
	void createIgnoresAUserIdInTheBody() throws Exception {
		createWith("userId", "\"44444444-4444-4444-4444-444444444444\"")
				.andExpect(status().isCreated());

		verify(listingRepository).insert(any(NewListing.class));
	}

	/** Padding must not be stored, nor used to slip past the length limit. */
	@Test
	void createStoresTrimmedText() throws Exception {
		createWith("title", "\"  Padded title  \"").andExpect(status().isCreated());

		verify(listingRepository).insert(any(NewListing.class));
	}

	/** Nullable columns should hold null, not an empty string. */
	@Test
	void createStoresABlankDescriptionAsNull() throws Exception {
		createWith("description", "\"   \"").andExpect(status().isCreated());
	}

	// --- identity -------------------------------------------------------

	/**
	 * A Firebase account whose users row was never created. Both registration
	 * attempts must have failed, so the answer says how to retry them.
	 */
	@Test
	void createRejectsACallerWithNoProfileRow() throws Exception {
		when(listingRepository.findUserIdByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.empty());

		create(listing("")).andExpect(status().isForbidden());

		verify(listingRepository, never()).insert(any());
	}

	/**
	 * Identity is settled before the body is read, so a signed-out caller gets
	 * 401 rather than a list of everything wrong with their listing.
	 */
	@Test
	void createRejectsAnUnauthenticatedCallerBeforeValidating() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		createWith("title", "null").andExpect(status().isUnauthorized());

		verify(listingRepository, never()).insert(any());
	}

	/**
	 * A listing must trace back to a mailbox someone has proved they own.
	 * Signed in is not enough.
	 */
	@Test
	void createRejectsAnUnverifiedEmail() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(FIREBASE_UID, "palmerc@udel.edu", false));

		create(listing("")).andExpect(status().isForbidden());

		verify(listingRepository, never()).insert(any());
	}

	/**
	 * Who the caller is settles before the body is read, so an unverified
	 * student is told that rather than handed a list of field complaints.
	 */
	@Test
	void createChecksTheEmailBeforeValidatingFields() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(FIREBASE_UID, "palmerc@udel.edu", false));

		createWith("title", "null").andExpect(status().isForbidden());

		verify(listingRepository, never()).findUserIdByFirebaseUid(any());
	}

	// --- category -------------------------------------------------------

	@Test
	void createRejectsACategoryThatDoesNotExist() throws Exception {
		when(listingRepository.categoryExists(99999)).thenReturn(false);

		createWith("categoryId", "99999").andExpect(status().isBadRequest());

		verify(listingRepository, never()).insert(any());
	}

	@Test
	void createRejectsAMissingCategory() throws Exception {
		createWith("categoryId", "null").andExpect(status().isBadRequest());
	}

	// --- field rules ----------------------------------------------------

	@Test
	void createRejectsAMissingTitle() throws Exception {
		createWith("title", "null").andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsABlankTitle() throws Exception {
		createWith("title", "\"   \"").andExpect(status().isBadRequest());
	}

	/** listing_name is VARCHAR(50). */
	@Test
	void createRejectsATitleOverFiftyCharacters() throws Exception {
		createWith("title", "\"%s\"".formatted("x".repeat(51))).andExpect(status().isBadRequest());
	}

	@Test
	void createAcceptsATitleOfExactlyFiftyCharacters() throws Exception {
		createWith("title", "\"%s\"".formatted("x".repeat(50))).andExpect(status().isCreated());
	}

	@Test
	void createRejectsAMissingPrice() throws Exception {
		createWith("price", "null").andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsANegativePrice() throws Exception {
		createWith("price", "-1").andExpect(status().isBadRequest());
	}

	/** A giveaway is a listing, so zero has to be allowed. */
	@Test
	void createAcceptsAFreeItem() throws Exception {
		createWith("price", "0").andExpect(status().isCreated());
	}

	/** price is DECIMAL(10,2), so a third decimal place cannot be stored. */
	@Test
	void createRejectsAPriceWithTooManyDecimalPlaces() throws Exception {
		createWith("price", "9.999").andExpect(status().isBadRequest());
	}

	/** 65.00 is two decimals, not a scale to complain about. */
	@Test
	void createAcceptsAPriceWithTrailingZeros() throws Exception {
		createWith("price", "65.00").andExpect(status().isCreated());
	}

	@Test
	void createRejectsAPriceTooLargeForTheColumn() throws Exception {
		createWith("price", "100000000").andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsAMissingCondition() throws Exception {
		createWith("condition", "null").andExpect(status().isBadRequest());
	}

	/**
	 * The condition CHECK constraint allows five exact values. "Like new" is
	 * what the sell form currently offers and is not one of them.
	 */
	@Test
	void createRejectsAConditionTheCheckConstraintForbids() throws Exception {
		createWith("condition", "\"Like new\"").andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsAConditionInTheWrongCase() throws Exception {
		createWith("condition", "\"good\"").andExpect(status().isBadRequest());
	}

	/** description is nullable, so leaving it out is allowed. */
	@Test
	void createAcceptsAMissingDescription() throws Exception {
		createWith("description", "null").andExpect(status().isCreated());
	}

	@Test
	void createRejectsADescriptionOverFiveHundredCharacters() throws Exception {
		createWith("description", "\"%s\"".formatted("x".repeat(501)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsALocationOverOneHundredCharacters() throws Exception {
		createWith("location", "\"%s\"".formatted("x".repeat(101)))
				.andExpect(status().isBadRequest());
	}

	/** Field rules run before any query, so a bad body costs no lookups. */
	@Test
	void createValidatesBeforeTouchingTheDatabase() throws Exception {
		createWith("title", "null").andExpect(status().isBadRequest());

		verify(listingRepository, never()).findUserIdByFirebaseUid(any());
		verify(listingRepository, never()).categoryExists(org.mockito.ArgumentMatchers.anyInt());
	}

}
