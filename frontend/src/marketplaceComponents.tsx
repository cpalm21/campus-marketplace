import { useState } from "react"
import type { ReactNode } from "react"
import { Link } from "react-router"
import { Icon } from "./listingComponents"
import type { Listing } from "./marketplaceData"

export function MarketplaceLayout({
  active,
  children,
}: {
  active: "browse" | "saved" | null
  children: ReactNode
}) {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="browse-page">
      <header className="browse-header">
        <div className="browse-container browse-nav">
          <Link className="browse-logo" to="/">
            <span className="browse-logo-mark">
              <Icon name="leaf" />
            </span>
            CampusLoop
          </Link>
          <nav className={menuOpen ? "browse-nav-links open" : "browse-nav-links"}>
            <Link to="/" onClick={closeMenu}>Home</Link>
            <Link className={active === "browse" ? "active" : undefined} to="/browse" onClick={closeMenu}>
              Browse
            </Link>
            <Link className={active === "saved" ? "active" : undefined} to="/saved" onClick={closeMenu}>
              Saved
            </Link>
          </nav>
          <div className="browse-nav-actions">
            <Link className="browse-button ghost" to="/sell">
              Sell an item
            </Link>
            <button
              className="browse-menu-button"
              type="button"
              aria-label="Toggle navigation"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <Icon name="menu" />
            </button>
          </div>
        </div>
      </header>
      {children}
    </div>
  )
}

export function ListingCard({
  listing,
  saved,
  onToggleSaved,
}: {
  listing: Listing
  saved: boolean
  onToggleSaved: (id: string) => void
}) {
  return (
    <article className="marketplace-card">
      <Link className="marketplace-image" to={`/listings/${listing.id}`}>
        <img src={listing.image} alt={listing.title} />
        <span>{listing.category}</span>
      </Link>
      <button
        className={saved ? "marketplace-favorite active" : "marketplace-favorite"}
        type="button"
        aria-label={saved ? `Remove ${listing.title} from saved items` : `Save ${listing.title}`}
        aria-pressed={saved}
        onClick={() => onToggleSaved(listing.id)}
      >
        <Icon name="heart" size={19} />
      </button>
      <div className="marketplace-card-body">
        <Link to={`/listings/${listing.id}`}>
          <h2>{listing.title}</h2>
        </Link>
        <strong className="marketplace-price">${listing.price}</strong>
        <p>{listing.condition} condition</p>
        <div>
          <Icon name="map" size={15} />
          <span>{listing.location}</span>
        </div>
      </div>
    </article>
  )
}
