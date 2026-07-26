# NoiseFile — session notes, 2026-07-25

Written because George is running ~8 parallel sessions across 48 hours and
needs this to survive the session, not just his memory of it. Everything
below was discussed, decided, or flagged in this conversation. Nothing here
has been built into the app yet except what's noted as done.

## What actually got built and shipped this session

- `legal-corpus/raw/{County}/{City}/{core-noise-ordinance,construction,animals,complaints-enforcement,other}/`
  — raw official-source acquisition for all 15 target cities. 52 files
  downloaded (real government PDFs or faithful HTML/PDF renderings of
  official code-portal pages), 1 blocked (URL-only, no file), 9 flagged
  `needs_review` (usually: only reachable via an aging Wayback Machine
  capture, or a jurisdiction/authority question that isn't confirmed by an
  official source yet).
- `legal-corpus/manifest.json` — 62 entries, one per source, each with city,
  county, topic, official title, issuing authority, chapter/section, canonical
  URL, local file path, format, SHA-256, access date, effective date (only
  when the source displayed one), geographic scope, authority level
  (city/county/JPA/contractor), and an acquisition note.
- `legal-corpus/acquisition-report.md` — city-by-city checklist, what's
  downloaded vs. URL-only vs. blocked, and every county-involvement /
  ambiguous-authority finding (see below — this is the part that actually
  matters for correctness, not the file-sorting).
- Committed and pushed to `main` at `25e4ce4`.
- `README.md` — added an "Ordinance coverage pipeline" section explaining the
  two-stage model (raw acquisition → structured catalog) and linking to the
  new corpus.

## The architecture discussion (this is the part to not lose)

George asked, correctly, how the app is supposed to give a fast answer on a
phone without a 2-5 minute AI round-trip, and whether the "origin" should be
Google Drive.

**Answer, and it turned out to already be the documented plan in
`docs/ORDINANCE_LIBRARY.md` and `docs/PRODUCT_BRIEF.md` before this
conversation even started** — George had independently arrived at the same
design the app already commits to:

- Not Drive. Drive is not a queryable runtime data layer.
- This is a bounded lookup problem, not an evolving-legal-field reasoning
  problem, per George: "It's either noisy or not, it's either the dog is
  barking or not, it's either construction is happening or not." Roughly a
  few hundred discrete facts total across 15 cities.
- Two-stage pipeline:
  1. Offline, one-time (or periodically re-run) extraction: raw ordinance
     text → structured fact `{city, category, rule, hours/dB/thresholds,
     source_section, source_url}`, human-verified against the source.
  2. Ship the structured result as a static packet
     (`app/src/main/assets/rules/catalog-v1.json`) bundled in the app. Runtime
     = an exact `jurisdictionId` + `noiseType` dictionary lookup, no network
     call, no LLM call, milliseconds.
