package edu.udel.campusmarketplace.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Header parsing only. These cases are rejected before the Firebase Admin SDK
 * is touched, so they need no credentials and make no network call.
 */
class FirebaseTokenVerifierTests {

	private final FirebaseTokenVerifier verifier = new FirebaseTokenVerifier();

	@Test
	void rejectsAMissingHeader() {
		assertUnauthorized(null);
	}

	@Test
	void rejectsAHeaderWithoutTheBearerScheme() {
		assertUnauthorized("abc123");
	}

	@Test
	void rejectsBasicAuthentication() {
		assertUnauthorized("Basic dXNlcjpwYXNz");
	}

	@Test
	void rejectsABearerHeaderWithNoToken() {
		assertUnauthorized("Bearer    ");
	}

	private void assertUnauthorized(String authorizationHeader) {
		assertThatThrownBy(() -> verifier.verify(authorizationHeader))
				.isInstanceOf(ResponseStatusException.class)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories
						.type(ResponseStatusException.class))
				.satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

}
