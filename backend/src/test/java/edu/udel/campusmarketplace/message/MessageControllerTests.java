package edu.udel.campusmarketplace.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Covers the controller and the field rules behind it. The token verifier is
 * mocked, so these run without Firebase credentials and without a database;
 * header parsing is covered in FirebaseTokenVerifierTests.
 *
 * Nothing is stored yet, so a well-formed request is a 204. These tests pin
 * the contract that the insert will later have to keep, following
 * ListingControllerTests, which was written against the same placeholder
 * before ListingRepository existed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTests {

	private static final String FIREBASE_UID = "firebase-uid-123";
	private static final UUID LISTING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	/**
	 * Built here rather than autowired: this application exposes no
	 * ObjectMapper bean, and these tests only need one to escape a body.
	 */
	private static final ObjectMapper JSON = new ObjectMapper();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@BeforeEach
	void theCallerIsSignedIn() {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(FIREBASE_UID, "palmerc@udel.edu", true));
	}

	@Test
	void sendsAMessage() throws Exception {
		send(text("Is the desk still available?")).andExpect(status().isNoContent());
	}

	/**
	 * The limit counts what the student typed, not what Java stores. A
	 * 2,000-character message is accepted whether or not its characters each
	 * happen to fit in one char, which is what codePointCount buys us.
	 */
	@Test
	void sendsAMessageOfExactlyTheMaximumLength() throws Exception {
		send(text("a".repeat(2000))).andExpect(status().isNoContent());
	}

	@Test
	void sendsTwoThousandEmojiEvenThoughJavaStoresThemAsFourThousandChars() throws Exception {
		send(text("😀".repeat(2000))).andExpect(status().isNoContent());
	}

	@Test
	void rejectsAMessageOverTheMaximumLength() throws Exception {
		send(text("a".repeat(2001)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be longer than 2000 characters."));
	}

	@Test
	void rejectsAnEmptyMessage() throws Exception {
		send(text("")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be empty."));
	}

	/** Whitespace is not a message, and the stored text is trimmed anyway. */
	@Test
	void rejectsAWhitespaceOnlyMessage() throws Exception {
		send(text("   \n\t  ")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be empty."));
	}

	@Test
	void rejectsANullMessage() throws Exception {
		send(text(null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be empty."));
	}

	/** An absent field and an explicit null read the same to the caller. */
	@Test
	void rejectsABodyWithNoTextField() throws Exception {
		send("{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be empty."));
	}

	/**
	 * A body Jackson cannot read never reaches the service, so the message
	 * comes from ApiExceptionHandler rather than naming a field.
	 */
	@Test
	void rejectsAMalformedBody() throws Exception {
		send("{\"text\":").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request body or parameter."));
	}

	/**
	 * Identity settles before the body is looked at, so a caller who is not
	 * signed in is told that and nothing about their fields. The body here
	 * would have failed validation, which is the point: the status is 401 and
	 * not 400.
	 */
	@Test
	void rejectsAnUnauthenticatedSenderBeforeLookingAtTheBody() throws Exception {
		when(tokenVerifier.verify(any())).thenThrow(new ResponseStatusException(
				HttpStatus.UNAUTHORIZED, "Expected an Authorization: Bearer <Firebase ID token> header."));

		send(text("")).andExpect(status().isUnauthorized());
	}

	/**
	 * A listing id that is not a UUID fails in Spring's path binding, which
	 * runs before the handler and therefore before the token is checked. That
	 * is the one case where a caller learns something without being
	 * authenticated, and it is only that the URL was not a URL.
	 */
	@Test
	void rejectsAListingIdThatIsNotAUuid() throws Exception {
		mockMvc.perform(post("/api/listings/not-a-uuid/messages")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(text("Hello")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request body or parameter."));
	}

	private ResultActions send(String body) throws Exception {
		return mockMvc.perform(post("/api/listings/{listingId}/messages", LISTING_ID)
				.header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	/** Builds a request body, so no test has to escape JSON by hand. */
	private static String text(String value) throws Exception {
		Map<String, String> body = new HashMap<>();
		body.put("text", value);
		return JSON.writeValueAsString(Collections.unmodifiableMap(body));
	}

}
