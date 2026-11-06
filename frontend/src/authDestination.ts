// Carry the return route through signup and email verification, without external redirects.
export function authLink(path: "/login" | "/signup") {
  const next = new URLSearchParams(window.location.search).get("next")
  if (!next?.startsWith("/")) return path
  try {
    const url = new URL(next, window.location.origin)
    if (url.origin !== window.location.origin) return path
    return `${path}?next=${encodeURIComponent(url.pathname + url.search + url.hash)}`
  } catch {
    return path
  }
}
