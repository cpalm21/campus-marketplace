import { useState } from "react"
import { Link } from "react-router"
import { ListingCard, MarketplaceLayout } from "./marketplaceComponents"
import {
  getSavedListingIds,
  listings,
  toggleSavedListing,
} from "./marketplaceData"
import "./browse.css"

export default function SavedListingsPage() {
  const [savedIds, setSavedIds] = useState(getSavedListingIds)
  const savedListings = listings.filter((listing) => savedIds.includes(listing.id))

  function toggleSaved(id: string) {
    setSavedIds(toggleSavedListing(id))
  }

  return (
    <MarketplaceLayout active="saved">
      <main className="browse-container saved-listings-main">
        <div className="saved-heading">
          <span className="browse-eyebrow">Your shortlist</span>
          <h1>Saved items</h1>
          <p>Keep track of the campus finds you want to come back to.</p>
        </div>
        {savedListings.length ? (
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
