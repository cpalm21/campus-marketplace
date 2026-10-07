import {
    createUserWithEmailAndPassword,
    sendEmailVerification,
} from "firebase/auth";
import type { User } from "firebase/auth";
import { auth } from "./firebase";

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

    return result.user;
}