# Play Store competitive read — 2026-09-28

Method: 12 Play Store searches from the US store ("noise complaint", "neighbor noise log", "noise ordinance", "noisy neighbour", "noise nuisance diary", "noise evidence recorder", "noise log app", "noise diary", "barking dog log", "decibel meter complaint", "sound level evidence", "the noise app"), then each candidate's store page read for downloads, rating, price, update date and description. Numbers are the store's own buckets.

## Tier 1 — log + law (the corner NoiseFile is in)

| App | Downloads | Rating | Price | Updated | Law layer |
|---|---|---|---|---|---|
| **dBLog: Noise Complaint Log** (GriswoldLabs, `com.griswoldlabs.dblog`) | 10+ | none yet | free + $19.99 one-time Pro (ads in free) | Sep 28, 2026 | 27 US cities: day/night limits by zone, measurement spot, duration rules, citation; GPS/ZIP city detect; **flags a reading over the limit live**; verified complaint channel per city; letter generator |
| **Noise Complaint: dB Meter Log** (`com.quiet.log.noise.tracker`) | 10+ | 3.8 | $2.99 + IAP to $59.99 | Aug 12, 2026 | 100+ cities / 20 countries via GPS; "live evidence checklist: see if your recording legally qualifies"; authority contact + penalty info in PDF/Word/Excel |
| **Noise Log – Neighbor Complaint** (`com.gbapps.noiselog`) | 1K+ | 4.5 | free + IAP $2.99–59.99 | Sep 7, 2026 | Country-level letter templates only (DE, UK, AU, US by state, CA, PL) |

## Tier 2 — log-and-evidence, no law

| App | Downloads | Rating | Price | Updated |
|---|---|---|---|---|
| Nuisance Noise Recorder (`sjq.nuisancenoiserecorder.com`) | 10K+ | 2.0 (73) | free + $0.99 export | May 2026 |
| Floor Noise Log (`com.menew.noiseanalyzer`, Korea, apartment floor noise) | 1K+ | – | free | Sep 22, 2026 |
| Noise Recorder – Auto dB Meter (`com.cooltime.noise`, Korea) | 1K+ | – | free | Aug 2026 |
| My Noisy Neighbour (`com.noisy.neighbour.app`) | 100+ | – | free + $0.99 | Jun 2025 |
| FloorLog (`com.project.jjan.floorlog`, on-device AI sound tags) | 50+ | – | free + IAP | Jul 2026 |
| Noise Diary & dB Meter (`com.dustline.noisediary`) | 1+ | 4.7 | free + $3.99 | Jul 2026 |
| Noise Recorder (`uk.co.andytomg.noiserecorder`, threshold clips) | 500+ | – | $0.39 | Aug 2026 |

## Tier 3 — meters (huge, no case-building)

| App | Downloads | Rating | Price |
|---|---|---|---|
| Sound Meter (`kr.sira.sound`, Smart Tools) | 10M+ | 4.4 (206K) | $2.99 |
| Sound Meter (`com.gamebasic.decibel`) | 10M+ | 4.5 (186K) | free |
| Sound meter: SPL & dB (`com.ktwapps.soundmeter`) | 5M+ | 4.6 (53K) | free |
| Decibel X (`com.skypaw.decibel`) | 5M+ | 4.0 (13K) | IAP to $47.99 |
| SmarterNoise (`com.smarternoise.app`, video + level, health icons) | 100K+ | 4.2 (2K) | $2.99 |
| NoiseCapture (Ifsttar, community noise maps) | 100K+ | 3.6 | free |
| NYC Noise (NYC DEP, anonymous levels to the city) | 1K+ | 4.1 | free |
| Noise Tracker (CSIR-NEERI India) | 10K+ | 3.5 | free |

## Read

- **Exactly NoiseFile:** nobody. No app ships exact-jurisdiction Bay Area packets with the code's own words, hours logic, required-incident counts, ambient-jump capture and a no-verdict boundary.
- **Closest and above us on features:** dBLog (updated the same day as this read). It has what we chose not to build — a live "over the limit" flag, hash-chained tamper-evident logs, numbered PDF exhibits with a declaration page, WebDAV backup, team/properties screens — plus 27 US cities. Its city list is not published on the store page; Bay Area coverage unknown. 10+ downloads: no traction yet.
- **Second closest:** Noise Complaint: dB Meter Log — breadth (100+ cities) over depth; "legally qualifies" checklist is the verdict we refuse. 10+ downloads, 3.8★.
- **Only log-and-letter app with any traction:** Noise Log – Neighbor Complaint, 1K+, country templates.
- **The whole log-and-law corner on Android is under ~1,500 installs combined.** Meter apps are 30M+. Nobody has converted meter users into case-builders.
- **Price signal:** free + one-time Pro ($19.99) or $2.99 paid. NoiseFile at $0.99 sits below everyone.

## What "above and beyond" would mean for us (not decided)

1. PDF exhibit export with a declaration page (dBLog has it; we have copy-text and a share sheet).
2. GPS city detect (both Tier-1 apps have it; we pick from a list).
3. Tamper-evident log hashing (dBLog).
4. More cities. dBLog 27, the other 100+, us 15 — but ours quote the code.
