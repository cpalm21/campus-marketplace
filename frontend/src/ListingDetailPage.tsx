import { useState } from "react"
import { Link, useParams } from "react-router"
import { Icon } from "./listingComponents"
import { MarketplaceLayout } from "./marketplaceComponents"
import {
  getSavedListingIds,
  listings,
  toggleSavedListing,
} from "./marketplaceData"
import "./browse.css"

export default function ListingDetailPage() {
  const { listingId } = useParams()
  const listing = listings.find((item) => item.id === listingId)
  const [savedIds, setSavedIds] = useState(getSavedListingIds)

  function toggleSaved(id: string) {
    setSavedIds(toggleSavedListing(id))
  }

  return (
    <MarketplaceLayout active={null}>
      <main className="browse-container listing-detail-main">
        {listing ? (
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
                <p className="listing-detail-description">{listing.description}</p>
                <p className="listing-detail-location">
                  <Icon name="map" size={17} /> Pickup near {listing.location}
                </p>
                <button
                  className={
                    savedIds.includes(listing.id)
                      ? "marketplace-action-button saved"
                      : "marketplace-action-button"
                  }
                  type="button"
                  aria-pressed={savedIds.includes(listing.id)}
                  onClick={() => toggleSaved(listing.id)}
                >
                  {savedIds.includes(listing.id) ? "Remove from saved" : "Save item"}
                </button>
              </div>
            </article>
          </>
        ) : (
          <div className="browse-empty listing-not-found">
            <h1>We couldn’t find that item.</h1>
            <p>It may have been removed or the link may be incorrect.</p>
            <Link className="browse-button marketplace-action-button saved" to="/browse">
              Browse listings
            </Link>
          </div>
        )}
      </main>
    </MarketplaceLayout>
  )
}
