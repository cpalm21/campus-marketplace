-- Firebase columns and domain data are owned by the merged 20261105192356 migration.
-- Normalize legacy missing timestamps so keyset pagination always has an ordering value.
UPDATE public.listings SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE public.listings SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE public.listings ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE public.listings ALTER COLUMN updated_at SET NOT NULL;

-- Removal from the marketplace must not destroy transaction/message history.
ALTER TABLE public.listings ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX listings_feed_idx ON public.listings (status, created_at DESC, listing_id DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX listings_user_idx ON public.listings (user_id, created_at DESC, listing_id DESC);
CREATE INDEX listings_price_idx ON public.listings (status, price, created_at DESC, listing_id DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX listing_images_listing_idx ON public.listing_images (listing_id, image_order);
CREATE INDEX listings_category_idx ON public.listings (category_id);
CREATE INDEX users_university_idx ON public.users (university_id);

-- All access goes through the Firebase-authenticated Java backend using a
-- trusted database role. No direct Data API policies are granted to clients.
ALTER TABLE public.universities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listing_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
