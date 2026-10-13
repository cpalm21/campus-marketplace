package edu.udel.campusmarketplace.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.udel.campusmarketplace.auth.TokenVerifier;
import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Covers the controller and the service checks behind it. The token verifier
 * is mocked so these run without service account credentials; header parsing
 * and token verification are covered in FirebaseTokenVerifierTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenVerifier tokenVerifier;

	@Test
	void registerReturnsTheIdentityFromTheToken() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser("firebase-uid-123", "palmerc@udel.edu", false));

		mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firebaseUid").value("firebase-uid-123"))
				.andExpect(jsonPath("$.email").value("palmerc@udel.edu"));
	}

	/**
	 * An unverified email must still register: signup happens before the user
	 * opens the verification link.
	 */
	@Test
	void registerAcceptsAnUnverifiedEmail() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser("firebase-uid-123", "palmerc@udel.edu", false));

		mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"))
				.andExpect(status().isOk());
	}

	@Test
	void registerRejectsEmailOutsideTheUniversityDomain() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser("firebase-uid-456", "someone@gmail.com", true));

		mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"))
				.andExpect(status().isForbidden());
	}

	/**
	 * A lookalike domain must not pass: the check compares the part after the
	 * last @, not a suffix.
	 */
	@Test
	void registerRejectsALookalikeDomain() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser("firebase-uid-789", "attacker@notudel.edu", true));

		mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registerRejectsATokenCarryingNoEmail() throws Exception {
		when(tokenVerifier.verify(any()))
				.thenReturn(new VerifiedUser("firebase-uid-000", null, false));

		mockMvc.perform(post("/api/users/register")
				.header(HttpHeaders.AUTHORIZATION, "Bearer any-token"))
				.andExpect(status().isBadRequest());
	}

}
