import { useMemo, useState } from "react"
import type { ReactNode } from "react"
import { ListingCard, MarketplaceLayout } from "./marketplaceComponents"
import {
  getSavedListingIds,
  listings,
  marketplaceCategories,
  toggleSavedListing,
} from "./marketplaceData"
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

export default function Browse() {
  const [search, setSearch] = useState("")
  const [category, setCategory] = useState("All")
  const [condition, setCondition] = useState("All conditions")
  const [sort, setSort] = useState("Newest")
  const [saved, setSaved] = useState(getSavedListingIds)

  const filteredListings = useMemo(() => {
    const query = search.trim().toLowerCase()
    const filtered = listings.filter((listing) => {
      const matchesSearch = !query || `${listing.title} ${listing.category} ${listing.location}`.toLowerCase().includes(query)
      const matchesCategory = category === "All" || listing.category === category
      const matchesCondition = condition === "All conditions" || listing.condition === condition
      return matchesSearch && matchesCategory && matchesCondition
    })

    if (sort === "Price: low to high") return [...filtered].sort((a, b) => a.price - b.price)
    if (sort === "Price: high to low") return [...filtered].sort((a, b) => b.price - a.price)
    return filtered
  }, [search, category, condition, sort])

  function toggleSaved(id: string) {
    setSaved(toggleSavedListing(id))
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
              {marketplaceCategories.map((item) => <button className={category === item ? "category-chip active" : "category-chip"} type="button" key={item} onClick={() => setCategory(item)}>{item}</button>)}
            </div>

            <div className="results-toolbar">
              <div><strong>{filteredListings.length}</strong> items available</div>
              <div className="browse-filters">
                <label><Icon name="sliders" /><span className="sr-only">Condition</span><select value={condition} onChange={(event) => setCondition(event.target.value)}><option>All conditions</option><option>Excellent</option><option>Like new</option><option>Good</option><option>Fair</option></select></label>
                <label><span className="sr-only">Sort</span><select value={sort} onChange={(event) => setSort(event.target.value)}><option>Newest</option><option>Price: low to high</option><option>Price: high to low</option></select></label>
              </div>
            </div>

            {filteredListings.length > 0 ? (
              <div className="marketplace-grid">
                {filteredListings.map((listing) => (
                  <ListingCard
                    key={listing.id}
                    listing={listing}
                    saved={saved.includes(listing.id)}
                    onToggleSaved={toggleSaved}
                  />
                ))}
              </div>
            ) : (
              <div className="browse-empty"><h2>No items match those filters</h2><p>Try a different search or category.</p><button type="button" onClick={() => { setSearch(""); setCategory("All"); setCondition("All conditions") }}>Clear filters</button></div>
            )}
          </div>
        </section>
      </main>
    </MarketplaceLayout>
  )
}
