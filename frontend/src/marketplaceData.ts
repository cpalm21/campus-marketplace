import { apiFetch } from "./apiClient"

// Reads the marketplace from the backend. This module used to export a
// hardcoded array of sample listings; every page that imported it was showing
// invented data. The shapes below mirror docs/backend-api.md.

/** A listing as the marketplace pages display it. */
export type Listing = {
  id: string
  title: string
  price: number
  /** Resolved from categoryId, because the backend sends only the id. */
  category: string
  condition: string
  location: string
  image: string
  description: string
  status: ListingStatus
  createdAt: string
}

export type ListingStatus = "Available" | "Reserved" | "Sold"

/** The backend's listing response. */
type ListingResponse = {
  listingId: string
  categoryId: number
  title: string
  price: number
  condition: string
  description: string | null
  location: string | null
  status: ListingStatus
  createdAt: string
}

type CategoryResponse = { categoryId: number; name: string }

type ListingImageResponse = { listingImageId: string; imageUrl: string; imageOrder: number }

type CursorPage<T> = { items: T[]; nextCursor: string | null }

type FavoriteResponse = { listing: ListingResponse; savedAt: string }

// Shown when a listing has no image metadata yet. Inline so an empty grid
// never depends on the network or an outside host.
const PLACEHOLDER_IMAGE =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 300'%3E%3Crect width='400' height='300' fill='%23e9ebe7'/%3E%3Cpath d='M150 170l40-45 35 40 25-25 45 55H150z' fill='%23c7cdc4'/%3E%3Ccircle cx='160' cy='120' r='16' fill='%23c7cdc4'/%3E%3C/svg%3E"

/** The exact condition values the backend accepts. */
export const LISTING_CONDITIONS = ["New", "Great", "Good", "Fair", "Poor"]

/** Sort options, paired with the values the backend's sort parameter takes. */
export const LISTING_SORTS = [
  { label: "Newest", value: "newest" },
  { label: "Price: low to high", value: "priceAsc" },
  { label: "Price: high to low", value: "priceDesc" },
]

// Categories are small, public, and change rarely, so the app fetches them
// once per page load and every listing reuses the result.
let categoryRequest: Promise<CategoryResponse[]> | null = null

export function loadCategories(): Promise<CategoryResponse[]> {
  if (!categoryRequest) {
    categoryRequest = apiFetch<CategoryResponse[]>("/api/categories", { anonymous: true })
      // A failed lookup must not be cached, or the whole session is stuck with
      // listings that cannot name their category.
      .catch((error) => {
        categoryRequest = null
        throw error
      })
  }

  return categoryRequest
}

async function categoryNames(): Promise<Map<number, string>> {
  const categories = await loadCategories()
  return new Map(categories.map((category) => [category.categoryId, category.name]))
}

function toListing(
  response: ListingResponse,
  names: Map<number, string>,
  image: string,
): Listing {
  return {
    id: response.listingId,
    title: response.title,
    price: response.price,
    category: names.get(response.categoryId) ?? "Other",
    condition: response.condition,
    // The backend stores these as optional and sends null when blank, but the
    // cards and detail page render them as plain text.
    location: response.location ?? "Campus",
    image,
    description: response.description ?? "",
    status: response.status,
    createdAt: response.createdAt,
  }
}

export async function loadListingImages(
  listingId: string,
  signal?: AbortSignal,
): Promise<string[]> {
  const images = await apiFetch<ListingImageResponse[]>(
    "/api/listings/" + encodeURIComponent(listingId) + "/images",
    { signal },
  )

  return [...images]
    .sort((a, b) => a.imageOrder - b.imageOrder)
    .map((image) => image.imageUrl)
}

