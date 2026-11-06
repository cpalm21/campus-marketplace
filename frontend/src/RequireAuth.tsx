import { useEffect, useState } from "react"
import type { ReactNode } from "react"
import { onAuthStateChanged } from "firebase/auth"
import type { User } from "firebase/auth"
import { Navigate, useLocation } from "react-router"
import { auth } from "./firebase"

export default function RequireAuth({ children }: { children: ReactNode }) {
  // Wait for Firebase to restore the session before choosing a destination.
  const [session, setSession] = useState<{ ready: boolean; user: User | null }>({ ready: false, user: null })
  const location = useLocation()
  useEffect(() => onAuthStateChanged(auth,
    user => setSession({ ready: true, user }),
    () => setSession({ ready: true, user: null }),
  ), [])
  if (!session.ready) return null
  const email = session.user?.email || ""
  if (!session.user?.emailVerified || !/^[^\s@]+@udel\.edu$/i.test(email)) {
    const next = location.pathname + location.search + location.hash
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />
  }
  return <>{children}</>
}
