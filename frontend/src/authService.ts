import {
    createUserWithEmailAndPassword,
    sendEmailVerification,
} from "firebase/auth";
import type { User } from "firebase/auth";
import { auth } from "./firebase";

// Mirrors the backend's UserResponse record.
type UserResponse = {
    firebaseUid: string;
    email: string;
};

// Creates the caller's profile row in our backend.
//
// There is no request body: the ID token goes in the Authorization header and
// the backend reads the UID and email out of it, so the identity is proven
// rather than claimed.
async function registerProfile(user: User): Promise<void> {
    const token = await user.getIdToken();

    const response = await fetch("/api/users/register", {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });

    if (!response.ok) {
        throw new Error(
            `Profile registration failed with status ${response.status}.`
        );
    }

    const profile: UserResponse = await response.json();

    console.info("Backend verified registration for", profile.firebaseUid);
}

// Validates inputs, creates the Firebase account, and sends verification email.
export async function signUp(
    email: string,
    password: string,
    confirmPassword: string
): Promise<User> {
    const normalizedEmail = email.trim();
    const emailParts = normalizedEmail.split("@");

    if (
        emailParts.length !== 2 ||
        emailParts[1].toLowerCase() !== "udel.edu"
    ) {
        throw new Error("Please use your University of Delaware email.");
    }

    if (password !== confirmPassword) {
        throw new Error("Passwords do not match.");
    }

    const result = await createUserWithEmailAndPassword(
        auth,
        normalizedEmail,
        password
    );

    await sendEmailVerification(result.user);

    // Deliberately non-fatal. The Firebase account already exists by this
    // point, so throwing here would report "signup failed" for an account the
    // user cannot create again — the email is taken. Registration is
    // idempotent, so a later sign-in can create the missing profile row.
    try {
        await registerProfile(result.user);
    } catch (err) {
        console.warn("Could not create backend profile for new user.", err);
    }

    return result.user;
}