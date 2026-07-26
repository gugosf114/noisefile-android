# NoiseFile Legal Corpus — Acquisition Report

Acquisition session date: 2026-07-25
Scope: 15 Bay Area cities. Acquisition only — no legal analysis, extraction, or interpretation was performed, and no application code was modified.

Total manifest entries: 62 (see `manifest.json`)

## Method notes (apply across all cities)

- Municode, American Legal (amlegal), eCode360, qcode/General Code, and Code Publishing Company (codepublishing.com, which now 301-redirects to `municipal.codes` under the same vendor) were all treated as official code portals.
- Several official portals (Municode, amlegal, eCode360-chapter-shells) serve their text as JavaScript-rendered content that a plain HTTP fetch cannot see. Where that happened, the actual official page was driven in a real, uninstrumented browser session so its own DOM/API responses could be read verbatim, then re-assembled into a clean archival PDF with a cover sheet. No text was paraphrased, summarized, or invented in this process — headings and body paragraphs were extracted as-is from the official page.
- Several `.gov` sites (sanjoseca.gov, sjpd.org, fremontpolice.gov, fremont.gov, sf311.org, sfdph.org, sunnyvale.ca.gov, santaclaraca.gov, svaca.com, vallejo.gov) returned bot-management blocks (Akamai/Cloudflare "Access Denied" or indefinite hangs) to every fetch method available in this environment. Where a recent Internet Archive Wayback Machine capture of the exact same official URL existed, that capture was used instead and is labeled with its capture date; the canonical URL recorded is always the live government URL, not the archive.org URL. Where no snapshot existed, the item is marked `blocked` or `needs_review` with the canonical URL preserved rather than guessed at.
- No file was accepted as source material if it was an error page, login page, empty JS shell, or search-results page.

## City-by-city checklist

| City | County | Core ordinance PDF | Construction | Animals | Complaints/Enforcement |
|---|---|---|---|---|---|
| San Jose | Santa Clara | ✅ Ch. 10.16 | ✅ Ch. 20.100 Pt.3 | ✅ Ch. 7.40 (+1 blocked form) | ✅ SJPD page |
| San Francisco | City & County of SF (consolidated) | ✅ Police Code Art. 29 | ✅ (within Art. 29) | ✅ 3 sources | ✅ 1 downloaded + 2 needs_review |
| Oakland | Alameda | ✅ Ch. 8.18 | ✅ Planning Code 17.120.050 | ✅ Oakland Animal Services | ✅ CPAB guide |
| Fremont | Alameda | ✅ Ch. 9.25 | ✅ Ch. 18.160 | ✅ Tri-City Animal Shelter | ✅ FPD page |
| Santa Rosa | Sonoma | ✅ Ch. 17-16 (4 articles) | ⚠️ needs_review | ✅ Sonoma Co. Animal Services | ✅ SRPD page |
| Hayward | Alameda | ✅ Ch.4 Art.1 Noise Regs | ✅ (embedded, Sec. 4-1.04) | ✅ City page | ✅ Code Enforcement |
| Concord | Contra Costa | ✅ Dev. Code 18.150.130(O) | ✅ (PD flyer) | ✅ (county contract, per PD flyer) | ✅ 2 sources |
| Sunnyvale | Santa Clara | ✅ Ch. 19.42.030 | ✅ Ch. 16.08.030 | ⚠️ needs_review (SVACA) | ✅ Neighborhood Complaint page |
| Santa Clara | Santa Clara | ✅ Ch. 9.10 (combined) | ✅ (within Ch. 9.10 Art. II) | ⚠️ needs_review (SVACA) | ✅ PD page |
| Concord/Vallejo etc. continued below | | | | | |
| Vallejo | Solano | ✅ Ch. 7.84 | ⚠️ needs_review | ⚠️ needs_review | ✅ Code Enforcement page |
| Berkeley | Alameda | ✅ Ch. 13.40 (14 sections) | ✅ official PDF | ✅ Animal Care Services | ✅ Noise Standards page |
| Richmond | Contra Costa | ✅ Ch. 9.52 | ✅ (within Ch. 9.52) | ⚠️ needs_review (county) | ⚠️ needs_review (county FAQ) |
| Antioch | Contra Costa | ✅ Secs. 5-17.01–.06 (combined) | ✅ (within same chapter) | ✅ 2 city sources (→ county contact) | ✅ Code Enforcement page |
| Daly City | San Mateo | ✅ Ch. 9.22 | ⚠️ not located separately | ✅ City FAQ (→ PHS contractor) | ✅ Contact DCPD page |
| San Mateo | San Mateo | ✅ Ch. 7.30 (13 sections, combined) | ✅ (within Ch. 7.30) | ✅ (routed via PD page) | ✅ PD page |

