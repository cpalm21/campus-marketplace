package edu.udel.campusmarketplace.auth;

/**
 * Turns an Authorization header into a trusted identity.
 *
 * An interface rather than a concrete class so controllers and services depend
 * on the check instead of on Firebase, and so tests can supply an identity
 * without a network call or service account credentials.
 */
public interface TokenVerifier {

	/**
	 * Verifies the bearer token in the given Authorization header value.
	 *
	 * @param authorizationHeader the raw header value, or null when absent
	 * @return the identity the token proves
	 * @throws org.springframework.web.server.ResponseStatusException 401 when
	 *     the header is missing, malformed, or holds a token that does not
	 *     verify
	 */
	VerifiedUser verify(String authorizationHeader);

}
