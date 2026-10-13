package edu.udel.campusmarketplace.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Registration logic.
 *
 * The endpoint contract is settled; persistence is not. Whoever wires up the
 * database replaces the body of {@link #register} and leaves the signature
 * alone, so the controller and any frontend code keep working untouched.
 */
@Service
public class UserService {

	/** Only this university's accounts may register for now. */
	private static final String ALLOWED_EMAIL_DOMAIN = "udel.edu";

	/**
	 * Returns the caller's profile, creating it on first call.
	 *
	 * Takes a {@link VerifiedUser} rather than an email and UID so an
	 * unverified identity cannot reach this method by mistake.
	 *
	 * The email domain is checked here and not only in the browser: the
	 * frontend check is a courtesy that gives fast feedback, but anyone can
	 * call this endpoint directly, so the rule has to be enforced server side
	 * to mean anything.
	 *
	 * TODO Derive university_id from the email domain, then look the row up by
	 * firebase_uid and insert it when absent. Return the database-generated
	 * user_id once there is one.
	 */
	public UserResponse register(VerifiedUser verifiedUser) {
		String email = verifiedUser.email();

		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "The Firebase account has no email address.");
		}

		if (!hasAllowedDomain(email)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Registration is limited to " + ALLOWED_EMAIL_DOMAIN + " email addresses.");
		}

		// Note that emailVerified is deliberately not required. Registration
		// runs immediately after signup, before the user has opened the
		// verification link, so demanding it here would reject every new
		// account. Gate listing and messaging on it instead.

		return new UserResponse(verifiedUser.firebaseUid(), email);
	}

	private static boolean hasAllowedDomain(String email) {
		int at = email.lastIndexOf('@');
		return at >= 0 && email.substring(at + 1).equalsIgnoreCase(ALLOWED_EMAIL_DOMAIN);
	}

}
