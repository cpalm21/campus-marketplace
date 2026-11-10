import { createBrowserRouter } from "react-router"
import App from "./App"
import LandingPage from "./landingpage"
import LoginPage from "./loginpage";
import Browse from "./Browse";
import ListingFormPage from "./ListingFormPage";
import RequireAuth from "./RequireAuth";
import ListingDetailPage from "./ListingDetailPage";
import SavedListingsPage from "./SavedListingsPage";

export const router = createBrowserRouter([
  { path: "/listings/:listingId", Component: ListingDetailPage },
  { path: "/listings/new", element: <RequireAuth><ListingFormPage /></RequireAuth> },
  { path: "/sell", element: <RequireAuth><ListingFormPage /></RequireAuth> },
  {
    path: "/",
    Component: LandingPage,
  },
  {
    path: "/signup",
    Component: App,
  },
  {
    path: "/login",
    Component: LoginPage
  },
  {
    path: "/browse",
    Component: Browse
  },
  {
    path: "/saved",
    Component: SavedListingsPage
  },
])
