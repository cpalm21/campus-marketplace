package edu.udel.campusmarketplace.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;

/**
 * Registration logic.
 */
@Service
public class UserService {

	private final UserRepository userRepository;

	UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Returns the caller's profile, creating it on first call.
	 *
	 * Takes a {@link VerifiedUser} rather than an email and UID so an
	 * unverified identity cannot reach this method by mistake.
	 *
	 * Which universities may register is data, not code: an email domain that
	 * no universities row claims is rejected. Adding a school is an insert,
	 * and so is revoking one. The matching check in the browser only gives
	 * fast feedback, since anyone can call this endpoint directly.
	 *
	 * Idempotent by design. The frontend calls it after signup and should also
	 * call it on login, so a user whose first call failed still gets a row on
	 * the next sign-in. Calling it twice must never create two rows.
	 */
	public UserResponse register(VerifiedUser verifiedUser) {
		String email = verifiedUser.email();

		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "The Firebase account has no email address.");
		}

		// Looked up before the user, so the rule is enforced on every call
		// rather than only on the one that inserts. It costs a query per
		// login, and it means a school that is removed stops being accepted
		// immediately rather than only for new accounts.
		UUID universityId = userRepository.findUniversityIdByEmailDomain(domainOf(email))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Registration is limited to participating universities."));

		// Note that emailVerified is deliberately not required. Registration
		// runs immediately after signup, before the user has opened the
		// verification link, so demanding it here would reject every new
		// account. Gate listing and messaging on it instead.

		User user = userRepository.findByFirebaseUid(verifiedUser.firebaseUid())
				.orElseGet(() -> userRepository.insert(
						new NewUser(verifiedUser.firebaseUid(), email, universityId)));

		// TODO Now that a row exists, user_id is what the frontend will need
		// as the foreign key for listings and favourites, so UserResponse
		// should probably grow it. Left out here because it changes the
		// frontend contract.
		return new UserResponse(user.firebaseUid(), user.email());
	}

	/** The part after the last '@', or "" when there is no '@'. */
	private static String domainOf(String email) {
		int at = email.lastIndexOf('@');
		return at < 0 ? "" : email.substring(at + 1);
	}

}
