package edu.udel.campusmarketplace.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.udel.campusmarketplace.auth.VerifiedUser;

/** Resolves a trusted identity to an eligible marketplace profile. */
@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User require(VerifiedUser identity, boolean writing) {
        if (writing && !identity.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Verify your email before changing marketplace data.");
        }
        User user = users.findByFirebaseUid(identity.firebaseUid()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Register a marketplace profile first."));
        if (identity.email() == null || !user.email().equalsIgnoreCase(identity.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The token email does not match your profile.");
        }
        String email = identity.email();
        int at = email.lastIndexOf('@');
        UUID university = users.findUniversityIdByEmailDomain(at < 0 ? "" : email.substring(at + 1)).orElse(null);
        if (!user.universityId().equals(university)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A participating university profile is required.");
        }
        return user;
    }
}
