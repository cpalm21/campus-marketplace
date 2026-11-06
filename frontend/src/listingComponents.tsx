import { useState } from "react"
import type { ReactNode } from "react"
import { Link, NavLink } from "react-router"
import { auth } from "./firebase"

export type IconName = "arrow" | "back" | "book" | "camera" | "chat" | "check" | "chevron" | "edit" | "filter" | "heart" | "leaf" | "map" | "menu" | "plus" | "search" | "send" | "settings" | "shield" | "spark" | "trash" | "user"

export function Icon({ name, size = 22 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, ReactNode> = {
    arrow: <path d="m5 12h14m-5-5 5 5-5 5" />,
    back: <path d="m19 12H5m5 5-5-5 5-5" />,
    book: (
      <>
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z" />
        <path d="M4 5.5v16M8 7h8" />
      </>
    ),
    camera: (
      <>
        <path d="M14.5 4 16 6h3a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3l1.5-2z" />
        <circle cx="12" cy="13" r="3.5" />
      </>
    ),
    chat: (
      <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z" />
    ),
    check: <path d="m5 12 4 4L19 6" />,
    chevron: <path d="m9 18 6-6-6-6" />,
    edit: (
      <>
        <path d="M12 20h9" />
        <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4z" />
      </>
    ),
    filter: <path d="M4 5h16M7 12h10M10 19h4" />,
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
    plus: <path d="M12 5v14M5 12h14" />,
    search: (
      <>
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
      </>
    ),
    send: <path d="m22 2-7 20-4-9-9-4zM22 2 11 13" />,
    settings: (
      <>
        <circle cx="12" cy="12" r="3" />
        <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6v-.2h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z" />
      </>
    ),
    shield: (
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Zm-3-10 2 2 4-5" />
    ),
    spark: (
      <path d="m12 3 1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6zM5 16l.8 2.2L8 19l-2.2.8L5 22l-.8-2.2L2 19l2.2-.8z" />
    ),
    trash: (
      <>
        <path d="M3 6h18M8 6V3h8v3M19 6l-1 15H6L5 6M10 11v5m4-5v5" />
      </>
    ),
    user: (
      <>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21a8 8 0 0 1 16 0" />
      </>
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

export function Logo() {
  return (
    <Link className="logo" to="/" aria-label="CampusLoop home">
      <span className="logo-mark">
        <Icon name="leaf" size={23} />
      </span>
      <span>CampusLoop</span>
    </Link>
  )
}

function Header() {
  const currentUser = auth.currentUser
  const signedIn = Boolean(currentUser?.emailVerified)
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)
  return (
    <header className="site-header">
      <div className="container nav-wrap">
        <Logo />
        <nav className={open ? "nav-links open" : "nav-links"}>
          <NavLink to="/" onClick={close}>
            Home
          </NavLink>
          <NavLink to="/browse" onClick={close}>
            Browse
          </NavLink>
          {signedIn && (
            <>
              <NavLink to="/listings/new" onClick={close}>
                Sell an item
              </NavLink>
              <NavLink to="/messages" onClick={close}>
                Messages
              </NavLink>
              <NavLink to="/saved" onClick={close}>
                Saved
              </NavLink>
              <NavLink
                className="mobile-only-link"
                to="/my-listings"
                onClick={close}
              >
                My listings
              </NavLink>
              <NavLink
                className="mobile-only-link"
                to="/settings"
                onClick={close}
              >
                Account settings
              </NavLink>
            </>
          )}
        </nav>
        <div className="nav-actions">
          {signedIn ? (
            <>
              <Link
                className="button button-ghost compact-nav"
                to="/my-listings"
              >
                My listings
              </Link>
              <Link className="account-chip" to="/settings">
                {currentUser?.photoURL && <img src={currentUser.photoURL} alt="" />}
                <span>{currentUser?.displayName?.split(" ")[0] || currentUser?.email?.split("@")[0]}</span>
                <Icon name="chevron" size={15} />
              </Link>
            </>
          ) : (
            <>
              <Link className="button button-ghost compact-nav" to="/login">
                Log in
              </Link>
              <Link className="button button-dark compact-nav" to="/signup">
                Join campus
              </Link>
            </>
          )}
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

function Footer() {
  return (
    <footer>
      <div className="container footer-wrap">
        <Logo />
        <p>Making University of Delaware living lighter, together.</p>
        <nav>
          <Link to="/browse">Marketplace</Link>
          <Link to="/signup">Join</Link>
          <Link to="/settings">Account</Link>
        </nav>
      </div>
    </footer>
  )
}

export function PageHeader({
  eyebrow,
  title,
  text,
  action,
}: {
  eyebrow?: string
  title: string
  text?: string
  action?: ReactNode
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <span className="section-kicker">{eyebrow}</span>}
        <h1>{title}</h1>
        {text && <p>{text}</p>}
      </div>
      {action}
    </div>
  )
}

export function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && !error && <small>{hint}</small>}
      {error && <small className="field-error">{error}</small>}
    </label>
  )
}


export function ListingLayout({ children }: { children: ReactNode }) {
  return <div className="listing-page"><div className="app-shell"><Header />{children}<Footer /></div></div>
}
