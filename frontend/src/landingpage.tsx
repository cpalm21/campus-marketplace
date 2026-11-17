import { useState } from "react"
import type { ReactNode } from "react"
import "./landingpage.css"

type IconName =
  | "arrow"
  | "heart"
  | "leaf"
  | "map"
  | "menu"
  | "shield"
  | "spark"

type FeaturedListing = {
  id: string
  title: string
  price: number
  category: string
  condition: string
  location: string
  image: string
}

type Benefit = {
  icon: IconName
  title: string
  text: string
}

const benefits: Benefit[] = [
  {
    icon: "leaf",
    title: "Spend a little less",
    text: "Find textbooks, furniture, and dorm basics for less.",
  },
  {
    icon: "map",
    title: "Meet on campus",
    text: "Arrange a pickup that works for both of you.",
  },
  {
    icon: "spark",
    title: "Pass things along",
    text: "Sell what you no longer need to another student.",
  },
  {
    icon: "shield",
    title: "Just for Blue Hens",
    text: "Create an account with your UD email.",
  },
]

const featuredListings: FeaturedListing[] = [
  {
    id: "desk-1",
    title: "Solid wood study desk",
    price: 45,
    category: "Furniture",
    condition: "Good",
    location: "near Morris Library",
    image:
      "https://images.unsplash.com/photo-1646470742744-d98a021857e1?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=1200",
  },
  {
    id: "biology-books",
    title: "Intro biology textbook set",
    price: 28,
    category: "Textbooks",
    condition: "Like new",
    location: "at Trabant Student Center",
    image:
      "https://images.unsplash.com/photo-1512820790803-83ca734da794?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=1200",
  },
  {
    id: "chair-1",
    title: "Wood dorm chair",
    price: 25,
    category: "Furniture",
    condition: "Good",
    location: "near Harter Hall",
    image:
      "https://images.unsplash.com/photo-1730373451883-45a448eb9bfc?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=1200",
  },
  {
    id: "mini-fridge",
    title: "Compact dorm mini fridge",
    price: 65,
    category: "Dorm supplies",
    condition: "Like new",
    location: "at Laird Campus",
    image:
      "https://images.unsplash.com/photo-1787507470186-deb4523d46cb?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=1200",
  },
]

