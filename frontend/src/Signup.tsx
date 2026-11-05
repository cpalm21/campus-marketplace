import { useState } from "react"
import type { FormEvent, ReactNode } from "react"
import { signUp } from "./authService"
import "./signup.css"

function Icon({ name, size = 22 }: { name: "leaf" | "menu"; size?: number }) {
  const paths: Record<"leaf" | "menu", ReactNode> = {
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

function Field({
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

export default function Signup() {
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setError("")
    setSuccess("")

    // Check the form before calling the Firebase signup function.
    const nextErrors: Record<string, string> = {}
    const cleanEmail = email.trim()

    if (!name.trim()) {
      nextErrors.name = "Please enter your name."
    }
    if (!/^[^\s@]+@udel\.edu$/i.test(cleanEmail)) {
      nextErrors.email = "Use your University of Delaware email ending in @udel.edu."
    }
    if (password.length < 8) {
      nextErrors.password = "Password must be at least 8 characters."
    }
    if (password !== confirmPassword) {
      nextErrors.confirm = "Passwords do not match."
    }

    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setIsSubmitting(true)
    try {
      // Keep the existing Firebase authService signup and email verification flow.
      await signUp(cleanEmail, password, confirmPassword)
      setSuccess("Account created! Check your UD email for the verification link.")
      setPassword("")
      setConfirmPassword("")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Signup failed. Please try again.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="campus-loop-signup">
      <Header />
      <main className="auth-page">
        <div className="auth-decoration">
          <Icon name="leaf" size={34} />
        </div>
        <section className="auth-card">
          <span className="section-kicker">University of Delaware</span>
          <h1>Join your campus loop</h1>
          <p className="auth-intro">
            Create an account with your university email to start buying and
            selling.
          </p>

          <form className="stack-form" onSubmit={handleSubmit} noValidate>
            <fieldset className="signup-fields" disabled={isSubmitting}>
              <Field label="Full name" error={errors.name}>
                <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Alex Morgan"
                  autoComplete="name"
                  required
                  aria-invalid={Boolean(errors.name)}
                />
              </Field>

              <Field
                label="University email"
                error={errors.email}
                hint="Use your @udel.edu email."
              >
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="alex@udel.edu"
                  autoComplete="email"
                  required
                  aria-invalid={Boolean(errors.email)}
                />
              </Field>

              <div className="two-column-fields">
                <Field label="Password" error={errors.password}>
                  <input
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="8+ characters"
                    autoComplete="new-password"
                    required
                    aria-invalid={Boolean(errors.password)}
                  />
                </Field>
                <Field label="Confirm password" error={errors.confirm}>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    placeholder="Repeat password"
                    autoComplete="new-password"
                    required
                    aria-invalid={Boolean(errors.confirm)}
                  />
                </Field>
              </div>

              <button
                className="button button-dark button-full"
                type="submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Creating account..." : "Create account"}
              </button>
            </fieldset>

            {error && <p className="signup-feedback signup-error" role="alert">{error}</p>}
            {success && <p className="signup-feedback signup-success" role="status">{success}</p>}
          </form>

          <p className="auth-switch">
            Already have an account? <a href="/login">Log in</a>
          </p>
        </section>
      </main>
      <Footer />
    </div>
  )
}
