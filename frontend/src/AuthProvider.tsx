import { useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"
import { onAuthStateChanged, signOut as firebaseSignOut } from "firebase/auth"
import type { User } from "firebase/auth"
import { auth } from "./firebase"
import { AuthContext, isSignedIn } from "./authContext"
import type { AuthContextValue } from "./authContext"

// Holds the app's session state. The context, the useAuth hook and the
// isSignedIn rule live in authContext.ts so this file exports only a
// component, which is what keeps Fast Refresh working.
export function AuthProvider({ children }: { children: ReactNode }) {
  // loading starts true because Firebase restores the session from IndexedDB
  // asynchronously. Treating that gap as "signed out" is the bug this file
  // exists to fix: it made the account header show the logged-out state on
  // every page load and never recover, since auth.currentUser is a plain
  // value that triggers no re-render when it resolves.
  const [session, setSession] = useState<{ loading: boolean; user: User | null }>({
    loading: true,
    user: null,
  })

  useEffect(
    () =>
      // onAuthStateChanged returns its own unsubscribe function, which is what
      // this effect hands back as cleanup. One subscription for the app.
      onAuthStateChanged(
        auth,
        (user) => setSession({ loading: false, user }),
        // An auth error is not a reason to hang on the loading state forever.
        // Fail closed: resolve as nobody signed in.
        () => setSession({ loading: false, user: null }),
      ),
    [],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session.user,
      loading: session.loading,
      signedIn: isSignedIn(session.user),
      // Wrapped so callers never need to import the Firebase auth instance,
      // and so the app has exactly one way to end a session.
      signOut: () => firebaseSignOut(auth),
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
