# NoiseFile — who else is out here, and how they get their numbers (read 2026-09-06)

Short version: the decibel-meter lane is packed and worthless; the "log it,
letter it" lane has a handful of young, mostly solo apps; the "verified city
rule + right office" lane has one real player (Decibel Shield, 36 big cities,
none in the Bay Area); the "city buys it and officers review it" lane exists
only in the UK. Nobody found does exact-jurisdiction offline packets, Android,
Bay Area depth, and a clock check. That corner is small and it is ours.

## 1. The apps

| App | Where / who | What it does | dB honesty | Ordinance knowledge | Money |
|---|---|---|---|---|---|
| **Decibel Shield** (APPSTACK LLC) | iOS 4.7★ (~600 ratings) + free web meter; content updated 2026-09-03 | meter, exposure guidance, noise-ordinance pages, complaint guide, hearing tests | says plainly: uncalibrated, ±10 dB, "not evidence for a noise complaint" | **36 US cities / 26 states**, "researched from the city's official codified text, re-checked against the live code", verification date, links to the code. CA = Los Angeles, San Diego, Inglewood. **Zero Bay Area cities.** | premium subscription in the iOS app; web free |
| **QuietCase** (Rowlytics, Australia) | iOS; released 2026-03-16, v1.0.11 2026-08-27; no ratings yet; AU/US/UK/CA/NZ/IE | meter, timestamped audio/video, diary, pattern charts, auto complaint letters "tailored to jurisdiction", "compliance checking against local regulations", "tribunal-ready" packages | color-coded meter; claims "professional noise evidence — accepted by councils, tribunals" with no receipts | claims jurisdiction tailoring; no visible source list | Evidence Builder $9.99/mo, Dispute Toolkit $24.99/mo |
| **Noise Log: Neighbor Complaint** (solo dev) | iOS (2024-07) + Google Play | timestamp, estimated dB, duration, categories, impact tags, calendar heatmap, PDF report, letter templates by country, quiet-hours flag fixed at 10 PM–7 AM | "estimated" | none (one default quiet window for everyone) | $3.99/week, $14.99/yr, $19.99 lifetime |
| **NoiseNote**, **My Noisy Neighbour** (Google Play) | Android, small | log + audio clips + PDF "council-ready" report | meter shown | none | freemium |
| **The Noise App** (RHE Global, UK, since 2015) | UK, Netherlands, Ireland, Australia; **1,000,000+ reports a year**; no US presence | resident taps, records 30 seconds (max five per 24 h), fills a form; the council's or housing provider's officer listens on a secure site; case builds over time; new audio classifier for triage | no dB verdict at all — the recording is the evidence, a human decides | the buyer *is* the authority | councils and housing providers pay; residents get it free. Bristol: complaints down 35%, 93% of cases closed in six months |
| **NYC Noise** (NYC DEP, launched 2025-11-24) | iOS + Android, free, city-run | five-second dB reading + noise type + place; no audio; data goes to DEP to find hotspots and aim enforcement | phone dB, used statistically, never per-case | the city is the app | taxpayer |
| **NIOSH Sound Level Meter** (CDC) | iOS only | occupational meter | ±2 dBA; **Type 2 (IEC 61672) with an external calibrated mic**; no Android because Android hardware is too fragmented to verify in a lab | none | free |
| **NoiseCapture** (Université Gustave Eiffel, open source) | Android | community noise maps; phone-to-phone calibration at "NoiseCapture parties" | ±4.5 dB in mapping after community calibration | none | free |
| Decibel X, Sound Meter, and ~200 others | both stores | a number on a screen | usually "informational only" | none | ads / subs |
| **Local Noise Laws**, **noiseevidence.com** | websites | ordinance summaries (12+ cities), free "resolution kit" PDF, web meter | — | summaries, sourcing not disclosed | lead-gen |

## 2. How the numbers are really made (the papers)

