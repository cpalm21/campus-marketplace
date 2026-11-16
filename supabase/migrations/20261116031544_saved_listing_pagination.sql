-- Old manual imports may omit the optional timestamp. Pagination requires a stable value.
UPDATE public.favorites SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
ALTER TABLE public.favorites ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX favorites_saved_feed_idx
    ON public.favorites (user_id, created_at DESC, listing_id DESC);
CREATE INDEX favorites_listing_idx ON public.favorites (listing_id);
