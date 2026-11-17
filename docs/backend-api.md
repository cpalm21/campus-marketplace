# Marketplace backend API

This document describes the backend on `feat/marketplace-backend-workflows`,
building on create-listing PR [#20](https://github.com/cpalm21/campus-marketplace/pull/20).
The recent merge review and implementation decisions are in
[backend-review-2026-11-15.md](backend-review-2026-11-15.md).

## Authentication and access

All listing, image, and favorites endpoints require a Firebase ID token:

```http
Authorization: Bearer <Firebase ID token>
```

The token is verified before the service is invoked. Register the account with
`POST /api/users/register` first. Registration remains HTTP 200 for both a new
profile and a repeated call, preserving the existing frontend contract.
Concurrent first registrations for one Firebase UID converge on one profile;
an email already assigned to a different UID returns 409. Registration accepts
unverified email so it can run immediately after signup.

Listing and favorites reads require a registered participating-university profile
whose stored email matches the token, ignoring case. Its university must still
own the email domain. Mutations additionally require `email_verified` in the token.
After verification, refresh the token or sign out and back in. University scope,
seller ownership, and favorites ownership come from the stored profile; clients
cannot select another user's ID or university in a request.

Public endpoints `GET /api/categories` and `GET /api/universities` are unchanged.
Use their IDs and canonical names rather than the frontend's sample choices.

## Routes

| Method | Route | Success | Purpose |
| --- | --- | --- | --- |
| POST | `/api/listings` | 201 + Location | Create a listing; request contract from PR #20 is preserved. |
| GET | `/api/listings` | 200 | Search and browse your university's listings. |
| GET | `/api/listings/{listingId}` | 200 | Read one visible listing. |
| PUT | `/api/listings/{listingId}` | 200 | Replace your available listing's editable details. |
| PATCH | `/api/listings/{listingId}/status` | 200 | Change your listing's availability. |
| DELETE | `/api/listings/{listingId}` | 204 | Hide your available listing while retaining related records. |
| GET | `/api/users/me/listings` | 200 | Search your own non-deleted listings, across statuses by default. |
| GET | `/api/listings/{listingId}/images` | 200 | Read ordered image metadata. |
| PUT | `/api/listings/{listingId}/images` | 200 | Replace all image metadata on your available listing. |
| GET | `/api/users/me/favorites` | 200 | Read your saved listings. |
| PUT | `/api/users/me/favorites/{listingId}` | 204 | Save another seller's eligible listing; no body. |
| DELETE | `/api/users/me/favorites/{listingId}` | 204 | Remove your saved item, including stale or absent saves. |

Path IDs are UUIDs. Responses omit Firebase credentials, password hashes, email,
university IDs, and internal seller user IDs.

## Listing details

POST and PUT accept the same body. PUT replaces all editable details; omitted
optional fields are cleared. Seller ID, status, listing ID, and timestamps cannot
be set through this body.

```json
{
  "title": "Adjustable desk lamp",
  "price": 12.50,
  "categoryId": 1,
  "condition": "Good",
  "description": "Adjustable lamp, works well.",
  "location": "North Campus"
}
```

| Field | Rule |
| --- | --- |
| `title` | Required; trimmed, 1–50 characters. This maps to `listings.listing_name`. |
| `price` | Required; 0–99999999.99, at most two decimal places after ignoring trailing zeros. Free items are valid. |
| `categoryId` | Required; must refer to an existing category. |
| `condition` | Required; exactly `New`, `Great`, `Good`, `Fair`, or `Poor`, after trimming. |
| `description` | Optional; trimmed, at most 500 characters; blank becomes null. |
| `location` | Optional; trimmed, at most 100 characters; blank becomes null. |

A listing response is:

```json
{
  "listingId": "11111111-1111-4111-8111-111111111111",
  "categoryId": 1,
  "title": "Adjustable desk lamp",
  "price": 12.50,
  "condition": "Good",
  "description": "Adjustable lamp, works well.",
  "location": "North Campus",
  "status": "Available",
  "createdAt": "2026-11-15T20:30:00Z"
}
```

The Location returned by POST resolves through the detail endpoint. Only the
seller can update, change status, delete, or replace image metadata.

### Availability and removal

PATCH status uses `{"status":"Reserved"}`. Available may become Reserved or Sold;
Reserved may become Available or Sold. Repeating the current status is a no-op.
Sold is terminal. Only Available listings can be edited, removed, or have images
replaced; release a reservation first. These are seller-managed availability
states. Marking Sold does not initiate payment or create a transaction record.

Deletion sets `deleted_at`. Listing images, favorites, messages, transactions,
and reviews are not hard-deleted. Deleted listings are omitted from browse,
detail, image reads, seller listings, and favorites. A repeated listing DELETE
returns 404. Reserved or Sold deletion returns 409.

Mutations lock the listing row in a transaction before checking ownership and
status, preventing an edit, favorite insertion, or image replacement from racing
past a committed sale or deletion. This serializes competing writes; concurrent
edits of an Available listing otherwise use the last committed replacement.

## Search, filtering, and pagination

Both listing browse routes accept:

| Parameter | Rule/default |
| --- | --- |
| `q` | Optional trimmed text, maximum 100 characters. Case-insensitive literal substring match over title, description, pickup location, and category name. `%`, `_`, and backslash are literal text. |
| `categoryId` | Optional positive integer. An unknown positive ID yields an empty page. |
| `condition` | Optional exact canonical condition. |
| `status` | Available, Reserved, or Sold. `/api/listings` defaults to Available; `/api/users/me/listings` defaults to all three. |
| `minPrice`, `maxPrice` | Optional inclusive bounds, same price range/precision as listings. Minimum must not exceed maximum. |
| `sort` | `newest` (default), `priceAsc`, or `priceDesc`. |
| `limit` | Default 20; integer between 1 and 100. |
| `cursor` | Opaque next-page position from the preceding response. |

Filters combine with AND. Search uses bound SQL parameters and escapes LIKE
metacharacters. Sort expressions come from a fixed allowlist. There is no public
parameter for selecting a seller or university.

```json
{"items": [{"listingId": "...", "title": "..."}], "nextCursor": "..."}
```

`items` contains full listing responses. A null cursor means the end. Keep the
same normalized filters, sort, campus, and seller scope on the next request;
changing any of those requires restarting without a cursor. The page size may
change. An invalid or mismatched cursor returns 400.

Newest orders by creation time descending, then UUID descending. Price sorting
orders by price in the requested direction, then creation time and UUID descending.
The cursor includes these tie-breakers so equal prices or timestamps do not skip
or repeat rows. This is a live feed rather than a snapshot: edits to price or status,
saves/removals, and deletions may change subsequent pages. No total-count query is
performed. Literal substring search currently scans candidate text; full-text or
trigram search is a future optimization to benchmark against actual data volume.

Example (the shell variable holds the token without embedding it in source):

```bash
curl --get 'http://localhost:8080/api/listings' \
  --header "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  --data-urlencode 'q=desk' \
  --data-urlencode 'sort=priceAsc' \
  --data-urlencode 'maxPrice=50' \
  --data-urlencode 'limit=20'
```

## Image metadata

PUT images accepts:

```json
{"imageUrls":["https://example.com/cover.jpg","https://example.com/side.jpg"]}
```

Supply zero to four distinct HTTPS URLs, each at most 500 characters, without
embedded credentials or fragments. Surrounding whitespace is removed. The order
in the array is the display order, starting at 1; the first image is the cover.
An empty array clears the images. A missing/null array, null element, duplicate,
invalid URL, or more than four URLs returns 400 without replacing existing metadata.

GET and PUT return arrays of
`{"listingImageId":"...","imageUrl":"https://...","imageOrder":1}`.
Replacing the same ordered URLs preserves existing metadata IDs. A changed set is
replaced atomically and advances the listing's internal `updated_at` timestamp.
Image IDs are metadata IDs rather than permanent attachment identities.

This API stores links only. It never fetches the URLs and does not upload, resize,
delete, or validate remote image bytes. The frontend's local `blob:` previews
cannot be persisted; a separate upload/storage flow must produce HTTPS URLs.

## Saved listings

Only Available or Reserved listings belonging to another seller in your university
may be saved. Saving your own listing returns 403. Saving a Sold listing returns
409, including a repeated save after that item became Sold. For an eligible listing,
repeated and simultaneous saves create one favorite and preserve the original
save time. Already-saved Sold listings stay visible with their current status.

GET favorites takes `limit` (default 20, 1–100) and `cursor`. It returns:

```json
{"items":[{"listing":{"listingId":"...","title":"..."},"savedAt":"2026-11-15T20:30:00Z"}],"nextCursor":null}
```

Nested listings contain the full listing response. Favorites order by save time
descending and listing UUID descending. The next-page cursor includes both. Listing
details are current, rather than a snapshot taken at save time. Deleted and
other-university listings are hidden but their stale favorite rows can still be
removed. Removing an absent favorite succeeds, never deletes the listing, and
never removes another user's saved row.

## Errors and time handling

Application rejections and malformed JSON/typed parameters return
`{"message":"..."}`. Messages are explanatory, not stable programmatic codes.

| Status | Meaning |
| --- | --- |
| 400 | Invalid fields, query, cursor, image metadata, JSON, or typed parameter. |
| 401 | Missing, malformed, expired, or invalid Firebase token. |
| 403 | Unverified mutation, missing/ineligible profile, mismatched email, or another seller's mutation within the same campus. |
| 404 | Unknown/deleted listing or a listing belonging to another university. |
| 409 | Invalid lifecycle operation, saving Sold, or a database constraint conflict. |
| 503 | Firebase credentials not configured or a database operation unavailable. |

SQL and database credentials are not returned to callers. Errors are logged on the
server. Reference-data endpoints retain their existing unavailable-data message.

Existing database timestamp columns have no timezone. Backend connections set their
SQL session timezone to UTC, and repository mappers explicitly interpret stored
timestamps as UTC. API listing and favorite timestamps are ISO-8601 instants with Z.
Image replacement and lifecycle writes use database time. Historical rows are
assumed to have been written in UTC; this change does not reinterpret old local-time
imports or change the database column types.

## Database rollout and verification

Do not edit the applied `20261105192356_add_firebase_auth_fields.sql` migration.
The two follow-up migrations run after it:

| Migration | Changes |
| --- | --- |
| `20261116031531_marketplace_listing_workflows.sql` | Fill legacy null listing timestamps, make timestamps required, add `deleted_at`, add feed/price/seller/category/image and campus-join indexes, enable RLS on all nine marketplace tables. |
| `20261116031544_saved_listing_pagination.sql` | Fill null save timestamps, require them, and add user/save-order and listing-reference indexes. |

No Firebase columns are recreated and no existing rows are deleted. Timestamp
backfills use migration time where necessary, so preexisting rows without a time
cannot retain an unknown historical ordering. These are normal transactional DDL
migrations; index creation can take locks and should be scheduled appropriately
if a future deployment has large tables. Apply them before starting this backend
version. The existing deployment workflow still requires the `database_skeleton`
branch; creating this PR does not deploy the hosted database.

RLS has no direct-client policies. The Java backend connects as a trusted
table-owner/BYPASSRLS role and enforces Firebase authorization in its services.
Direct Supabase Data API clients are denied access to marketplace rows, even if
they have table grants. Do not use the backend database credential in a browser.

With a disposable PostgreSQL database initialized from all migrations:

```bash
cd backend
DATABASE_URL='jdbc:postgresql://localhost:5432/marketplace_test' \
DATABASE_USERNAME=postgres DATABASE_PASSWORD=postgres ./mvnw test
```

Without DATABASE_URL, SQL integration tests are skipped and credential-free
controller/unit tests still run. With DATABASE_URL, tests create temporary users,
listings, images, and favorites. Most roll back; concurrency tests commit isolated
fixtures and delete them afterward. Never run them against production.

Backend CI uses PostgreSQL 17, applies every migration, runs the full Maven suite,
then runs `backend/src/test/resources/rls-check.sql` with ON_ERROR_STOP. The RLS probe
temporarily creates a client role with table grants and private rows, checks all
nine RLS flags and blocked reads/writes, then rolls the entire probe back. It needs
CREATEROLE/superuser privileges and is intended only for disposable databases.

Integration tests cover CRUD/history retention, campus and ownership boundaries,
state transitions, literal search and combined filters, price/time ties, cursor
scope, seller listings, image order/validation/replacement, favorites privacy,
concurrent saves/registration, and writes waiting behind sale/deletion locks.