- **NIOSH 2014** tested 192 apps; on iOS only 4 came within ±2 dBA of a Type 1 meter; Android apps were not reliable enough to rank (Kardous & Shaw).
- **NIOSH 2016, JASA**: with an **external calibrated microphone** the same apps land within **±1 dB** of a Type 1 meter from 65 to 95 dB. The mic, not the app, is the instrument.
- **NIOSH's own app** meets **IEC 61672 Type 2 with an external mic**; it is iOS-only because "hundreds of Android devices, even the same manufacturer uses different parts" made lab verification impossible.
- **2017 (Sensors, PMC5426841)**: Android phones (Samsung S7 Edge, S4, J5, BQ) can be brought to about 1–2 dB with a **per-model linear calibration**; uncalibrated, model-to-model spread is the whole problem.
- **2023 (JMIR, PMC10686533)**: on an iPhone 13 Pro built-in mic the NIOSH app sits within 0.5 dBA of reference; Decibel X reads 2–4 dBA low. Verdict: apps "complement but not replace" sound level meters.
- **Legal practice**: code enforcement and courts use the officer's calibrated meter; a phone reading is not certified proof anywhere found. What gets an officer to come out is a consistent log of dates, times, durations — plus, in the UK, the recording.

So the field's honest position is exactly NoiseFile's 7/29 boundary: the phone number is context, the log is the evidence, the officer's meter decides.

## 3. Is it crowded?

- **Meter apps:** hundreds. Not a business.
- **Log-and-letter apps:** five or six, all 2024–2026, mostly one-person, iOS-first. QuietCase is the only one charging real money ($25/month) and it is six months old.
- **Verified city rules:** Decibel Shield alone, big cities only, as web pages plus a meter app. No Bay Area city. No offline packets. No incident-count logic. No clock check.
- **Institutional (city or housing provider buys it):** The Noise App owns the UK; nothing like it in the US. NYC built its own, but for statistics, not for a resident's case.

## 4. What this says about NoiseFile

1. **Keep the boundary.** Every serious player (NIOSH, The Noise App, NYC DEP, Decibel Shield's own fine print) treats a phone dB as context, not proof. The 7/29 tests were right.
2. **The corner is real and empty:** exact-jurisdiction, verified, offline packets for a region, with the required incident count, the right office, and now the clock. Decibel Shield proves people pay for the rule lookup (600 ratings, subscription) and has not touched the Bay Area.
3. **The number can be made honest two ways, both proven:** (a) a plug-in calibrated USB-C or Lightning mic, which is what turns NIOSH's app into a Type 2 meter; (b) a one-time per-model calibration table, which the 2017 paper shows gets Android to 1–2 dB. Both are features, not tonight.
4. **The big-money model is the UK one:** sell to the housing provider or the city, give it to residents free, let an officer review the recording. In the US that door is open. It is a sales job, not a code job.
5. **The Noise App's newest move is an audio classifier for triage.** WiM's team already owns speech and audio classification; that is a real overlap worth remembering.

## Sources

- https://decibelshield.app/noise-ordinance/ and https://decibelshield.app/
- https://apps.apple.com/tr/app/quietcase/id6759844730
- https://apps.apple.com/us/app/noise-log-neighbor-complaint/id6789116754 and https://play.google.com/store/apps/details?id=com.gbapps.noiselog
- https://play.google.com/store/apps/details?id=com.noisenote.app and https://play.google.com/store/apps/details?id=com.noisy.neighbour.app
- https://thenoiseapp.com/ and https://www.rheglobal.com/news/the-noise-app-transforming-noise-management-for-housing-associations-and-local-authorities
- https://www.nyc.gov/site/dep/news/25-030/dep-launches-new-innovative-mobile-app-better-understand-city-s-noise
- https://www.cdc.gov/niosh/bulletin/2014/sound-app.html and https://www.cdc.gov/niosh/bulletin/2017/sound-app.html
- https://pubmed.ncbi.nlm.nih.gov/27794313/ (Kardous & Shaw 2016, JASA)
- https://pmc.ncbi.nlm.nih.gov/articles/PMC5426841/ (2017, Android calibration)
- https://pmc.ncbi.nlm.nih.gov/articles/PMC10686533/ (2023, app accuracy on iPhone 13 Pro)
- https://github.com/Universite-Gustave-Eiffel/NoiseCapture
- https://www.localnoiselaws.com/ and https://noiseevidence.com/
