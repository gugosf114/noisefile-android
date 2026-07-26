# NoiseFile — session notes, 2026-07-25

George is running like 8 sessions at once across 48 hours, so this exists
because his memory of "what did that session do" isn't going to hold. Not a
transcript — this is the takeaway. Go read `acquisition-report.md` if you
want the receipts.

## What got built

Scraped all 15 target cities' actual noise ordinances off their real
government sites — Municode, amlegal, eCode360, city law libraries,
whatever each city happens to use — and dumped them raw into
`legal-corpus/raw/{County}/{City}/...`. 52 files actually downloaded, 1 dead
end (San Jose's petition form, Akamai just says no, no Wayback copy either),
9 marked `needs_review` because something about them isn't fully nailed down
(stale archive copy, or a jurisdiction claim I couldn't confirm — see below).

Every file has a SHA-256 and the exact government URL it came from, logged in
`manifest.json`. Pushed to main at `25e4ce4`, then README + this file at
`52e67a8`.

## The architecture thing — this is the part that matters

George asked how the app answers fast on a phone instead of making someone
wait through an AI round-trip, and whether the source of truth should just be
Google Drive.

Short version: no, not Drive, and it turns out the repo already had this
figured out before either of us said a word — `docs/ORDINANCE_LIBRARY.md`
already spells out almost exactly what George re-derived cold: it's not a
"legal reasoning" problem, it's a lookup table. Noisy or not. Dog barking or
not. Construction happening or not. A few hundred facts, total, across 15
cities. You don't need an AI thinking live for that, you need:

1. Do the extraction once, offline — raw ordinance → structured fact
   (city, category, hours/dB, source section, source URL) — human checks it
   against the actual text.
2. Ship that as a flat JSON file in the app
   (`app/src/main/assets/rules/catalog-v1.json`). At runtime it's a
   dictionary lookup. No network call. No model call. Milliseconds.

AI's job is stage 1 (and maybe later, phrasing an already-known answer into
normal sentences — never re-deciding the rule live). If a city/category isn't
in the catalog, the app says "don't have this yet," full stop — it does not
guess from a nearby city or make something up. That was already the rule
before today.

Not settled: whether catalog updates ship only via full app releases, or
whether there's a backend cache so updates can go out without a store push.
Didn't decide, just flagged.

## "Was this built for Sonnet" tangent

George's spidey sense said maybe this was an eval, and separately wondered if
Sonnet specifically was the right fit since it looked like glorified
file-sorting. My answer, for the record: the folder/naming part is nothing,
any model does that fine. What actually took care:

- Concord's own police-department flyer cites an old code section (62-201)
  that doesn't match the site's current numbering (18.150.130) — caught that
  and flagged it instead of assuming old cite = current law.
- Told apart "city or county explicitly says the county handles this" (Santa
  Rosa/Sonoma, Concord/Contra Costa, Antioch/Contra Costa — all stated
  directly by an official source) from "a search result mentioned a county
  number near this city's name and nothing official backs it up" (Richmond —
  flagged, not asserted).
- Didn't mark something "downloaded" when it only came from a 2023 Wayback
  snapshot — said so instead.
- SVACA (Sunnyvale + Santa Clara's animal control) is a multi-city joint
  authority — not the county, not either city. Calling that "county" would
  have been a wrong answer baked into the data forever.

None of that needed a bigger model. It needed not lying about how sure I was.
That's the whole difference.

## Who handles animal-noise complaints, city by city — don't re-derive this later

- **County runs it, and says so directly:** Santa Rosa (Sonoma County),
  Concord (Contra Costa, per the city's own PD flyer), Antioch (Contra Costa,
  per the city's own page).
- **Probably county, not confirmed by the city itself:** Richmond — Contra
  Costa's number showed up in a Richmond-focused search but no official
  Richmond source says so. Flagged, not asserted.
- **Joint powers authority, not county:** Sunnyvale + Santa Clara → SVACA.
- **Private contractor, not county — and it matters:** Daly City + San Mateo
  use Peninsula Humane Society for general animal control, but PHS will not
  touch a barking-dog-noise-only complaint — that goes to city police
  instead. If the catalog ever routes a Daly City barking complaint to PHS,
  that's a bug, not a fact.
- **City just handles it themselves, no delegation found:** San Jose, San
  Francisco, Oakland, Fremont (Tri-City Animal Shelter — shared service under
  Fremont PD, still not a county), Hayward, Vallejo, Berkeley.

## Loose ends per city

- **Santa Rosa** — couldn't pin a specific construction-hours code section.
  Secondary sources give hours, code itself didn't cough up the section.
- **Daly City** — the 60/50 dBA numbers everyone cites aren't in the 3-section
  chapter I actually pulled. Probably in a zoning chapter I didn't find.
- **Vallejo** — Ch. 7.36 (animal nuisance) wouldn't render out of Municode no
  matter how I asked. Construction hours also not cleanly separated from the
  noise chapter I did get.
- **San Jose** — the barking-dog abatement form is just gone, Akamai-blocked,
  no archive copy. URL only.
- A handful of `needs_review` items are just "go re-check this live," nothing
  actually missing — SF311, SFDPH, SVACA, both Vallejo pages.

## What's next

Turn the raw sources into `catalog-v1.json` entries for the 14 cities that
aren't San Jose, same schema San Jose already uses. George's call: build it,
then throw real scenarios at it — 2am construction in Oakland, a dog barking
15 minutes in Concord, music at 10:30pm in Daly City — and fix whatever
breaks. Not a line-by-line review before there's anything to actually test.
