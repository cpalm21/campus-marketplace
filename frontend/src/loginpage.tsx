// React state keeps track of the form values and open menus.
import { useState } from "react"
import type { ReactNode, SubmitEvent } from "react"
import "./loginpage.css"

// Draw one of the small icons used on this page.
function Icon({
  name,
  size = 22,
}: {
  name: "book" | "check" | "leaf" | "menu"
  size?: number
}) {
  // Each name matches an SVG drawing. The size is set by the Icon props.
  const paths: Record<"book" | "check" | "leaf" | "menu", ReactNode> = {
    book: (
      <>
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z" />
        <path d="M4 5.5v16M8 7h8" />
      </>
    ),
    check: <path d="m5 12 4 4L19 6" />,
    leaf: (
      <>
        <path d="M21 3C12 3 5 7 5 14c0 2 1 4 3 5 7 2 12-5 13-16Z" />
        <path d="M3 21c3-6 7-9 13-12" />
      </>
    ),
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
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

// The logo links back to the home page.
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

// Navigation at the top of the page.
function Header() {
  // Track whether the menu is open on small screens.
  const [open, setOpen] = useState(false)

  return (
    <header className="site-header">
      <div className="container nav-wrap">
        <Logo />
        <nav className={open ? "nav-links open" : "nav-links"}>
          <a href="/" onClick={() => setOpen(false)}>
            Home
          </a>
          <a href="/browse" onClick={() => setOpen(false)}>
            Browse
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

// Bottom section with the logo and links to other pages.
function Footer() {
  return (
    <footer>
      <div className="container footer-wrap">
        <Logo />
        <p>Making University of Delaware living lighter, together.</p>
        <nav>
          <a href="/browse">Marketplace</a>
          <a href="/signup">Join</a>
          <a href="/settings">Account</a>
        </nav>
      </div>
    </footer>
  )
}

// Reuse this layout for an input, its label, and an optional error message.
// children is the input passed between the Field tags.
function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: ReactNode
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {error && <small className="field-error">{error}</small>}
    </label>
  )
}

// Main component that brings the login page together.
export default function LoginPage() {
  // Store what the user types and any errors shown below the inputs.
  const [form, setForm] = useState({ email: "", password: "" })
  const [errors, setErrors] = useState<Record<string, string>>({})
  // Control the reset popup and its confirmation message.
  const [resetOpen, setResetOpen] = useState(false)
  const [resetSent, setResetSent] = useState(false)

  // Run when the user submits the login form.
  function submit(event: SubmitEvent<HTMLFormElement>) {
    // Keep the browser from reloading the page when the form is submitted.
    event.preventDefault()
    // These are basic checks only: an @ in the email and a nonempty password.
    const nextErrors: Record<string, string> = {}
    if (!form.email.includes("@")) nextErrors.email = "Enter a valid email."
    if (!form.password) nextErrors.password = "Enter your password."
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      // Demo login only. This does not check an account or verify a password.
      // Replace this with real authentication when connecting the backend.
      window.localStorage.setItem("campusloop-signed-in", "true")
      // Read the requested destination from the URL, or use /browse.
      // Before real use, validate this destination; // can lead to another site.
      const next = new URLSearchParams(window.location.search).get("next")
      window.location.assign(next?.startsWith("/") ? next : "/browse")
    }
  }

  return (
    <div className="campus-loop-login">
      <Header />
      <main className="auth-page">
        <div className="auth-decoration">
          <Icon name="leaf" size={34} />
        </div>
        <section className="auth-card">
          <span className="section-kicker">University of Delaware</span>
          <h1>Welcome back</h1>
          <p className="auth-intro">
            Log in to message sellers, save items, and manage your listings.
          </p>
          {/* Use the checks in submit instead of the browser validation messages. */}
          <form className="stack-form" onSubmit={submit} noValidate>
            {/* Typing updates the email while keeping the current password. */}
            <Field label="University email" error={errors.email}>
              <input
                type="email"
                value={form.email}
                onChange={(event) =>
                  setForm({ ...form, email: event.target.value })
                }
                placeholder="alex@udel.edu"
              />
            </Field>
            {/* The password input hides the characters as the user types. */}
            <Field label="Password" error={errors.password}>
              <input
                type="password"
                value={form.password}
                onChange={(event) =>
                  setForm({ ...form, password: event.target.value })
                }
                placeholder="Your password"
              />
            </Field>
            <button className="button button-dark button-full" type="submit">
              Log in
            </button>
            <button
              className="plain-link"
              type="button"
              onClick={() => setResetOpen(true)}
            >
              Forgot your password?
            </button>
          </form>
          <p className="auth-switch">
            New to CampusLoop? <a href="/signup">Sign up</a>
          </p>
        </section>
      </main>
      <Footer />

      {/* Show the reset popup only when resetOpen is true. */}
      {resetOpen && (
        // Clicking the background closes the popup.
        <div className="modal-backdrop" onMouseDown={() => setResetOpen(false)}>
          {/* Clicks inside this box stay inside it, so they do not close the popup. */}
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reset-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <span className="round-icon">
              <Icon name="book" size={25} />
            </span>
            <h2 id="reset-title">Reset your password</h2>
            <p>
              Enter your university email and we'll send reset instructions.
            </p>
            {/* Demo only: this switches the message but does not send an email. */}
            {resetSent ? (
              <div className="success-notice" role="status">
                <Icon name="check" size={18} />
                Reset request sent. Check your inbox.
              </div>
            ) : (
              <Field label="University email">
                <input type="email" placeholder="alex@udel.edu" />
              </Field>
            )}
            <div className="button-row">
              <button
                className="button button-dark"
                type="button"
                onClick={() => setResetSent(true)}
              >
                Send reset link
              </button>
              <button
                className="button button-ghost"
                type="button"
                onClick={() => setResetOpen(false)}
              >
                Close
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
