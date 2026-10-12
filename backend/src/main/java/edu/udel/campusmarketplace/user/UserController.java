package edu.udel.campusmarketplace.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Creates the caller's profile row after Firebase has created the account.
	 *
	 * TODO Require an Authorization: Bearer <Firebase ID token> header and pass
	 * the verified token through to the service.
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@RequestBody RegisterRequest request) {
		return userService.register(request);
	}

}
