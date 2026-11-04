import { useMemo, useState } from "react"
import { Link } from "react-router"
import type { ReactNode } from "react"
import "./browse.css"

type Listing = {
  id: string
  title: string
  price: number
  category: string
  condition: string
  location: string
  image: string
}

const listings: Listing[] = [
  { id: "desk-1", title: "Solid wood study desk", price: 45, category: "Furniture", condition: "Good", location: "Morris Library", image: "https://images.unsplash.com/photo-1646470742744-d98a021857e1?auto=format&fit=crop&w=900&q=80" },
  { id: "biology-books", title: "Intro biology textbook set", price: 28, category: "Textbooks", condition: "Like new", location: "Trabant Student Center", image: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80" },
  { id: "chair-1", title: "Wood dorm chair", price: 25, category: "Furniture", condition: "Good", location: "Harter Hall", image: "https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80" },
  { id: "mini-fridge", title: "Compact dorm mini fridge", price: 65, category: "Dorm supplies", condition: "Like new", location: "Laird Campus", image: "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?auto=format&fit=crop&w=900&q=80" },
  { id: "bike", title: "Blue commuter bicycle", price: 120, category: "Bikes", condition: "Fair", location: "Kirkbride Hall", image: "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=900&q=80" },
  { id: "calculator", title: "Graphing calculator TI-84", price: 70, category: "Electronics", condition: "Excellent", location: "Smith Hall", image: "https://images.unsplash.com/photo-1593642532400-2682810df593?auto=format&fit=crop&w=900&q=80" },
  { id: "lamp", title: "Adjustable desk lamp", price: 18, category: "Dorm supplies", condition: "Good", location: "East Campus", image: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80" },
  { id: "novel", title: "Modern fiction paperback set", price: 15, category: "Books", condition: "Good", location: "Morris Library", image: "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80" },
]

const categories = ["All", "Furniture", "Textbooks", "Electronics", "Bikes", "Dorm supplies", "Books"]

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
  const [saved, setSaved] = useState<string[]>([])
  const [menuOpen, setMenuOpen] = useState(false)

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
    setSaved((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  }

  return (
    <div className="browse-page">
      <header className="browse-header">
        <div className="browse-container browse-nav">
          <Link className="browse-logo" to="/"><span className="browse-logo-mark"><Icon name="leaf" /></span>CampusLoop</Link>
          <nav className={menuOpen ? "browse-nav-links open" : "browse-nav-links"}>
            <Link to="/">Home</Link><Link className="active" to="/browse" onClick={() => setMenuOpen(false)}>Browse</Link><Link to="/saved" onClick={() => setMenuOpen(false)}>Saved</Link>
          </nav>
          <div className="browse-nav-actions"><Link className="browse-button ghost" to="/sell">Sell an item</Link><button className="browse-menu-button" type="button" aria-label="Toggle navigation" aria-expanded={menuOpen} onClick={() => setMenuOpen((value) => !value)}><Icon name="menu" /></button></div>
        </div>
      </header>

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
              {categories.map((item) => <button className={category === item ? "category-chip active" : "category-chip"} type="button" key={item} onClick={() => setCategory(item)}>{item}</button>)}
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
                  <article className="marketplace-card" key={listing.id}>
                    <Link className="marketplace-image" to={`/listings/${listing.id}`}><img src={listing.image} alt="" /><span>{listing.category}</span></Link>
                    <button className={saved.includes(listing.id) ? "marketplace-favorite active" : "marketplace-favorite"} type="button" aria-label={saved.includes(listing.id) ? `Remove ${listing.title} from saved items` : `Save ${listing.title}`} onClick={() => toggleSaved(listing.id)}><Icon name="heart" size={19} /></button>
                    <div className="marketplace-card-body"><Link to={`/listings/${listing.id}`}><h2>{listing.title}</h2></Link><strong className="marketplace-price">${listing.price}</strong><p>{listing.condition} condition</p><div><Icon name="map" size={15} /><span>{listing.location}</span></div></div>
                  </article>
                ))}
              </div>
            ) : (
              <div className="browse-empty"><h2>No items match those filters</h2><p>Try a different search or category.</p><button type="button" onClick={() => { setSearch(""); setCategory("All"); setCondition("All conditions") }}>Clear filters</button></div>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}
