import { useEffect, useState } from "react"
import { Link, useNavigate, useParams } from "react-router"
import { Icon } from "./listingComponents"
import { MarketplaceLayout } from "./marketplaceComponents"
import { errorMessage, isAbortError, isSessionExpired } from "./apiClient"
import {
  loadListing,
  loadSavedListingIds,
  removeSavedListing,
  saveListing,
} from "./marketplaceData"
import type { Listing } from "./marketplaceData"
import "./browse.css"

export default function ListingDetailPage() {
  const { listingId } = useParams()
  const navigate = useNavigate()

  const [listing, setListing] = useState<Listing | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [saved, setSaved] = useState(false)
  const [saveError, setSaveError] = useState("")

  useEffect(() => {
    if (!listingId) {
      setLoading(false)
      return
    }

    const controller = new AbortController()

    loadListing(listingId, controller.signal)
      .then((result) => setListing(result.listing))
      .catch((err) => {
        if (isAbortError(err)) {
          return
        }

        if (isSessionExpired(err)) {
          navigate("/login?next=" + encodeURIComponent("/listings/" + listingId), { replace: true })
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
  }, [listingId, navigate])

  useEffect(() => {
    if (!listingId) {
      return
    }

    const controller = new AbortController()

    loadSavedListingIds(controller.signal)
      .then((ids) => setSaved(ids.has(listingId)))
      // The save button still works if this fails; it just starts unfilled.
      .catch(() => undefined)

    return () => controller.abort()
  }, [listingId])

  async function toggleSaved() {
    if (!listingId) {
      return
    }

    const wasSaved = saved

    setSaved(!wasSaved)
    setSaveError("")

    try {
      if (wasSaved) {
        await removeSavedListing(listingId)
      } else {
        await saveListing(listingId)
      }
    } catch (err) {
      // The backend refuses some saves deliberately: your own listing is 403
      // and a sold item is 409. Show its reason instead of a button that
      // quietly snaps back.
      setSaved(wasSaved)

      if (!isAbortError(err)) {
        setSaveError(errorMessage(err))
      }
    }
  }

  return (
    <MarketplaceLayout active={null}>
      <main className="browse-container listing-detail-main">
        {loading ? (
          <div className="browse-empty listing-not-found">
            <h1>Loading this item</h1>
          </div>
        ) : listing ? (
          <>
            <Link className="listing-back-link" to="/browse">
              <span aria-hidden="true">&larr;</span> Back to browse
            </Link>
            <article className="listing-detail">
              <div className="listing-detail-image">
                <img src={listing.image} alt={listing.title} />
              </div>
              <div className="listing-detail-info">
                <span className="listing-detail-category">{listing.category}</span>
                <h1>{listing.title}</h1>
                <p className="listing-detail-price">${listing.price}</p>
                <p className="listing-detail-condition">{listing.condition} condition</p>
                {/* A reserved or sold item still has a page, and a buyer should
                    be told before they message the seller about it. */}
                {listing.status === "Available" ? null : (
                  <p className="listing-detail-status">This item is {listing.status.toLowerCase()}.</p>
                )}
                <p className="listing-detail-description">{listing.description}</p>
                <p className="listing-detail-location">
                  <Icon name="map" size={17} /> Pickup near {listing.location}
                </p>
                {saveError ? <p role="alert" className="browse-error">{saveError}</p> : null}
                <Link
                  className="marketplace-action-button saved"
                  style={{ marginRight: 12, marginBottom: 12, gap: 8, color: "#fff" }}
                  to={`/messages/new/${encodeURIComponent(listing.id)}`}
                >
                  <Icon name="chat" size={18} />
                  Message seller
                </Link>
                <button
                  className={saved ? "marketplace-action-button saved" : "marketplace-action-button"}
                  type="button"
                  aria-pressed={saved}
                  onClick={toggleSaved}
                >
                  {saved ? "Remove from saved" : "Save item"}
                </button>
              </div>
            </article>
          </>
        ) : (
          <div className="browse-empty listing-not-found">
            <h1>We couldn’t find that item.</h1>
            <p>{error || "It may have been removed or the link may be incorrect."}</p>
            <Link className="browse-button marketplace-action-button saved" to="/browse">
              Browse listings
            </Link>
          </div>
        )}
      </main>
    </MarketplaceLayout>
  )
}
