import { createContext, useContext } from "react"
import type { User } from "firebase/auth"

// The single definition of "signed in" for the whole app.
//
// Both halves matter. emailVerified alone would let someone who signed up but
// never opened the verification link act like a member, and the domain test
// alone would trust an address nobody has proven they can read. Keeping the
// rule in one exported function is the point: the route guard and the page
// header cannot drift apart the way they did when each had its own copy.
//
// This mirrors, and must stay in step with, the server-side rule. The browser
// check is only fast feedback; UserService re-derives the university from the
// verified token on every call, because anyone can call the API directly.
export function isSignedIn(user: User | null): boolean {
  if (!user) return false
  return user.emailVerified && /^[^\s@]+@udel\.edu$/i.test(user.email ?? "")
}

export type AuthContextValue = {
  /** The raw Firebase user, or null. Present even when not yet eligible. */
  user: User | null
  /** True until Firebase has finished restoring any stored session. */
  loading: boolean
  /** Whether this user may act as a member. See {@link isSignedIn}. */
  signedIn: boolean
  signOut: () => Promise<void>
}

// No default value, so a component rendered outside the provider fails loudly
// in useAuth instead of silently reading a "signed out" placeholder.
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)

  if (!value) {
    throw new Error("useAuth must be called inside <AuthProvider>.")
  }

  return value
}