function Icon({ name, size = 22 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, ReactNode> = {
    arrow: <path d="m5 12h14m-5-5 5 5-5 5" />,
    heart: (
      <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.9-8.6a5.5 5.5 0 0 0-.1-7.8z" />
    ),
    leaf: (
      <>
        <path d="M21 3C12 3 5 7 5 14c0 2 1 4 3 5 7 2 12-5 13-16Z" />
        <path d="M3 21c3-6 7-9 13-12" />
      </>
    ),
    map: (
      <>
        <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
        <circle cx="12" cy="10" r="2.5" />
      </>
    ),
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
    shield: (
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Zm-3-10 2 2 4-5" />
    ),
    spark: (
      <path d="m12 3 1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6zM5 16l.8 2.2L8 19l-2.2.8L5 22l-.8-2.2L2 19l2.2-.8z" />
    ),
  }

  return (
    <svg
      aria-hidden="true"
      fill="none"
      height={size}
      viewBox="0 0 24 24"
      width={size}
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
    >
      {paths[name]}
    </svg>
  )
}

function Logo() {
  return (
    <a className="logo" href="/" aria-label="CampusLoop home">
      <span className="logo-mark">
        <Icon name="leaf" size={23} />
      </span>
      <span>CampusLoop</span>
    </a>
  )
}

function Header() {
  const [open, setOpen] = useState(false)

  return (
    <header className="site-header">
      <div className="container nav-wrap">
        <Logo />
        <nav className={open ? "nav-links open" : "nav-links"}>
          <a className="active" href="/" onClick={() => setOpen(false)}>
            Home
          </a>
          <a href="/browse" onClick={() => setOpen(false)}>
            Browse
          </a>
          <a href="/saved" onClick={() => setOpen(false)}>
            Saved
          </a>
        </nav>
        <div className="nav-actions">
          <a className="button button-ghost compact-nav" href="/login">
            Log in
          </a>
          <a className="button button-dark compact-nav" href="/signup">
            Join campus
          </a>
          <button
            className="icon-button menu-button"
            type="button"
            aria-label="Toggle navigation"
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            <Icon name="menu" />
          </button>
        </div>
      </div>
    </header>
  )
}

function ItemCard({ listing }: { listing: FeaturedListing }) {
  const [saved, setSaved] = useState(false)

  return (
    <article className="listing-card">
      <div className="listing-image">
        <img src={listing.image} alt={listing.title} />
        <span className="category-pill">{listing.category}</span>
      </div>
      <button
        className={saved ? "favorite active" : "favorite"}
        type="button"
        aria-label={saved ? "Remove from saved items" : "Save item"}
        aria-pressed={saved}
        onClick={() => setSaved((value) => !value)}
      >
        <Icon name="heart" size={19} />
      </button>
      <div className="listing-body">
        <div className="listing-title-row">
          <h3>{listing.title}</h3>
          <strong>${listing.price}</strong>
        </div>
        <p>{listing.condition} condition</p>
        <div className="listing-location">
          <Icon name="map" size={15} />
          {listing.location}
        </div>
      </div>
    </article>
  )
}

function Footer() {
  return (
    <footer>
      <div className="container footer-wrap">
        <Logo />
        <p>A place for Blue Hens to buy and sell with each other.</p>
        <nav>
          <a href="/browse">Marketplace</a>
          <a href="/signup">Join</a>
          <a href="/login">Account</a>
        </nav>
      </div>
    </footer>
  )
}

export default function LandingPage() {
  return (
    <div className="campus-loop-landing">
      <Header />
      <main>
        <section className="hero">
          <div className="container hero-grid">
            <div className="hero-copy">
              <div className="eyebrow">
                A marketplace for UD students
              </div>
              <h1>
                Buy and sell
                <br />
                <span className="accent-blue">on campus.</span>
              </h1>
              <p className="hero-description">
                Shop textbooks, furniture, bikes, and dorm essentials from
                other UD students. Have something you don’t need anymore? List
                it here.
              </p>
              <div className="button-row hero-buttons">
                <a className="button button-dark button-large" href="/browse">
                  Browse listings <Icon name="arrow" size={18} />
                </a>
                <a className="button button-ghost button-large" href="/listings/new">
                  Sell an item
                </a>
              </div>
              <div className="trust-line">
                <span>
                  <Icon name="shield" size={17} /> UD email required
                </span>
                <span>
                  <Icon name="map" size={17} /> Meet on campus
                </span>
              </div>
            </div>
            <div className="hero-visual">
              <div className="photo-frame">
                <img
                  src="/ud-campus.jpg"
                  alt="University of Delaware campus in the fall"
                />
              </div>
              <div className="hero-photo-caption">
                <strong>University of Delaware</strong>
                <span>Newark, Delaware</span>
              </div>
            </div>
          </div>
        </section>

        <section className="benefits">
          <div className="container benefit-grid">
            {benefits.map(({ icon, title, text }) => (
              <article className="benefit" key={title}>
                <span className="benefit-icon">
                  <Icon name={icon} size={23} />
                </span>
                <div>
                  <h2>{title}</h2>
                  <p>{text}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="featured-section" id="featured-items">
          <div className="container">
            <div className="section-heading">
              <div>
                <span className="section-kicker">A few examples</span>
                <h2>Things you can find here</h2>
              </div>
            </div>
            <div className="listing-grid">
              {featuredListings.map((listing) => (
                <ItemCard listing={listing} key={listing.id} />
              ))}
            </div>
          </div>
        </section>

        <section className="cta-section">
          <div className="container cta-card">
            <div>
              <span className="section-kicker">
                Have something you don’t use anymore?
              </span>
              <h2>List it for another UD student.</h2>
            </div>
            <a className="button button-light" href="/listings/new">
              List an item <Icon name="arrow" size={18} />
            </a>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
