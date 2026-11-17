// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest"
import { cleanup, render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it } from "vitest"
import { MemoryRouter } from "react-router"
import LandingPage from "./landingpage"
import { MarketplaceLayout } from "./marketplaceComponents"

afterEach(cleanup)

describe("LandingPage", () => {
  it("links the main actions to browse and listing creation", () => {
    render(<LandingPage />)

    expect(
      screen.getByRole("link", { name: "Browse listings" }),
    ).toHaveAttribute("href", "/browse")
    expect(
      screen.getByRole("link", { name: "Sell an item" }),
    ).toHaveAttribute("href", "/listings/new")
  })

  it("links visitors to login, signup, and browse from the header and footer", () => {
    render(<LandingPage />)

    const header = screen.getByRole("banner")
    const footer = screen.getByRole("contentinfo")

    expect(within(header).getByRole("link", { name: "Log in" })).toHaveAttribute(
      "href",
      "/login",
    )
    expect(
      within(header).getByRole("link", { name: "Join campus" }),
    ).toHaveAttribute("href", "/signup")
    expect(within(header).getByRole("link", { name: "Saved" })).toHaveAttribute(
      "href",
      "/saved",
    )
    expect(
      within(footer).getByRole("link", { name: "Marketplace" }),
    ).toHaveAttribute("href", "/browse")
    expect(within(footer).getByRole("link", { name: "Join" })).toHaveAttribute(
      "href",
      "/signup",
    )
    expect(within(footer).getByRole("link", { name: "Account" })).toHaveAttribute(
      "href",
      "/login",
    )
  })

  it("opens the mobile navigation and closes it after choosing a page", async () => {
    const user = userEvent.setup()
    render(<LandingPage />)

    const header = screen.getByRole("banner")
    const menuButton = within(header).getByRole("button", {
      name: "Toggle navigation",
    })
    const navigation = within(header).getByRole("navigation")

    expect(menuButton).toHaveAttribute("aria-expanded", "false")
    await user.click(menuButton)
    expect(menuButton).toHaveAttribute("aria-expanded", "true")
    expect(navigation).toHaveClass("open")

    const browseLink = within(navigation).getByRole("link", {
      name: "Browse",
    })
    browseLink.addEventListener("click", (event) => event.preventDefault())
    await user.click(browseLink)
    expect(menuButton).toHaveAttribute("aria-expanded", "false")
    expect(navigation).not.toHaveClass("open")
  })

  it("toggles the saved state for a featured listing", async () => {
    const user = userEvent.setup()
    render(<LandingPage />)

    const deskCard = screen
      .getByRole("heading", { name: "Solid wood study desk" })
      .closest("article")
    if (!(deskCard instanceof HTMLElement)) {
      throw new Error("Expected the desk listing to be inside a card.")
    }

    const saveButton = within(deskCard).getByRole("button", {
      name: "Save item",
    })

    expect(saveButton).toHaveAttribute("aria-pressed", "false")
    await user.click(saveButton)
    expect(
      within(deskCard).getByRole("button", {
        name: "Remove from saved items",
      }),
    ).toHaveAttribute("aria-pressed", "true")
  })

  it("uses a UD campus photo instead of a listing photo", () => {
    render(<LandingPage />)

    expect(
      screen.getByRole("img", { name: "University of Delaware campus in the fall" }),
    ).toHaveAttribute("src", "/ud-campus.jpg")
  })

  it("keeps Saved available in the Browse page header", () => {
    render(
      <MemoryRouter>
        <MarketplaceLayout active="browse">
          <main>Browse content</main>
        </MarketplaceLayout>
      </MemoryRouter>,
    )

    expect(screen.getByRole("link", { name: "Saved" })).toHaveAttribute(
      "href",
      "/saved",
    )
  })
})