// Image metadata lives behind its own endpoint, so filling a grid costs one
// request per listing on top of the search itself. At a page size of 20 that
// is acceptable, and the requests run in parallel, but the real fix is for the
// listing response to carry its cover URL. Worth raising with Sean rather than
// working around here.
async function attachCovers(
  items: ListingResponse[],
  names: Map<number, string>,
  signal?: AbortSignal,
): Promise<Listing[]> {
  return Promise.all(
    items.map(async (item) => {
      let cover = PLACEHOLDER_IMAGE

      try {
        const images = await loadListingImages(item.listingId, signal)

        if (images.length > 0) {
          cover = images[0]
        }
      } catch {
        // A listing whose images cannot be read is still a listing. Dropping
        // it from the grid over a missing picture would hide a real item.
      }

      return toListing(item, names, cover)
    }),
  )
}

export type ListingQuery = {
  q?: string
  categoryId?: number
  condition?: string
  minPrice?: number
  maxPrice?: number
  sort?: string
  limit?: number
  cursor?: string
}

export type ListingPage = { items: Listing[]; nextCursor: string | null }

/** Searches the signed-in user's university listings. */
export async function searchListings(
  query: ListingQuery = {},
  signal?: AbortSignal,
): Promise<ListingPage> {
  const names = await categoryNames()

  const page = await apiFetch<CursorPage<ListingResponse>>("/api/listings", {
    query: {
      q: query.q,
      categoryId: query.categoryId,
      condition: query.condition,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
      sort: query.sort,
      limit: query.limit,
      cursor: query.cursor,
    },
    signal,
  })

  return { items: await attachCovers(page.items, names, signal), nextCursor: page.nextCursor }
}

/** Reads one listing with all of its images, cover first. */
export async function loadListing(
  listingId: string,
  signal?: AbortSignal,
): Promise<{ listing: Listing; images: string[] }> {
  const [names, response] = await Promise.all([
    categoryNames(),
    apiFetch<ListingResponse>("/api/listings/" + encodeURIComponent(listingId), { signal }),
  ])

  let images: string[] = []

  try {
    images = await loadListingImages(listingId, signal)
  } catch {
    // Same reasoning as the grid: show the item without its photos.
  }

  return {
    listing: toListing(response, names, images[0] ?? PLACEHOLDER_IMAGE),
    images,
  }
}

// Saved listings now live in the database rather than this browser's
// localStorage, so a user's shortlist follows them to another device and the
// backend can enforce who is allowed to save what.

const FAVORITE_PAGE_LIMIT = 100
const MAX_FAVORITE_PAGES = 10

async function loadFavoriteResponses(signal?: AbortSignal): Promise<FavoriteResponse[]> {
  const all: FavoriteResponse[] = []
  let cursor: string | null = null

  // Bounded rather than an open loop: a cursor that never resolves to null
  // would otherwise spin forever against a live feed.
  for (let page = 0; page < MAX_FAVORITE_PAGES; page++) {
    const result: CursorPage<FavoriteResponse> = await apiFetch<CursorPage<FavoriteResponse>>(
      "/api/users/me/favorites",
      { query: { limit: FAVORITE_PAGE_LIMIT, cursor }, signal },
    )

    all.push(...result.items)
    cursor = result.nextCursor

    if (!cursor) {
      break
    }
  }

  return all
}

/** The user's saved listings, newest save first. */
export async function loadSavedListings(signal?: AbortSignal): Promise<Listing[]> {
  const [names, favorites] = await Promise.all([
    categoryNames(),
    loadFavoriteResponses(signal),
  ])

  return attachCovers(
    favorites.map((favorite) => favorite.listing),
    names,
    signal,
  )
}

/** Just the ids, for deciding which hearts on a grid are filled in. */
export async function loadSavedListingIds(signal?: AbortSignal): Promise<Set<string>> {
  const favorites = await loadFavoriteResponses(signal)
  return new Set(favorites.map((favorite) => favorite.listing.listingId))
}

export async function saveListing(listingId: string): Promise<void> {
  await apiFetch<void>("/api/users/me/favorites/" + encodeURIComponent(listingId), {
    method: "PUT",
  })
}

export async function removeSavedListing(listingId: string): Promise<void> {
  await apiFetch<void>("/api/users/me/favorites/" + encodeURIComponent(listingId), {
    method: "DELETE",
  })
}
