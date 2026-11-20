import { auth } from "./firebase"

// One place where the frontend talks to our backend.
//
// Every listing, image, and favorites endpoint requires a Firebase ID token in
// an Authorization header (see docs/backend-api.md). Before this file existed
// only authService.ts sent one, so the marketplace pages had no way to reach
// the backend at all and ran on hardcoded sample data instead.

/** A refusal from the backend, carrying its status and explanation. */
export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = "ApiError"
    this.status = status
  }
}

/** Thrown before any request when a call needs a signed-in user and has none. */
export class NotSignedInError extends Error {
  constructor() {
    super("Please log in to continue.")
    this.name = "NotSignedInError"
  }
}

export type ApiRequest = {
  method?: string
  /** Serialized as a JSON body. Omit for GET and for the no-body routes. */
  body?: unknown
  /** Query values; empty, null, and undefined entries are left out. */
  query?: Record<string, string | number | boolean | null | undefined>
  signal?: AbortSignal
  /** For the public reference endpoints, which take no token. */
  anonymous?: boolean
}

export async function apiFetch<T>(path: string, request: ApiRequest = {}): Promise<T> {
  const url = path + buildQuery(request.query)

  let response = await send(url, request, false)

  if (!request.anonymous && shouldRetryWithFreshToken(response.status)) {
    // Exactly one retry, with a newly minted token. Retrying further would
    // only repeat a refusal the backend means.
    response = await send(url, request, true)
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  // The favorites routes answer 204 with no body, so there is nothing to parse.
  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

function shouldRetryWithFreshToken(status: number): boolean {
  // 401 is the backend saying the token itself was missing, malformed,
  // expired, or invalid. Firebase normally refreshes a token before it
  // expires, but a suspended laptop or a skewed clock can still produce one
  // the server rejects, and that is worth a single forced refresh.
  if (status === 401) {
    return true
  }

  // 403 on a write can mean the token's email_verified claim is stale rather
  // than that the user is unverified: Firebase does not reissue a token the
  // moment someone clicks the verification link, so a user who just verified
  // in another tab still carries the old claim. If this browser already knows
  // the account is verified, the claim is what is behind, so refresh and retry.
  if (status === 403) {
    return auth.currentUser?.emailVerified === true
  }

  return false
}

async function send(url: string, request: ApiRequest, forceRefresh: boolean): Promise<Response> {
  const headers: Record<string, string> = {}

  if (!request.anonymous) {
    const user = auth.currentUser

    if (!user) {
      throw new NotSignedInError()
    }

    headers.Authorization = "Bearer " + (await user.getIdToken(forceRefresh))
  }

  if (request.body !== undefined) {
    headers["Content-Type"] = "application/json"
  }

  return fetch(url, {
    method: request.method ?? "GET",
    headers,
    body: request.body === undefined ? undefined : JSON.stringify(request.body),
    signal: request.signal,
  })
}

function buildQuery(query: ApiRequest["query"]): string {
  if (!query) {
    return ""
  }

  const params = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    // An absent filter and a filter set to "" mean the same thing here: do not
    // constrain this field. Sending the empty string would be a real value.
    if (value === undefined || value === null || value === "") {
      continue
    }

    params.set(key, String(value))
  }

  const encoded = params.toString()
  return encoded ? "?" + encoded : ""
}

async function toApiError(response: Response): Promise<ApiError> {
  let message = ""

  try {
    const body = await response.json()

    if (body && typeof body.message === "string") {
      message = body.message
    }
  } catch {
    // Not every failure arrives as the documented {"message": "..."} body. A
    // dropped backend or the dev proxy answers with HTML or nothing, and that
    // should still reach the user as a readable error rather than a parse crash.
  }

  return new ApiError(response.status, message || fallbackMessage(response.status))
}

function fallbackMessage(status: number): string {
  if (status === 401) return "Your session has expired. Please log in again."
  if (status === 403) return "You do not have permission to do that."
  if (status === 404) return "That item is no longer available."
  if (status === 409) return "That item has changed. Refresh and try again."
  if (status === 503) return "The marketplace is temporarily unavailable. Please try again soon."
  return "Something went wrong. Please try again."
}

/** True when an error means the session is gone and the user should log in. */
export function isSessionExpired(error: unknown): boolean {
  return error instanceof NotSignedInError || (error instanceof ApiError && error.status === 401)
}

/** The message to show a user for any error this module can produce. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof NotSignedInError) {
    return error.message
  }

  // A rejected fetch with no response at all is almost always the backend not
  // running, which during development is the single most common cause.
  return "Could not reach the marketplace. Check your connection and try again."
}

/** True when a request was cancelled by us, which callers should ignore. */
export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError"
}
