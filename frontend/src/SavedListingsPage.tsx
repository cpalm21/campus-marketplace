import { useEffect, useState } from "react"
import { Link, useNavigate } from "react-router"
import { ListingCard, MarketplaceLayout } from "./marketplaceComponents"
import { errorMessage, isAbortError, isSessionExpired } from "./apiClient"
import { loadSavedListings, removeSavedListing } from "./marketplaceData"
import type { Listing } from "./marketplaceData"
import "./browse.css"

export default function SavedListingsPage() {
  const navigate = useNavigate()
  const [savedListings, setSavedListings] = useState<Listing[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    const controller = new AbortController()

    loadSavedListings(controller.signal)
      .then(setSavedListings)
      .catch((err) => {
        if (isAbortError(err)) {
          return
        }

        if (isSessionExpired(err)) {
          navigate("/login?next=" + encodeURIComponent("/saved"), { replace: true })
          return
        }

        setError(errorMessage(err))
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [navigate])

  async function toggleSaved(id: string) {
    const removed = savedListings.find((listing) => listing.id === id)

    if (!removed) {
      return
    }

    // Every card on this page is saved, so the only action is removal. Drop it
    // from the list straight away and restore it if the backend refuses.
    setSavedListings((current) => current.filter((listing) => listing.id !== id))
    setError("")

    try {
      await removeSavedListing(id)
    } catch (err) {
      setSavedListings((current) => [removed, ...current])

      if (!isAbortError(err)) {
        setError(errorMessage(err))
      }
    }
  }

  return (
    <MarketplaceLayout active="saved">
      <main className="browse-container saved-listings-main">
        <div className="saved-heading">
          <span className="browse-eyebrow">Your shortlist</span>
          <h1>Saved items</h1>
          <p>Keep track of the campus finds you want to come back to.</p>
        </div>
        {error ? <p role="alert" className="browse-error">{error}</p> : null}
        {loading ? (
          <div className="browse-empty saved-empty">
            <h2>Loading your saved items</h2>
          </div>
        ) : savedListings.length ? (
          <div className="marketplace-grid">
            {savedListings.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                saved
                onToggleSaved={toggleSaved}
              />
            ))}
          </div>
        ) : (
          <div className="browse-empty saved-empty">
            <h2>No saved items yet</h2>
            <p>Save something from Browse and it’ll be waiting here.</p>
            <Link className="browse-button marketplace-action-button saved" to="/browse">
              Browse listings
            </Link>
          </div>
        )}
      </main>
    </MarketplaceLayout>
  )
}
