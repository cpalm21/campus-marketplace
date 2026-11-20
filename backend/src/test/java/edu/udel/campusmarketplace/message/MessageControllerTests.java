package edu.udel.campusmarketplace.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import edu.udel.campusmarketplace.listing.Listing;
import edu.udel.campusmarketplace.listing.ListingRepository;
import edu.udel.campusmarketplace.user.User;
import edu.udel.campusmarketplace.user.UserRepository;

/**
 * Covers the controller and the service rules behind it. The token verifier and
 * the repositories are mocked, so these run without Firebase credentials and
 * without a database; header parsing is covered in FirebaseTokenVerifierTests
 * and the SQL itself needs integration tests.
 *
 * Follows ListingControllerTests, including its habit of asserting on what
 * would have been stored rather than only on the status code.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTests {

	private static final String FIREBASE_UID = "firebase-uid-123";
	private static final String EMAIL = "palmerc@udel.edu";
	private static final UUID UNIVERSITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID SENDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID LISTING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID SELLER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	/**
	 * Built here rather than autowired: this application exposes no
	 * ObjectMapper bean, and these tests only need one to escape a body.
	 */
	private static final ObjectMapper JSON = new ObjectMapper();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@MockitoBean
	private MessageRepository messageRepository;

	@MockitoBean
	private ListingRepository listingRepository;

	@MockitoBean
	private UserRepository userRepository;

	@BeforeEach
	void theCallerIsRegisteredAndTheListingIsAvailable() {
		when(tokenVerifier.verify(any())).thenReturn(new VerifiedUser(FIREBASE_UID, EMAIL, true));
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(
				new User(SENDER_ID, FIREBASE_UID, EMAIL, UNIVERSITY_ID, Instant.now())));
		when(userRepository.findUniversityIdByEmailDomain("udel.edu")).thenReturn(Optional.of(UNIVERSITY_ID));
		when(listingRepository.find(LISTING_ID, UNIVERSITY_ID, false))
				.thenReturn(Optional.of(listing("Available")));
		when(messageRepository.insert(any())).thenAnswer(call -> stored(call.getArgument(0)));
	}

	/** A listing owned by someone other than the caller. */
	private static Listing listing(String status) {
		return new Listing(LISTING_ID, SELLER_ID, 4, "Solid wood study desk", new BigDecimal("45.00"),
				"Good", "Sturdy.", "Morris Library", status, Instant.now(), Instant.now());
	}

	/** Echoes back what was asked for, as the database's RETURNING would. */
	private static Message stored(NewMessage newMessage) {
		return new Message(UUID.randomUUID(), newMessage.listingId(), newMessage.senderId(),
				newMessage.receiverId(), newMessage.text(), Instant.now(), false);
	}

	@Test
	void sendsAMessage() throws Exception {
		send(text("Is the desk still available?"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.text").value("Is the desk still available?"))
				.andExpect(jsonPath("$.isMine").value(true))
				.andExpect(jsonPath("$.isRead").value(false))
				.andExpect(jsonPath("$.messageId").exists())
				.andExpect(jsonPath("$.sentAt").exists());
	}

	/** Internal user ids never cross the wire, following ListingResponse. */
	@Test
	void doesNotRevealParticipantIds() throws Exception {
		send(text("Hello"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.senderId").doesNotExist())
				.andExpect(jsonPath("$.receiverId").doesNotExist())
				.andExpect(jsonPath("$.listingId").doesNotExist());
	}

	/**
	 * The sender comes from the verified token and the receiver from the
	 * listing's seller. Neither is read from the body, which is the whole
	 * reason SendMessageRequest carries only text.
	 */
	@Test
	void storesTheCallerAsSenderAndTheSellerAsReceiver() throws Exception {
		send(text("Hello")).andExpect(status().isCreated());

		ArgumentCaptor<NewMessage> captor = ArgumentCaptor.forClass(NewMessage.class);
		verify(messageRepository).insert(captor.capture());
		assertThat(captor.getValue().senderId()).isEqualTo(SENDER_ID);
		assertThat(captor.getValue().receiverId()).isEqualTo(SELLER_ID);
		assertThat(captor.getValue().listingId()).isEqualTo(LISTING_ID);
	}

	/** Stored text is trimmed, so padding cannot sneak past the length rule. */
	@Test
	void storesTrimmedText() throws Exception {
		send(text("   Is it still available?\n ")).andExpect(status().isCreated());

		ArgumentCaptor<NewMessage> captor = ArgumentCaptor.forClass(NewMessage.class);
		verify(messageRepository).insert(captor.capture());
		assertThat(captor.getValue().text()).isEqualTo("Is it still available?");
	}

	/**
	 * The limit counts what the student typed, not what Java stores. A
	 * 2,000-character message is accepted whether or not its characters each
	 * happen to fit in one char, which is what codePointCount buys us.
	 */
	@Test
	void sendsAMessageOfExactlyTheMaximumLength() throws Exception {
		send(text("a".repeat(2000))).andExpect(status().isCreated());
	}

	@Test
	void sendsTwoThousandEmojiEvenThoughJavaStoresThemAsFourThousandChars() throws Exception {
		send(text("😀".repeat(2000))).andExpect(status().isCreated());
	}

	@Test
	void rejectsAMessageOverTheMaximumLength() throws Exception {
		send(text("a".repeat(2001)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be longer than 2000 characters."));
		verify(messageRepository, never()).insert(any());
	}

	@Test
	void rejectsAnEmptyMessage() throws Exception {
		send(text("")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A message cannot be empty."));
		verify(messageRepository, never()).insert(any());
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
	 * The field rule is checked before the listing is looked up, so a
	 * malformed request costs no queries.
	 */
	@Test
	void doesNotLookUpTheListingForAnEmptyMessage() throws Exception {
		send(text("")).andExpect(status().isBadRequest());
		verify(listingRepository, never()).find(any(), any(), eq(false));
	}

	@Test
	void rejectsAMessageAboutAListingTheCallerCannotSee() throws Exception {
		when(listingRepository.find(LISTING_ID, UNIVERSITY_ID, false)).thenReturn(Optional.empty());

		send(text("Hello")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Listing not found."));
	}

	/**
	 * messages has CHECK (sender_id <> receiver_id), so this is also the check
	 * that keeps a constraint violation from surfacing as a 409.
	 */
	@Test
	void rejectsMessagingYourOwnListing() throws Exception {
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(
				new User(SELLER_ID, FIREBASE_UID, EMAIL, UNIVERSITY_ID, Instant.now())));

		send(text("Hello")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You cannot message yourself about your own listing."));
		verify(messageRepository, never()).insert(any());
	}

	@Test
	void rejectsAMessageAboutASoldListing() throws Exception {
		when(listingRepository.find(LISTING_ID, UNIVERSITY_ID, false))
				.thenReturn(Optional.of(listing("Sold")));

		send(text("Hello")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("This item has sold, so it can no longer be messaged about."));
	}

	/** Reserved is not terminal: arranging a pickup is exactly the point. */
	@Test
	void sendsAMessageAboutAReservedListing() throws Exception {
		when(listingRepository.find(LISTING_ID, UNIVERSITY_ID, false))
				.thenReturn(Optional.of(listing("Reserved")));

		send(text("When can I pick it up?")).andExpect(status().isCreated());
	}

	/** Sending is a write, so require() asks for a verified email. */
	@Test
	void rejectsASenderWhoseEmailIsNotVerified() throws Exception {
		when(tokenVerifier.verify(any())).thenReturn(new VerifiedUser(FIREBASE_UID, EMAIL, false));

		send(text("Hello")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Verify your email before changing marketplace data."));
	}

	@Test
	void rejectsASenderWithNoMarketplaceProfile() throws Exception {
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.empty());

		send(text("Hello")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Register a marketplace profile first."));
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
