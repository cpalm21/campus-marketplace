-- Disposable CI database only. Superuser/CREATEROLE is needed for the probe role.
-- Roll back role, grants, and fixtures even on success; ON_ERROR_STOP makes failures fail CI.
BEGIN;
CREATE ROLE marketplace_api_probe NOLOGIN;
INSERT INTO public.users(firebase_uid, email, university_id)
SELECT 'rls-' || gen_random_uuid(), gen_random_uuid() || '@udel.edu', university_id
FROM public.universities WHERE email_domain = 'udel.edu'
RETURNING user_id AS probe_user_id \gset
INSERT INTO public.listings(user_id, category_id, listing_name, price, status)
SELECT :'probe_user_id', category_id, 'RLS probe', 1, 'Available'
FROM public.categories WHERE name = 'Furniture';

DO $$
BEGIN
    IF (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relrowsecurity
          AND c.relname IN ('universities', 'users', 'categories', 'listings', 'listing_images',
                           'favorites', 'transactions', 'reviews', 'messages')) <> 9 THEN
        RAISE EXCEPTION 'All nine marketplace tables must enable RLS';
    END IF;
END $$;

GRANT USAGE ON SCHEMA public TO marketplace_api_probe;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO marketplace_api_probe;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO marketplace_api_probe;
SET LOCAL ROLE marketplace_api_probe;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.users) OR EXISTS (SELECT 1 FROM public.listings) THEN
        RAISE EXCEPTION 'Direct client role can read private marketplace data';
    END IF;
    BEGIN
        INSERT INTO public.categories(name) VALUES ('unauthorized-client-write');
        RAISE EXCEPTION 'Direct client role can write marketplace data';
    EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'RLS rejects direct client writes as expected';
    END;
END $$;
ROLLBACK;
