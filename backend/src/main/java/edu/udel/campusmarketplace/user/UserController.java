package edu.udel.campusmarketplace.user;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udel.campusmarketplace.auth.TokenVerifier;

/**
 * User profile endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final TokenVerifier tokenVerifier;

	UserController(UserService userService, TokenVerifier tokenVerifier) {
		this.userService = userService;
		this.tokenVerifier = tokenVerifier;
	}

	/**
	 * Creates the caller's profile row after Firebase has created the account.
	 *
	 * There is no request body: identity comes from the verified ID token, so
	 * a caller cannot register as someone else. The header is optional to
	 * Spring only so a missing one fails as 401 from the verifier rather than
	 * as a generic 400.
	 *
	 * Responds 200, not 201: the service stores nothing yet, so claiming a
	 * resource was created would be a lie the frontend could build on.
	 *
	 * TODO Return 201 once a row is genuinely inserted, and keep 200 for the
	 * idempotent repeat call that finds an existing profile.
	 */
	@PostMapping("/register")
	public UserResponse register(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return userService.register(tokenVerifier.verify(authorization));
	}

}
