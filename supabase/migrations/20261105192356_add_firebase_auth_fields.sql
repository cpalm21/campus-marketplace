-- ============================================
-- FIREBASE AUTHENTICATION SUPPORT
--
-- Brings the schema in line with the registration flow in
-- edu.udel.campusmarketplace.user. Firebase owns credentials; this database
-- owns the profile. The only link between them is users.firebase_uid.
--
-- Nothing is dropped. password_hash, first_name and last_name stay as
-- columns and merely stop being mandatory, so this migration is reversible
-- and no existing data is lost.
-- ============================================


-- ============================================
-- USERS: firebase_uid
--
-- The lookup key for every authenticated request. Added permissively and
-- tightened afterwards so the migration also succeeds on a database that
-- already holds rows, where a NOT NULL column cannot be added outright.
-- ============================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);

-- Pre-Firebase accounts, if any exist, get a placeholder that no real
-- Firebase UID can match, so they can never be signed in to by accident.
-- Updates zero rows on an empty table.
UPDATE users
SET firebase_uid = 'legacy_' || user_id
WHERE firebase_uid IS NULL;

ALTER TABLE users
    ALTER COLUMN firebase_uid SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_firebase_uid_key UNIQUE (firebase_uid);


-- ============================================
-- USERS: relax the columns Firebase registration does not fill
--
-- password_hash is kept for reversibility but will never be written, since
-- Firebase holds credentials. first_name and last_name become optional until
-- the signup form collects them.
-- ============================================

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL,
    ALTER COLUMN first_name DROP NOT NULL,
    ALTER COLUMN last_name DROP NOT NULL;


-- ============================================
-- UNIVERSITIES: email_domain
--
-- Lets university_id be derived from the verified email address instead of
-- trusting a signup dropdown. Which schools may register becomes data:
-- adding one is an insert, and revoking one is a delete.
-- ============================================

ALTER TABLE universities
    ADD COLUMN IF NOT EXISTS email_domain VARCHAR(100);

UPDATE universities
SET email_domain = 'udel.edu'
WHERE university_name = 'University of Delaware'
  AND email_domain IS NULL;

UPDATE universities
SET email_domain = 'temple.edu'
WHERE university_name = 'Temple University'
  AND email_domain IS NULL;

-- Any university added outside these migrations gets an unroutable
-- placeholder rather than blocking the NOT NULL below. A '.invalid' domain
-- can never appear in a real email address, so such a row simply accepts
-- nobody until someone sets its true domain.
UPDATE universities
SET email_domain = lower(replace(university_name, ' ', '-')) || '.invalid'
WHERE email_domain IS NULL;

ALTER TABLE universities
    ALTER COLUMN email_domain SET NOT NULL;

-- Indexed on lower(...) to match how UserRepository queries it, which makes
-- the uniqueness case-insensitive too: 'UDel.edu' cannot be added alongside
-- 'udel.edu'.
CREATE UNIQUE INDEX IF NOT EXISTS universities_email_domain_lower_key
    ON universities (lower(email_domain));
