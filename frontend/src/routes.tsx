import { createBrowserRouter } from "react-router"
import App from "./App"
import LandingPage from "./landingpage"
import LoginPage from "./loginpage";
import Browse from "./Browse";
import ListingFormPage from "./ListingFormPage";
import RequireAuth from "./RequireAuth";

export const router = createBrowserRouter([
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
])
