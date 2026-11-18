// Form choices.
//
// Categories are no longer listed here: they live in the database and the form
// loads them from GET /api/categories, because creating a listing has to send
// the backend's own categoryId rather than a name typed on this side.

// The exact five values the API accepts. The old list ("Like new", "Good",
// "Fair") was invented for the design and only overlapped on two, so picking
// "Like new" would have been rejected as an invalid condition.
export const conditions = ["New", "Great", "Good", "Fair", "Poor"]