- Where AI is actually useful: (a) the offline extraction pass itself
  (already how `catalog-v1.json`'s existing San Jose entries were presumably
  built), and (b) optionally, at runtime, phrasing an *already-resolved* fact
  into natural language or drafting complaint text — never re-deriving the
  rule live. The city/category packet goes before variable incident data in
  any such prompt so it can be prompt-cached (this is already written into
  `ORDINANCE_LIBRARY.md`'s "Optional model layer" section).
- Missing coverage must return "unavailable," never a nearby-city guess or a
  model-generated answer. This was already a hard product decision before
  today; it lines up with George's own framing.
- Refresh cadence: periodic re-acquisition (quarterly-ish, or on an
  amendment-detection trigger), diffed, reviewed, then a new catalog version
  ships with an app update. Not live.

**Open question raised but not settled:** whether George wants a lightweight
backend cache in front of the bundled JSON (for over-the-air catalog updates
without a full app release) or whether shipping a new catalog version with
every app update is fine for now. Not decided this session.

## The "is this a Sonnet-appropriate task" tangent

George wondered if this task was designed as an eval for Sonnet specifically,
and separately whether Sonnet was the right model for what looked like mostly
folder-sorting and file-naming work. Take, for the record:

- No visibility either way into whether a given conversation is an eval —
  nothing here read like one (real repo, real government sites, real bot
  blocks to work around).
- The mechanical part (consistent folder structure, filenames, JSON schema)
  is trivial for any current model. The part that actually required judgment
  and was worth getting right:
  - Recognizing Concord's PD flyer cited an old pre-recodification code
    section number (62-201) that doesn't match the current
    codepublishing.com numbering (18.150.130), and flagging the mismatch
    instead of assuming they're the same law.
  - Distinguishing "county explicitly states it serves this city" (Sonoma
    County ↔ Santa Rosa, Concord's own PD flyer ↔ Contra Costa, Antioch's own
    city page ↔ Contra Costa) from "a search result mentioned a county number
    near this city's name but no official source confirms it" (Richmond —
    flagged `needs_review`, not asserted).
  - Not labeling a source `downloaded` when it only came from a stale Wayback
    capture (SF311's page is a 2023 capture; flagged, not hidden).
  - Silicon Valley Animal Control Authority (SVACA) is a multi-city joint
    powers authority, not Santa Clara County government and not a city
    department — that's its own authority bucket, and conflating it with
    "county" would have been wrong for Sunnyvale and Santa Clara both.
- Conclusion: this didn't need a bigger model, it needed not lying about
  confidence levels. That's a discipline thing, not a raw-capability thing.

## County / authority findings worth remembering when building the catalog

These are the facts that will directly shape how `catalog-v1.json` routes
animal-noise complaints per city — don't re-derive them from scratch later,
they're already in `acquisition-report.md`:

- **County-administered (explicit):** Santa Rosa → Sonoma County Animal
  Services. Concord → Contra Costa County (city's own PD flyer states the
  contract). Antioch → Contra Costa County (city's own page states it).
- **County-administered but NOT confirmed by an official city-side source:**
  Richmond → possibly Contra Costa County, flagged `needs_review`.
- **Joint powers authority (neither city nor county):** Sunnyvale and Santa
  Clara → SVACA.
- **Private nonprofit contractor (neither city nor county):** Daly City and
  San Mateo → Peninsula Humane Society & SPCA for general animal control —
  but PHS explicitly does NOT take barking-dog-noise-only complaints; those
  route to city police non-emergency in both cities. This is a real trap for
  the catalog: routing a Daly City or San Mateo barking-dog complaint to PHS
  would be wrong.
- **City runs its own animal-noise function, no delegation found:** San Jose,
  San Francisco (consolidated, own Animal Care and Control), Oakland, Fremont
  (Tri-City Animal Shelter, a multi-city shared service under Fremont PD, not
  a county), Hayward, Vallejo, Berkeley.

## Per-city gaps to close before the catalog can claim full coverage

- **Santa Rosa** — no standalone construction-hours code section confidently
  located; secondary sources cite hours, code section not pinned down.
- **Daly City** — the 60/50 dBA figures cited by secondary sources weren't
  found inside the 3-section chapter actually captured; a separate
  zoning/performance-standards chapter probably has them and wasn't located.
- **Vallejo** — Chapter 7.36 (Animal Nuisance) repeatedly failed to render
  from Municode in this pass; general construction-hours provision not
  confidently separated from the noise-disturbance chapter already captured.
- **San Jose** — the official barking-dog abatement petition form
  (sanjoseca.gov) is fully blocked (Akamai) with no Wayback snapshot; URL-only
  in the manifest.
- Several `needs_review` items are just "re-fetch this live and confirm the
  Wayback capture still matches" — not missing data, just unconfirmed
  freshness (SF311, SFDPH, SVACA pages, Vallejo animal control, Vallejo code
  enforcement).

## Immediate next step, as of end of session

Extraction pass: turn the 15 raw core-ordinance sources (plus construction/
animal/complaint material) into structured `catalog-v1.json` entries for the
14 not-yet-covered cities, following the exact schema already established by
the San Jose entries and the contract in `docs/ORDINANCE_LIBRARY.md`. George
wants to build first, then run real test scenarios against it (e.g. "2am
construction in Oakland," "dog barking 15 minutes in Concord," "10:30pm music
in Daly City") and fix whatever the scenarios expose — not review line-by-line
before anything's testable.
