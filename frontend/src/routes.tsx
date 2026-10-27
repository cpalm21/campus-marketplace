import { createBrowserRouter } from "react-router"
import App from "./App"
import LandingPage from "./landingpage"
import LoginPage from "./loginpage";

export const router = createBrowserRouter([
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
    Component: LandingPage
  },
])
