package edu.udel.campusmarketplace.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Covers the controller and the service rules behind it. The token verifier
 * and the repository are mocked, so these run without Firebase credentials and
 * without a database; header parsing is covered in FirebaseTokenVerifierTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {

	private static final String FIREBASE_UID = "firebase-uid-123";
	private static final String UD_EMAIL = "palmerc@udel.edu";
	private static final UUID UD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@MockitoBean
	private UserRepository userRepository;

	@BeforeEach
	void onlyUdelIsAParticipatingUniversity() {
		when(userRepository.findUniversityIdByEmailDomain(any())).thenReturn(Optional.empty());
		when(userRepository.findUniversityIdByEmailDomain("udel.edu")).thenReturn(Optional.of(UD_ID));
	}

	private void callerIs(String firebaseUid, String email, boolean emailVerified) {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser(firebaseUid, email, emailVerified));
	}

	private static User storedUser() {
		return new User(USER_ID, FIREBASE_UID, UD_EMAIL, UD_ID, Instant.now());
	}

	private ResultActions register() throws Exception {
		return mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"));
	}

	@Test
	void registerCreatesTheUserOnFirstCall() throws Exception {
		callerIs(FIREBASE_UID, UD_EMAIL, false);
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.empty());
		when(userRepository.insert(any())).thenReturn(storedUser());

		register()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firebaseUid").value(FIREBASE_UID))
				.andExpect(jsonPath("$.email").value(UD_EMAIL));

		verify(userRepository).insert(new NewUser(FIREBASE_UID, UD_EMAIL, UD_ID));
	}

	/**
	 * The frontend calls this after signup and should call it again on login,
	 * so a second call must return the existing row rather than insert twice.
	 */
	@Test
	void registerIsIdempotentForAnAlreadyRegisteredUser() throws Exception {
		callerIs(FIREBASE_UID, UD_EMAIL, true);
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(storedUser()));

		register()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firebaseUid").value(FIREBASE_UID));

		verify(userRepository, never()).insert(any());
	}

	/**
	 * An unverified email must still register: signup happens before the user
	 * opens the verification link.
	 */
	@Test
	void registerAcceptsAnUnverifiedEmail() throws Exception {
		callerIs(FIREBASE_UID, UD_EMAIL, false);
		when(userRepository.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.empty());
		when(userRepository.insert(any())).thenReturn(storedUser());

		register().andExpect(status().isOk());
	}

	@Test
	void registerRejectsADomainNoUniversityClaims() throws Exception {
		callerIs("firebase-uid-456", "someone@gmail.com", true);

		register().andExpect(status().isForbidden());

		verify(userRepository, never()).insert(any());
	}

	/**
	 * A lookalike domain must not pass: the lookup uses the part after the
	 * last '@', not a suffix match.
	 */
	@Test
	void registerRejectsALookalikeDomain() throws Exception {
		callerIs("firebase-uid-789", "attacker@notudel.edu", true);

		register().andExpect(status().isForbidden());

		verify(userRepository, never()).insert(any());
	}

	@Test
	void registerRejectsATokenCarryingNoEmail() throws Exception {
		callerIs("firebase-uid-000", null, false);

		register().andExpect(status().isBadRequest());

		verify(userRepository, never()).insert(any());
	}

}
