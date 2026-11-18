import { useEffect, useState } from "react"
import type { ChangeEvent, FormEvent } from "react"
import { Link, useNavigate } from "react-router"
import { Field, Icon, PageHeader, ListingLayout } from "./listingComponents"
import { conditions } from "./listingData"
import { errorMessage, isSessionExpired } from "./apiClient"
import { createListing, loadCategories } from "./marketplaceData"
import "./listingformpage.css"

// The backend's limits, repeated here so the form can say what is wrong before
// spending a request to be told the same thing.
const MAX_TITLE = 50
const MAX_DESCRIPTION = 500
const MAX_LOCATION = 100
const MAX_PRICE = 99999999.99

export default function ListingFormPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    title: "",
    price: "",
    // The id the backend assigned, not a name. Held as a string because that
    // is what a <select> value is; converted on submit.
    categoryId: "",
    condition: "",
    description: "",
    location: "Pickup near Trabant Student Center",
  })
  const [photos, setPhotos] = useState<string[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})

  const [categories, setCategories] = useState<{ categoryId: number; name: string }[]>([])
  const [categoryError, setCategoryError] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState("")

  useEffect(() => {
    loadCategories()
      .then(setCategories)
      // Without categories there is no valid listing to send, so this one is
      // worth showing rather than silently degrading.
      .catch((err) => setCategoryError(errorMessage(err)))
  }, [])

  function upload(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files || []).slice(
      0,
      Math.max(0, 4 - photos.length),
    )
    files.forEach((file) => {
      const reader = new FileReader()
      reader.onload = () =>
        setPhotos((current) => [...current, String(reader.result)].slice(0, 4))
      reader.readAsDataURL(file)
    })
  }

  function validate(): Record<string, string> {
    const next: Record<string, string> = {}
    const title = form.title.trim()
    const price = Number(form.price)

    if (!title) next.title = "Add a clear item title."
    else if (title.length > MAX_TITLE) next.title = `Use ${MAX_TITLE} characters or fewer.`

    if (!form.price.trim() || !Number.isFinite(price) || price < 0) {
      next.price = "Enter a valid price."
    } else if (price > MAX_PRICE) {
      next.price = "That price is too high."
    } else if (!/^\d*(\.\d{1,2})?$/.test(form.price.trim())) {
      // The API rejects more precision than cents, so catch it here instead of
      // letting a 400 come back after the user waits for the request.
      next.price = "Use at most two decimal places."
    }

    if (!form.categoryId) next.category = "Choose a category."
    if (!form.condition) next.condition = "Choose a condition."

    const description = form.description.trim()
    if (description.length < 20) next.description = "Add at least 20 characters of detail."
    else if (description.length > MAX_DESCRIPTION)
      next.description = `Use ${MAX_DESCRIPTION} characters or fewer.`

    if (form.location.trim().length > MAX_LOCATION)
      next.location = `Use ${MAX_LOCATION} characters or fewer.`

    return next
  }

  async function submit(event: FormEvent) {
    event.preventDefault()

    const nextErrors = validate()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setSubmitting(true)
    setSubmitError("")

    try {
      const listing = await createListing({
        title: form.title.trim(),
        price: Number(form.price),
        categoryId: Number(form.categoryId),
        condition: form.condition,
        // Blank optional fields are left out rather than sent empty, since the
        // backend stores a blank as null anyway.
        description: form.description.trim() || undefined,
        location: form.location.trim() || undefined,
      })

      // Straight to the published item, so the seller sees the real thing
      // rather than a success message they have to act on.
      navigate(`/listings/${listing.id}`)
    } catch (err) {
      if (isSessionExpired(err)) {
        navigate("/login?next=" + encodeURIComponent("/sell"), { replace: true })
        return
      }

      setSubmitError(errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <ListingLayout>
      <main className="page-main">
        <div className="container narrow-container">
          <PageHeader
            eyebrow="Sell on campus"
            title="List an item"
            text="Add clear details and photos so another student knows exactly what to expect."
          />
          <form className="listing-form card" onSubmit={submit} noValidate>
            <section className="form-section">
              <div className="form-section-title">
                <span>1</span>
                <div>
                  <h2>Photos</h2>
                  <p>Add up to four photos. The first photo is your cover.</p>
                </div>
              </div>
              {/* Photos are previewed here but not published yet: the API
                  stores image links and nothing hosts an uploaded file. Saying
                  so on the form is better than letting a seller believe their
                  pictures went up with the listing. */}
              <p className="photo-notice" role="note">
                Photo uploads are coming soon. Your listing will publish without them for now.
              </p>
              <div className="photo-upload-grid">
                {photos.map((photo, index) => (
                  <div className="upload-preview" key={`${photo}-${index}`}>
                    <img src={photo} alt={`Item preview ${index + 1}`} />
                    <button
                      type="button"
                      aria-label="Remove photo"
                      onClick={() =>
                        setPhotos((items) =>
                          items.filter((_, photoIndex) => photoIndex !== index),
                        )
                      }
                    >
                      ×
                    </button>
                    {index === 0 && <span>Cover</span>}
                  </div>
                ))}
                {photos.length < 4 && (
                  <label className="upload-box">
                    <Icon name="camera" size={27} />
                    <strong>Add photos</strong>
                    <small>PNG or JPG</small>
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={upload}
                    />
                  </label>
                )}
              </div>
            </section>
            <section className="form-section">
              <div className="form-section-title">
                <span>2</span>
                <div>
                  <h2>Item details</h2>
                  <p>Be specific and honest about the item's condition.</p>
                </div>
              </div>
              <div className="form-fields">
                <Field label="Title" error={errors.title}>
                  <input
                    value={form.title}
                    maxLength={MAX_TITLE}
                    onChange={(event) =>
                      setForm({ ...form, title: event.target.value })
                    }
                    placeholder="e.g. Compact dorm mini fridge"
                  />
                </Field>
                <div className="two-column-fields">
                  <Field label="Price" error={errors.price}>
                    <div className="prefix-input">
                      <span>$</span>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={form.price}
                        onChange={(event) =>
                          setForm({ ...form, price: event.target.value })
                        }
                        placeholder="0"
                      />
                    </div>
                  </Field>
                  <Field label="Category" error={errors.category || categoryError}>
                    <select
                      value={form.categoryId}
                      onChange={(event) =>
                        setForm({ ...form, categoryId: event.target.value })
                      }
                    >
                      <option value="">
                        {categories.length ? "Choose one" : "Loading categories..."}
                      </option>
                      {categories.map((category) => (
                        <option key={category.categoryId} value={category.categoryId}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                  </Field>
                </div>
                <Field label="Condition" error={errors.condition}>
                  <div className="choice-row">
                    {conditions.map((condition) => (
                      <button
                        className={
                          form.condition === condition
                            ? "choice-chip active"
                            : "choice-chip"
                        }
                        type="button"
                        key={condition}
                        onClick={() => setForm({ ...form, condition })}
                      >
                        {condition}
                      </button>
                    ))}
                  </div>
                </Field>
                <Field
                  label="Description"
                  error={errors.description}
                  hint="Include dimensions, flaws, and what's included."
                >
                  <textarea
                    rows={5}
                    maxLength={MAX_DESCRIPTION}
                    value={form.description}
                    onChange={(event) =>
                      setForm({ ...form, description: event.target.value })
                    }
                    placeholder="Tell students about your item..."
                  />
                </Field>
                <Field label="General pickup location" error={errors.location}>
                  <input
                    value={form.location}
                    maxLength={MAX_LOCATION}
                    onChange={(event) =>
                      setForm({ ...form, location: event.target.value })
                    }
                  />
                </Field>
              </div>
            </section>
            {submitError ? (
              <p className="form-submit-error" role="alert">{submitError}</p>
            ) : null}
            <div className="form-actions">
              <Link className="button button-ghost" to="/browse">
                Cancel
              </Link>
              <button className="button button-dark" type="submit" disabled={submitting}>
                {submitting ? "Publishing..." : "Publish listing"}
              </button>
            </div>
          </form>
        </div>
      </main>
    </ListingLayout>
  )
}
