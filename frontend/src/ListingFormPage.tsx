import { useState } from "react"
import type { ChangeEvent, FormEvent } from "react"
import { Link } from "react-router"
import { Field, Icon, PageHeader, ListingLayout } from "./listingComponents"
import { categories, conditions } from "./listingData"
import "./listingformpage.css"

export default function ListingFormPage() {
  const [form, setForm] = useState({
    title: "",
    price: "",
    category: "",
    condition: "",
    description: "",
    location: "Pickup near Trabant Student Center",
  })
  const [photos, setPhotos] = useState<string[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})

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

  function submit(event: FormEvent) {
    event.preventDefault()
    const nextErrors: Record<string, string> = {}
    if (!form.title.trim()) nextErrors.title = "Add a clear item title."
    if (!form.price.trim() || !Number.isFinite(Number(form.price)) || Number(form.price) < 0)
      nextErrors.price = "Enter a valid price."
    if (!form.category) nextErrors.category = "Choose a category."
    if (!form.condition) nextErrors.condition = "Choose a condition."
    if (form.description.trim().length < 20)
      nextErrors.description = "Add at least 20 characters of detail."
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return
    // Listing persistence will be added here when the backend is ready.
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
                        value={form.price}
                        onChange={(event) =>
                          setForm({ ...form, price: event.target.value })
                        }
                        placeholder="0"
                      />
                    </div>
                  </Field>
                  <Field label="Category" error={errors.category}>
                    <select
                      value={form.category}
                      onChange={(event) =>
                        setForm({ ...form, category: event.target.value })
                      }
                    >
                      <option value="">Choose one</option>
                      {categories.slice(1).map((category) => (
                        <option key={category}>{category}</option>
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
                    value={form.description}
                    onChange={(event) =>
                      setForm({ ...form, description: event.target.value })
                    }
                    placeholder="Tell students about your item..."
                  />
                </Field>
                <Field label="General pickup location">
                  <input
                    value={form.location}
                    onChange={(event) =>
                      setForm({ ...form, location: event.target.value })
                    }
                  />
                </Field>
              </div>
            </section>
            <div className="form-actions">
              <Link className="button button-ghost" to="/browse">
                Cancel
              </Link>
              <button className="button button-dark" type="submit" disabled title="Publishing is not available yet">
                Publish listing
              </button>
            </div>
          </form>
        </div>
      </main>
    </ListingLayout>
  )
}
