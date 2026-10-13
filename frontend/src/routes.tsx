import { createBrowserRouter } from "react-router"
import App from "./App"
import LandingPage from "./landingpage"

export const router = createBrowserRouter([
  {
    path: "/",
    Component: LandingPage,
  },
  {
    path: "/signup",
    Component: App,
  },
])
