package edu.udel.campusmarketplace.listing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Covers the route and the validation rules behind it. The token verifier is
 * mocked, so these run without Firebase credentials; nothing here needs a
 * database, because every rule under test is one the service checks before
 * reaching for one.
 *
 * Header parsing itself is covered in FirebaseTokenVerifierTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTests {

	private static final String FIREBASE_UID = "firebase-uid-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@BeforeEach
	void theCallerIsSignedIn() {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(FIREBASE_UID, "palmerc@udel.edu", true));
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
				  "categoryId": 4,
				  "condition": "Good",
				  "description": "A compact mini fridge, sized for a dorm room.",
				  "location": "Pickup near Trabant Student Center"
				  %s
				}
				""".formatted(overrides);
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
	void createAcceptsAValidListing() throws Exception {
		create(listing(""))
				.andExpect(status().isNoContent());
	}

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
		createWith("title", "\"%s\"".formatted("x".repeat(50))).andExpect(status().isNoContent());
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
		createWith("price", "0").andExpect(status().isNoContent());
	}

	/** price is DECIMAL(10,2), so a third decimal place cannot be stored. */
	@Test
	void createRejectsAPriceWithTooManyDecimalPlaces() throws Exception {
		createWith("price", "9.999").andExpect(status().isBadRequest());
	}

	/** 65.00 is two decimals, not a scale to complain about. */
	@Test
	void createAcceptsAPriceWithTrailingZeros() throws Exception {
		createWith("price", "65.00").andExpect(status().isNoContent());
	}

	@Test
	void createRejectsAPriceTooLargeForTheColumn() throws Exception {
		createWith("price", "100000000").andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsAMissingCategory() throws Exception {
		createWith("categoryId", "null").andExpect(status().isBadRequest());
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
		createWith("description", "null").andExpect(status().isNoContent());
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

	/**
	 * Identity is settled before the body is read, so a signed-out caller gets
	 * 401 rather than a list of everything wrong with their listing.
	 */
	@Test
	void createRejectsAnUnauthenticatedCallerBeforeValidating() throws Exception {
		when(tokenVerifier.verify(any())).thenThrow(
				new org.springframework.web.server.ResponseStatusException(
						org.springframework.http.HttpStatus.UNAUTHORIZED));

		create(listing(", \"title\": null")).andExpect(status().isUnauthorized());
	}

}
