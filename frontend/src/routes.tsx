import { createBrowserRouter } from "react-router"
import App from "./App"
import LandingPage from "./landingpage"
import LoginPage from "./loginpage";
import Browse from "./Browse";
import ListingFormPage from "./ListingFormPage";
import RequireAuth from "./RequireAuth";
import ListingDetailPage from "./ListingDetailPage";
import SavedListingsPage from "./SavedListingsPage";
import MessagesPage from "./MessagesPage";

export const router = createBrowserRouter([
  // Every listing, image, and favorites endpoint requires a Firebase token,
  // so these pages cannot render for a signed-out visitor any more. They were
  // public while they ran on sample data.
  { path: "/listings/:listingId", element: <RequireAuth><ListingDetailPage /></RequireAuth> },
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
    element: <RequireAuth><Browse /></RequireAuth>,
  },
  {
    path: "/saved",
    element: <RequireAuth><SavedListingsPage /></RequireAuth>,
  },
  {
    path: "/messages",
    element: <RequireAuth><MessagesPage /></RequireAuth>,
  },
  {
    path: "/messages/:conversationId",
    element: <RequireAuth><MessagesPage /></RequireAuth>,
  },
  {
    path: "/messages/new/:listingId",
    element: <RequireAuth><MessagesPage /></RequireAuth>,
  },
])
