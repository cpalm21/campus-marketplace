import {
    createUserWithEmailAndPassword,
    sendEmailVerification,
    sendPasswordResetEmail,
    signInWithEmailAndPassword,
    signOut,
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
            Authorization: "Bearer " + token,
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
export function getAuthErrorMessage(error: unknown): string {
    const code = error instanceof Error && "code" in error
        ? String(error.code)
        : "";

    switch (code) {
        case "auth/user-not-found":
            return "Account not found.";
        case "auth/wrong-password":
        case "auth/invalid-password":
            return "Incorrect password.";
        case "auth/invalid-credential":
            return "Incorrect email or password.";
        case "auth/invalid-email":
            return "Enter a valid University of Delaware email.";
        case "auth/too-many-requests":
            return "Too many attempts. Please try again later.";
        case "auth/network-request-failed":
            return "Network error. Check your connection and try again.";
        default:
            return error instanceof Error ? error.message : "Authentication failed. Please try again.";
    }
}

export async function signIn(email: string, password: string): Promise<User> {
    const result = await signInWithEmailAndPassword(auth, email.trim(), password);

    if (!result.user.emailVerified) {
        await signOut(auth);
        throw new Error("Verify your email before logging in. Check your UD inbox.");
    }

    return result.user;
}

export async function resetPassword(email: string): Promise<void> {
    await sendPasswordResetEmail(auth, email.trim());
}