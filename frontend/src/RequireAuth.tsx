import type { ReactNode } from "react"
import { Navigate, useLocation } from "react-router"
import { useAuth } from "./authContext"

export default function RequireAuth({ children }: { children: ReactNode }) {
  // Session state comes from the one app-wide subscription in AuthProvider.
  // This component used to run its own onAuthStateChanged listener and keep a
  // second copy of the "is this user allowed" rule, which is how it and the
  // page header ended up disagreeing. Both now read the same source.
  const { loading, signedIn } = useAuth()
  const location = useLocation()

  // Render nothing while the stored session is still being restored. Deciding
  // during this gap would bounce an already-signed-in user to /login on every
  // refresh. Intentionally blank rather than a spinner: what belongs here is a
  // visual call for Yaz and Mia, not something to invent in the auth layer.
  if (loading) return null

  if (!signedIn) {
    // Carry the attempted destination so login can return the user to it.
    const next = location.pathname + location.search + location.hash
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />
  }

  return <>{children}</>
}
