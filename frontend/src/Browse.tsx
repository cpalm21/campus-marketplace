import { useCallback, useEffect, useState } from "react"
import type { ReactNode } from "react"
import { useNavigate } from "react-router"
import { ListingCard, MarketplaceLayout } from "./marketplaceComponents"
import { errorMessage, isAbortError, isSessionExpired } from "./apiClient"
import {
  LISTING_CONDITIONS,
  LISTING_SORTS,
  loadCategories,
  loadSavedListingIds,
  removeSavedListing,
  saveListing,
  searchListings,
} from "./marketplaceData"
import type { Listing } from "./marketplaceData"
import "./browse.css"

function Icon({ name, size = 20 }: { name: "leaf" | "search" | "map" | "heart" | "sliders" | "menu" | "arrow"; size?: number }) {
  const paths: Record<typeof name, ReactNode> = {
    leaf: <><path d="M21 3C12 3 5 7 5 14c0 2 1 4 3 5 7 2 12-5 13-16Z" /><path d="M3 21c3-6 7-9 13-12" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    map: <><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.9-8.6a5.5 5.5 0 0 0-.1-7.8z" />,
    sliders: <><path d="M4 7h10M18 7h2M4 17h2M10 17h10" /><circle cx="16" cy="7" r="2" /><circle cx="8" cy="17" r="2" /></>,
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
    arrow: <path d="m5 12h14m-5-5 5 5-5 5" />,
  }
  return <svg aria-hidden="true" fill="none" height={size} viewBox="0 0 24 24" width={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8">{paths[name]}</svg>
}

const ALL_CATEGORIES = "All"
const ALL_CONDITIONS = "All conditions"

export default function Browse() {
  const navigate = useNavigate()

  const [search, setSearch] = useState("")
  // Searching is a backend query now, so the typed value is held back briefly
  // rather than sending a request for every keystroke.
  const [query, setQuery] = useState("")
  const [categories, setCategories] = useState<{ categoryId: number; name: string }[]>([])
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [condition, setCondition] = useState(ALL_CONDITIONS)
  const [sort, setSort] = useState("newest")

  const [listings, setListings] = useState<Listing[]>([])
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState("")
  // Bumped by "Try again" to re-run the search effect. Re-setting a filter to
  // the value it already holds would not, since React skips equal updates.
  const [reloadToken, setReloadToken] = useState(0)

  const [saved, setSaved] = useState<Set<string>>(new Set())
  const [saveError, setSaveError] = useState("")

  // Send the user to log in rather than showing an error they cannot act on.
  const handleError = useCallback(
    (err: unknown, setMessage: (message: string) => void) => {
      if (isAbortError(err)) {
        return
      }

      if (isSessionExpired(err)) {
        navigate("/login?next=" + encodeURIComponent("/browse"), { replace: true })
        return
      }

      setMessage(errorMessage(err))
    },
    [navigate],
  )

  useEffect(() => {
    const timer = setTimeout(() => setQuery(search.trim()), 300)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    loadCategories()
      .then(setCategories)
      // The grid still works without category names, so a failure here only
      // costs the filter chips.
      .catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    setLoading(true)
    setError("")

    searchListings(
      {
        q: query,
        categoryId: categoryId ?? undefined,
        condition: condition === ALL_CONDITIONS ? undefined : condition,
        sort,
      },
      controller.signal,
    )
      .then((page) => {
        setListings(page.items)
        setNextCursor(page.nextCursor)
      })
      .catch((err) => handleError(err, setError))
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [query, categoryId, condition, sort, reloadToken, handleError])

  useEffect(() => {
    const controller = new AbortController()

    loadSavedListingIds(controller.signal)
      .then(setSaved)
      // Hearts default to empty if this fails; saving still works.
      .catch(() => undefined)

    return () => controller.abort()
  }, [])

  function loadMore() {
    if (!nextCursor || loadingMore) {
      return
    }

    setLoadingMore(true)

    searchListings({
      q: query,
      categoryId: categoryId ?? undefined,
      condition: condition === ALL_CONDITIONS ? undefined : condition,
      sort,
      cursor: nextCursor,
    })
      .then((page) => {
        setListings((current) => [...current, ...page.items])
        setNextCursor(page.nextCursor)
      })
      .catch((err) => handleError(err, setError))
      .finally(() => setLoadingMore(false))
  }

  async function toggleSaved(id: string) {
    const wasSaved = saved.has(id)

    // Update the heart immediately; the request usually succeeds, and waiting
    // on a round trip makes the button feel broken.
    setSaved((current) => {
      const next = new Set(current)

      if (wasSaved) {
        next.delete(id)
      } else {
        next.add(id)
      }

      return next
    })
    setSaveError("")

    try {
      if (wasSaved) {
        await removeSavedListing(id)
      } else {
        await saveListing(id)
      }
    } catch (err) {
      // Put the heart back. The backend refuses some saves on purpose: your
      // own listing is 403 and an item that has sold is 409, and the user
      // should see why rather than a heart that silently reverts.
      setSaved((current) => {
        const next = new Set(current)

        if (wasSaved) {
          next.add(id)
        } else {
          next.delete(id)
        }

        return next
      })
      handleError(err, setSaveError)
    }
  }

  function clearFilters() {
    setSearch("")
    setCategoryId(null)
    setCondition(ALL_CONDITIONS)
  }

  return (
    <MarketplaceLayout active="browse">
      <main>
        <section className="browse-intro">
          <div className="browse-container">
            <span className="browse-eyebrow">Campus marketplace</span>
            <h1>Find your next campus essential.</h1>
            <div className="browse-search"><Icon name="search" /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search textbooks, furniture, electronics..." aria-label="Search marketplace" /></div>
          </div>
        </section>

        <section className="browse-results">
          <div className="browse-container">
            <div className="category-row" aria-label="Listing categories">
              <button className={categoryId === null ? "category-chip active" : "category-chip"} type="button" onClick={() => setCategoryId(null)}>{ALL_CATEGORIES}</button>
              {categories.map((item) => <button className={categoryId === item.categoryId ? "category-chip active" : "category-chip"} type="button" key={item.categoryId} onClick={() => setCategoryId(item.categoryId)}>{item.name}</button>)}
            </div>

            <div className="results-toolbar">
              <div>{loading ? "Loading items..." : <><strong>{listings.length}</strong> items available</>}</div>
              <div className="browse-filters">
                <label><Icon name="sliders" /><span className="sr-only">Condition</span><select value={condition} onChange={(event) => setCondition(event.target.value)}><option>{ALL_CONDITIONS}</option>{LISTING_CONDITIONS.map((value) => <option key={value}>{value}</option>)}</select></label>
                <label><span className="sr-only">Sort</span><select value={sort} onChange={(event) => setSort(event.target.value)}>{LISTING_SORTS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
              </div>
            </div>

            {saveError ? <p role="alert" className="browse-error">{saveError}</p> : null}

            {error ? (
              <div className="browse-empty"><h2>We could not load the marketplace</h2><p>{error}</p><button type="button" onClick={() => setReloadToken((current) => current + 1)}>Try again</button></div>
            ) : loading ? (
              <div className="browse-empty"><h2>Loading listings</h2><p>Fetching items from your campus.</p></div>
            ) : listings.length > 0 ? (
              <>
                <div className="marketplace-grid">
                  {listings.map((listing) => (
                    <ListingCard
                      key={listing.id}
                      listing={listing}
                      saved={saved.has(listing.id)}
                      onToggleSaved={toggleSaved}
                    />
                  ))}
                </div>
                {nextCursor ? (
                  <div className="browse-load-more">
                    <button type="button" onClick={loadMore} disabled={loadingMore}>
                      {loadingMore ? "Loading..." : "Load more"}
                    </button>
                  </div>
                ) : null}
              </>
            ) : (
              <div className="browse-empty"><h2>No items match those filters</h2><p>Try a different search or category.</p><button type="button" onClick={clearFilters}>Clear filters</button></div>
            )}
          </div>
        </section>
      </main>
    </MarketplaceLayout>
  )
}