## Sources successfully downloaded

59 of 62 manifest entries have a local file (`downloaded` status), spanning 15 core-noise-ordinance PDFs (one per city, several combining construction/animal/enforcement material where the official code itself combines them), plus supporting construction, animal-noise, and complaint/enforcement material per city. Full detail with hashes and canonical URLs is in `manifest.json`.

## Official URL-only sources (no local file)

- **San Jose** — "Declaration and Petition for Abatement of a Public Nuisance" (sanjoseca.gov/Home/ShowDocument?id=52023): blocked by the site's Akamai edge on every fetch method tried, no Wayback snapshot exists for this exact document URL. Status: `blocked`.

## Missing categories / items flagged `needs_review`

- **Santa Rosa** — no standalone codified "construction hours" section was confidently located after checking the core noise chapter; secondary sources cite hours but the code section itself wasn't pinned down.
- **Sunnyvale / Santa Clara** — the shared animal-control authority (SVACA) page was reachable only via a ~3-month-old Wayback capture; live site returned Access Denied.
- **Vallejo** — the city's own Animal Control page came from an ~15-month-old Wayback capture (live site hung/timed out); a specific codified general-construction-hours provision was not confidently located separately from the noise-disturbance chapter already captured.
- **San Francisco** — SF311's noise-complaints page and SFDPH's Noise Enforcement Program page both returned live HTTP 504 errors from this environment; Wayback captures (2023-09-27 and 2024-10-08 respectively) were used instead and should be re-verified against the live pages before being relied on.
- **Richmond** — the Contra Costa County Noisy Animal Ordinance page does not itself list which incorporated cities it serves; a Richmond-specific search surfaced a county phone number, but explicit city-side confirmation of the county delegation was not found in this pass.
- **Daly City** — decibel figures (60/50 dBA) cited by secondary sources were not found verbatim inside the 3-section Ch. 9.22 that was captured; a separate zoning/performance-standards chapter likely carries the numeric limits and was not located in this pass.

## County involvement discovered

- **Santa Rosa** — Sonoma County Animal Services explicitly states it serves Santa Rosa directly (along with Healdsburg, Windsor, and unincorporated county territory). Explicit county delegation, not a geography-based assumption.
- **Concord** — City of Concord's own police-department flyer states the city contracts with the **Contra Costa County** Animal Services Department for all animal-noise complaints citywide. Explicit, city-stated delegation.
- **Antioch** — the city's own official Animal Services page directs residents to Contra Costa County Animal Services (925-779-6989). Explicit city-stated delegation.
- **Richmond** — a Richmond-focused search surfaced a Contra Costa County Animal Services number, but this was **not** found stated on an official Richmond city page in this pass — flagged `needs_review` rather than presented as confirmed delegation.
- **Sunnyvale / Santa Clara** — animal control is provided by the **Silicon Valley Animal Control Authority (SVACA)**, a multi-city joint-powers authority (serving Campbell, Los Gatos, Monte Sereno, Mountain View, Santa Clara, and, per older sources, Sunnyvale). This is **not** Santa Clara County government — flagged as its own authority category (`other (joint powers authority)`), not city and not county.
- **Daly City / San Mateo** — Peninsula Humane Society & SPCA is a **private nonprofit contractor**, not San Mateo County government, and explicitly does not accept barking-dog-noise-only complaints (those route to city police). Flagged as contractor authority, not county delegation.
- All other cities (San Jose, San Francisco [consolidated], Oakland, Fremont, Hayward, Vallejo, Berkeley) run their own city animal-control/animal-services function with no stated county or JPA delegation found.

## Ambiguous authority requiring later legal review

- Whether Contra Costa County's Noisy Animal Ordinance actually governs animal-noise complaints inside **Richmond** specifically (as opposed to only unincorporated county territory) is not confirmed by an official Richmond-side source in this corpus and should not be treated as settled.
- The exact current-vs-superseded relationship between Concord's PD-flyer-cited old code sections ("62-32(1)cc", "62-201(e)") and the current codepublishing.com Development Code numbering (18.150.130) was not resolved — the flyer may reference a pre-recodification numbering scheme.
- Whether SVACA still serves Sunnyvale (some sources describe changed animal-control providers over time for Sunnyvale) is unresolved in this pass.

No legal conclusions are asserted anywhere in this report or the manifest beyond what each official source states about itself.
