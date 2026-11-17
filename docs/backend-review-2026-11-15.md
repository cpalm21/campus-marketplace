# Backend review — November 15, 2026

## Reviewed state

The review fetched GitHub and examined `main` at `89ccf73` and open create-listing
PR [#20](https://github.com/cpalm21/campus-marketplace/pull/20) at `a487dbc`.
This follow-up branches from #20 so it preserves that work and its create contract.
Merge #20 first, then retarget this follow-up to main; the stacked diff isolates
the additional backend work. Neither PR is merged by this task.

| Recent change | Assessment and backend implication |
| --- | --- |
| [#14: browsing, filtering, and sorting](https://github.com/cpalm21/campus-marketplace/pull/14) | Browse uses in-memory sample listings. Needs database search, canonical filters, and stable pagination across price ties. |
| [#16: Firebase schema](https://github.com/cpalm21/campus-marketplace/pull/16) | Adds UID/domain fields and relaxes profile names/passwords. Reuse this migration rather than create duplicate columns or constraints. |
| [#17: listing form](https://github.com/cpalm21/campus-marketplace/pull/17) | Form validates locally and publishing is disabled. Needs a backend create contract and a future image upload flow. |
| [#18: app-wide auth](https://github.com/cpalm21/campus-marketplace/pull/18) | Tracks client identity and sign-out. Backend must independently verify tokens, registered profiles, and campus eligibility. |
| [#19: details and saved pages](https://github.com/cpalm21/campus-marketplace/pull/19) | Reads sample data; saves are browser-local. Needs real detail reads and user-specific persistence with privacy boundaries. |
| [#20: create-listing backend, open](https://github.com/cpalm21/campus-marketplace/pull/20) | Adds POST with a title/categoryId contract and 29 mocked controller tests. Needs real SQL verification, a GET for Location, and the remaining listing workflows. |

## Completed follow-up

- Preserved POST title, required condition, canonical category IDs, response shape,
  and omission of seller IDs. Retained its create validation/controller coverage.
- Added campus-scoped listing detail, text search, condition/category/status/price
  filters, newest/price ordering, and cursors that include ordering tie-breakers.
- Added seller-scoped listing browse, edits, status changes, and soft removal.
  Reserved listings must be released before editing/removal; Sold is terminal.
- Added up to four ordered HTTPS image links, atomic replacement/clearing,
  ownership/lifecycle checks, and preservation of unchanged image metadata IDs.
- Added private saved-listing reads, duplicate-safe saves, and stale-item removal.
- Shared participating-profile checks across listing, image, and favorite services.
  Profile email and university-domain eligibility are checked on each request.
- Made first registration insertion tolerate simultaneous requests for one Firebase
  UID without creating duplicate profiles.
- Added follow-up migrations for removal/timestamps/indexes/RLS, preserving the
  merged auth migration and existing records.
- Added consistent application/invalid-input/database error responses, explicit UTC
  timestamp mapping, real database tests, competing-transaction tests, and a CI RLS
  probe. Backend CI now supports PRs targeting feature branches as well as main.

The complete routes, JSON contracts, invariants, errors, rollout requirements,
test instructions, and practical limitations are in [backend-api.md](backend-api.md).

## Remaining work identified

| Follow-up | Scope and dependency |
| --- | --- |
| Connect the frontend | Replace sample data/localStorage with these APIs, load categories from reference data, attach a refreshed Firebase token, and render loading/errors/next pages. Backend endpoints alone do not change existing page behavior. |
| Reconcile form choices | Frontend currently offers Like new/Excellent and sample category names that do not match the schema. Use canonical backend values; adding new categories or conditions is a separate product/schema decision. |
| Image uploads | The new API stores HTTPS metadata only. Implement storage permissions and file upload before submitting the form's local blob previews. |
| Buyer/seller messaging | Needs conversation membership, participant privacy, read-state semantics, pagination, and handling of removed listings. No new messaging API is introduced here. |
| Purchase/transaction workflow | Needs agreed buyer identity, reservation ownership/expiry, completion/cancellation rules, idempotency, and payment requirements. Current seller availability states do not process money. |
| Profile editing | Names/profile images and changes to a Firebase account's email/university need an explicit reconciliation policy. This follow-up rejects token/profile mismatches rather than silently moving an account between campuses. |
| Search scale | Measure query plans at realistic volumes before adopting trigram/full-text indexes. Current substring search is parameterized and bounded by campus but not backed by a dedicated text index. |

No hosted migrations, frontend rewiring, payment integration, or messaging were
performed as part of this backend follow-up.

## Validation recorded for this review

- 112 Maven tests passed with no failures, errors, or skips against disposable
  PostgreSQL 16, including the existing create-controller tests. The final run used
  `-Duser.timezone=America/New_York` to check explicit UTC timestamp handling.
- The entire migration chain applied cleanly to a fresh database and the sample
  seed loaded successfully afterward.
- A second database was initialized with the merged migrations and seed first,
  then given legacy null listing/save timestamps. Both follow-up migrations
  applied, retained all five users/listings and three favorites, preserved a legacy
  password hash, and repaired the timestamps.
- The direct-client RLS probe passed on both databases.
- Git diff whitespace checks passed. Hosted schema/data was not changed.
