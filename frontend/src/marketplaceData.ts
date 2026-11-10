export type Listing = {
  id: string
  title: string
  price: number
  category: string
  condition: string
  location: string
  image: string
  description: string
}

export const listings: Listing[] = [
  {
    id: "desk-1",
    title: "Solid wood study desk",
    price: 45,
    category: "Furniture",
    condition: "Good",
    location: "Morris Library",
    image: "https://images.unsplash.com/photo-1646470742744-d98a021857e1?auto=format&fit=crop&w=900&q=80",
    description:
      "A sturdy solid wood desk with a spacious top for studying. Shows some light wear from normal use.",
  },
  {
    id: "biology-books",
    title: "Intro biology textbook set",
    price: 28,
    category: "Textbooks",
    condition: "Like new",
    location: "Trabant Student Center",
    image: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80",
    description:
      "A set of introductory biology textbooks in like-new condition. Great for keeping course reading together.",
  },
  {
    id: "chair-1",
    title: "Wood dorm chair",
    price: 25,
    category: "Furniture",
    condition: "Good",
    location: "Harter Hall",
    image: "https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80",
    description:
      "A comfortable wooden chair that works well at a desk or as extra seating. Normal signs of use.",
  },
  {
    id: "mini-fridge",
    title: "Compact dorm mini fridge",
    price: 65,
    category: "Dorm supplies",
    condition: "Like new",
    location: "Laird Campus",
    image: "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?auto=format&fit=crop&w=900&q=80",
    description:
      "A compact mini fridge in like-new condition, sized for a dorm room or office.",
  },
  {
    id: "bike",
    title: "Blue commuter bicycle",
    price: 120,
    category: "Bikes",
    condition: "Fair",
    location: "Kirkbride Hall",
    image: "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=900&q=80",
    description:
      "A blue commuter bike that is ready for campus trips. It has some cosmetic wear from regular use.",
  },
  {
    id: "calculator",
    title: "Graphing calculator TI-84",
    price: 70,
    category: "Electronics",
    condition: "Excellent",
    location: "Smith Hall",
    image: "https://images.unsplash.com/photo-1593642532400-2682810df593?auto=format&fit=crop&w=900&q=80",
    description:
      "A graphing calculator in excellent condition. A practical option for math and science courses.",
  },
  {
    id: "lamp",
    title: "Adjustable desk lamp",
    price: 18,
    category: "Dorm supplies",
    condition: "Good",
    location: "East Campus",
    image: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80",
    description:
      "An adjustable lamp for a dorm desk or bedside table. Works well and has light signs of use.",
  },
  {
    id: "novel",
    title: "Modern fiction paperback set",
    price: 15,
    category: "Books",
    condition: "Good",
    location: "Morris Library",
    image: "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80",
    description:
      "A small set of modern fiction paperbacks in good, readable condition.",
  },
]

export const marketplaceCategories = [
  "All",
  "Furniture",
  "Textbooks",
  "Electronics",
  "Bikes",
  "Dorm supplies",
  "Books",
]

const SAVED_LISTINGS_KEY = "campus-marketplace:saved-listings"
const listingIds = new Set(listings.map((listing) => listing.id))

export function getSavedListingIds(): string[] {
  const saved = window.localStorage.getItem(SAVED_LISTINGS_KEY)
  if (!saved) return []

  return saved.split(",").filter((id) => listingIds.has(id))
}

export function toggleSavedListing(id: string): string[] {
  const current = getSavedListingIds()
  const next = current.includes(id)
    ? current.filter((savedId) => savedId !== id)
    : [...current, id]

  window.localStorage.setItem(SAVED_LISTINGS_KEY, next.join(","))
  return next
}
