package edu.udel.campusmarketplace.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registration logic.
 *
 * The endpoint contract is settled; persistence is not. Whoever wires up the
 * database replaces the body of {@link #register} and leaves the signature
 * alone, so the controller and any frontend code keep working untouched.
 */
@Service
public class UserService {

	/**
	 * Returns the caller's profile, creating it on first call.
	 *
	 * TODO Take the verified Firebase UID and email from the ID token, derive
	 * university_id from the email domain, then look the row up by
	 * firebase_uid and insert it when absent.
	 */
	public UserResponse register(RegisterRequest request) {
		throw new ResponseStatusException(
				HttpStatus.NOT_IMPLEMENTED, "User registration is not wired up yet.");
	}

}
