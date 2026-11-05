package edu.udel.campusmarketplace.auth;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

/**
 * Verifies Firebase ID tokens with the Firebase Admin SDK.
 *
 * Credentials come from the environment, not from this repository: set
 * GOOGLE_APPLICATION_CREDENTIALS to the path of a service account key
 * downloaded from the Firebase console. The key must never be committed.
 */
@Component
public class FirebaseTokenVerifier implements TokenVerifier {

	private static final String BEARER_PREFIX = "Bearer ";

	/**
	 * Resolved on first use rather than at startup, so the application still
	 * boots for anyone working on unrelated endpoints without credentials
	 * configured. They get a clear 503 from this endpoint instead of a server
	 * that refuses to start.
	 */
	private volatile FirebaseAuth firebaseAuth;

	@Override
	public VerifiedUser verify(String authorizationHeader) {
		String idToken = extractBearerToken(authorizationHeader);

		FirebaseToken token;
		try {
			token = firebaseAuth().verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			// Covers an expired, revoked, malformed, or wrong-project token.
			// The reason is deliberately vague to the caller and detailed only
			// in the cause, so a client cannot probe for valid token shapes.
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED, "Could not verify the Firebase ID token.", e);
		}

		return new VerifiedUser(token.getUid(), token.getEmail(), token.isEmailVerified());
	}

	private static String extractBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Expected an Authorization: Bearer <Firebase ID token> header.");
		}

		String idToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (idToken.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED, "The Authorization header carried no token.");
		}

		return idToken;
	}

	private FirebaseAuth firebaseAuth() {
		FirebaseAuth resolved = firebaseAuth;
		if (resolved != null) {
			return resolved;
		}

		synchronized (this) {
			if (firebaseAuth == null) {
				firebaseAuth = FirebaseAuth.getInstance(firebaseApp());
			}
			return firebaseAuth;
		}
	}

	private static FirebaseApp firebaseApp() {
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}

		try {
			return FirebaseApp.initializeApp(FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.getApplicationDefault())
					.build());
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Firebase credentials are not configured. Set GOOGLE_APPLICATION_CREDENTIALS "
							+ "to a service account key file.",
					e);
		}
	}

}
