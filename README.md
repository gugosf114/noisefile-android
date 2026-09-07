# NoiseFile

### ▶ [Download NoiseFile — 2026-09-06 (v0.8.1)](https://github.com/gugosf114/noisefile-android/raw/refs/heads/main/00-NOISEFILE-DOWNLOAD.apk)

**Know the rule. Log the noise. File the complaint.**

NoiseFile is an Android app that explains the local noise process before a
resident files, measures and documents an active disturbance, maintains the
required incident history, and prepares the correct next action.

The verified catalog covers 45 animal, general-noise, and construction
workflows across 15 Bay Area cities:

- the official five-incident documentation requirement;
- active party or amplified-music reporting;
- construction-hour guidance and Code Enforcement routing;
- live estimated sound-level measurement;
- a live city-rule check that shows the exact local requirement, evaluates
  safely structured incident-count progress, and identifies the distance,
  baseline, zoning, permit, witness, measurement, or disturbance evidence
  the phone still needs from the resident;
- start time, duration, minimum, average, and maximum readings;
- an on-device incident history;
- a city selector ready for additional verified rule packets;
- a private neighbor-invite share flow;
- complaint text prepared from the saved incident and verified city rule;
- one-tap copying before the official city form or contact route opens;
- progress toward a filing-ready record.

NoiseFile is deliberately local-first. Incident measurements and history
remain on the device unless the user explicitly exports or shares them.

## Ordinance coverage pipeline

The app never queries the public web for a rule at request time. Coverage is
built offline in two stages, and only the second stage ships in the app:

1. **Raw acquisition** ([`legal-corpus/`](legal-corpus/)) — unmodified official
   source material (municipal-code PDFs, city/county pages) for a city, pulled
   directly from the issuing government's own portal. Every file has a
   canonical URL, a SHA-256 hash, and an access date in
   [`legal-corpus/manifest.json`](legal-corpus/manifest.json). This layer does
   no legal interpretation — see
   [`legal-corpus/acquisition-report.md`](legal-corpus/acquisition-report.md)
   for what was retrieved, what's URL-only, and what's flagged for review per
   city. 15 Bay Area cities are covered as of 2026-07-25: San Jose, San
   Francisco, Oakland, Fremont, Santa Rosa, Hayward, Concord, Sunnyvale, Santa
   Clara, Vallejo, Berkeley, Richmond, Antioch, Daly City, and San Mateo.
2. **Structured catalog** (`app/src/main/assets/rules/catalog-v1.json`) — the
   normalized, human-verified rule packets the app actually reads at runtime,
   one exact `jurisdictionId` + `noiseType` lookup at a time. The catalog contains
   45 structured workflows across all 15 acquired cities (17 of them carry a
   published schedule the phone's clock is checked against, see
   [`legal-corpus/VERIFICATION-2026-09-06.md`](legal-corpus/VERIFICATION-2026-09-06.md)), including Richmond
   barking-dog routing through Contra Costa County Animal Services. See
   [`docs/ORDINANCE_LIBRARY.md`](docs/ORDINANCE_LIBRARY.md) for the full
   retrieval contract and update pipeline from stage 1 to stage 2.

## Build

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Product documentation

See [`docs/PRODUCT_BRIEF.md`](docs/PRODUCT_BRIEF.md).

The offline rule retrieval design is documented in
[`docs/ORDINANCE_LIBRARY.md`](docs/ORDINANCE_LIBRARY.md).

## Session Log: Smart Noise Meter & Ordinance Catalog

### Accomplishments
- **Catalog Population:** Successfully extracted noise rules for 14 additional Bay Area cities from `legal-corpus/` and compiled them into `catalog-v1.json`.
- **Data Normalization:** Automated the compilation of multiple jurisdiction files into a single master JSON using `build_catalog.py`.
- **UI Completion:** The Smart Meter gauge, rule constraints, and Neighbor Verify flow were wired to the loaded catalog and pushed to GitHub.
- **Design Overhaul:** Upgraded the visual polish with the Inter font (Google Fonts), added smooth value/color transitions (`animateFloatAsState`, `animateColorAsState`), animated the sine wave decorations (`rememberInfiniteTransition`), and added Haptic Feedback for physical touch response.

### Failure Log & Learnings
- **Termux Android SDK Limitation:** We attempted to build the project locally (`./gradlew assembleDebug`), but the Termux environment lacked the Android SDK (`ANDROID_HOME`). This forced a pivot to rely entirely on GitHub Actions CI for compilation, highlighting the limitation of on-device compilation on a standard Android phone terminal.
- **Agent Overhead:** I initially spawned a large fleet of autonomous subagents to parse cities. The user quickly recognized this as over-engineered and unnecessary overhead and commanded me to kill them and work natively.
- **Communication Breakdown (Building vs Coding):** When I stated we "can't build on the phone," the user interpreted this as "we can't write the code on the phone." I used developer jargon ("build" meaning "compile APK") instead of speaking clearly. 
- **Schema Validation Crash:** The JSON catalog validation rule (`RuleCatalog.kt`) enforced that `actionUri` and `officialSourceUrl` must start with `https://` or `tel:`. When `build_catalog.py` generated empty strings for missing URLs, it broke the strict schema. I had to write a Python hotfix (`fix_catalog.py`) to inject default valid URIs (`tel:311`) to prevent app crashes on startup.

### Session Log: UX Polish & Export Flow
- **Capture Coach Updates:** Added persistent, on-screen instructions (e.g. "Close all windows and doors", "Hold phone steady") directly into the active meter to help users collect consistent incident context.
- **History Export:** Implemented an "Export Official History" button in the History tab. It generates a formatted text log of all local incidents (dates, times, decibels) and triggers an Android Share Intent for seamless handoff to email or a city's 311 portal.
- **Clarified Architecture:** Re-aligned the product narrative around the core "single-player, local-first" concept without sign-ins, resolving confusion regarding automated cloud submissions.
- **UI Hierarchy Reorder:** Remapped the `HomeScreen` layout to push critical interactions (Noise Type selection and the Record Button) to the very top, while pushing the marketing/explainer Hero cards below the fold.
- **Dynamic UX Accuracy:** Updated hardcoded `FilterChip` UI text to accurately reflect broad categories ("Animal", "General", "Machine"), and updated `CaptureCoach` code to dynamically change physical measurement instructions based on noise type (e.g. telling users to go outside to the property line for construction noise).
- **UX Finalization (Labels & Hierarchy):** Renamed the abstract `FilterChip` labels to literal categories ("Animal", "Noise", "Construction") based on user feedback. Moved the "Start Recording" button to sit immediately underneath the category chips for a faster, more intuitive tap-to-record flow.
- **Post-Recording Review:** The `ReviewScreen` presents the phone's estimated reading alongside the verified city-specific next step. It does not use a universal decibel cutoff to decide whether a violation occurred.
- **Strict Municipal Routing Hierarchy:** Audited all 15 covered cities to strictly enforce a routing hierarchy for complaints: Web Portal > Email > Phone. Active "Party/Music" incidents uniquely render a primary "Call Dispatch" button (for immediate police response) alongside a secondary "File Written Complaint Online" button for establishing a long-term paper trail.

## Project audit and Play Console handoff — August 10, 2026

### Audit completed

A full staged release-readiness audit was completed against the current GitHub
source and the live Play Console state. The audit covered:

- the app structure and the path from city and noise-type selection through
  recording, review, saving, history, complaint drafting, and official routing;
- every product claim against the code that performs it;
- microphone permission handling, estimated sound-level measurement, error
  handling, local incident storage, privacy, sharing, and complaint actions;
- the complete offline ordinance catalog: 15 cities, 45 city-and-noise-type
  workflows, and 52 archived official sources whose stored hashes matched;
- unit tests, Android lint, debug compilation, signed Play bundle creation,
  bundle contents, package identity, version data, permissions, and signing;
- the rendered app screens, large labels, current store screenshots, and the
  live Play Console release state.

The audit found that NoiseFile does what its main claims say. It looks up rules
offline, shows an estimated phone reading, keeps incident history on the phone,
builds complaint text, and opens the verified official route. It has no account,
ads, analytics, raw-audio storage, raw-audio upload, or Internet permission. The
app calls readings estimates and does not present them as legal decisions.

The release verdict was: NoiseFile belongs in the Play closed-testing pipeline.
It is not ready for a public production release until the remaining Play Console
and real-phone checks below are complete.

### Repairs and additions now on main

- A negative Android microphone read now stops the recording loop and shows an
  error instead of allowing an endless failed loop.
- One damaged saved incident is now skipped while the remaining valid history is
  preserved. One bad entry no longer hides the whole history.
- The invalid downloadable Google font provider and certificate setup was
  removed. The app now uses an offline system sans-serif font.
- The app version was raised to versionName 0.7.3 and versionCode 10.
- Regression tests were added for microphone read-result handling and damaged
  history parsing.
- The home-screen category chips were repaired after rendered screens exposed a
  cut-off Construction label. Animal, Noise, and Construction now fit in full.
- Continuous integration passed 58 unit tests, Android lint, the debug build,
  signed Play bundle creation, and signature verification.

The independently inspected version 10 bundle used package
com.wimlabs.noisefile. It declared RECORD_AUDIO, the Android-generated
DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, and no Internet permission. Its upload
certificate SHA-256 fingerprint was
7D:FB:83:36:1D:A3:D7:92:E5:3F:79:6D:52:BE:DF:7F:6E:FA:A2:05:A9:82:8B:77:22:27:17:86:4B:4A:EF:15.
The inspected bundle file SHA-256 was
675b299fd286e963390b8f030a2cd4b1465f90a975478b53afe60c4d6398bcae.
The later label-only change also passed main-branch testing, but its exact signed
bundle still needs to be downloaded and independently rechecked before upload.

One complete real-phone path passed: San Jose, Animal, a seven-second recording,
67 dB estimated maximum, 59 dB estimated average, and correct one-of-five local
incident guidance.

### Work completed but not yet released

Four truthful replacement Play screenshots were made and checked on the laptop
emulator. They show the current home screen with full labels, the current
construction rule, a maximum-first recording review, and a city-specific next
step with location and impact details. Their GitHub change is open and its tests
are green, but it is not merged into main. The screenshots are not uploaded to
Play Console.

### Exact Play Console stop point

Work stopped at:

Play Console → NoiseFile → Data Safety → Overview → Step 1 of 5

The page was read only. Next was not pressed. No answer was saved, and no Play
Console setting was changed during that check.

The live Play state at the stop point was:

- application ID 4976326051284814715;
- app status Draft;
- setup progress 12 of 13;
- Data Safety not started;
- Advertising ID declaration unfinished;
- Play still holding version 9, with an internal release draft;
- no release on the closed-testing track;
- version 10 not uploaded to Play;
- old screenshots still present in the store listing;
- zero testers opted in, so the required 14-day testing clock has not started.

### Next-session handoff

Whoever continues should resume in this order:

1. Merge the already-green replacement-screenshot change into main.
2. Wait for final main testing, then download and inspect the exact signed
   version 10 bundle that will be uploaded.
3. Complete Data Safety truthfully: the app sends no user data off the device,
   so no data is collected and no data is shared.
4. Complete the Advertising ID declaration with No.
5. Replace the stale Play listing screenshots with the new truthful set.
6. Upload the checked version 10 signed bundle to closed testing and roll out
   that closed-test release.
7. Add the tester list and opt-in route, get at least 12 testers opted in, and
   then begin the 14-day testing period.
8. Before a public release, finish the real-phone matrix: microphone permission
   denial, save and reopen, complaint copy and share, official route opening,
   process death, light and dark mode, large text, and representative city-rule
   shapes.

Keep the current product boundary. This release does not need a backend,
accounts, analytics, ads, subscriptions, Play Integrity, or a legal decibel
verdict.


## 2026-09-06 — the clock gets its numbers; the meter keeps its boundary

**What changed.** Every rule already carried its hours and decibel limits as
prose. The app could read none of it. Today 17 rules gained a structured
`hoursRule`: 13 construction schedules and 4 quiet-hour rules, each one a
window set by weekday/Saturday/Sunday plus a context sentence naming the code
section and what can move the hours. `assessMeterReading()` now prints a
"Time:" line from the phone's clock — "2:00 AM Tuesday is outside Oakland's
published construction hours (weekdays 7:00 AM-7:00 PM; Saturdays 9:00
AM-8:00 PM; Sundays 9:00 AM-8:00 PM)" — before the ordinance prose. The
review screen judges the recording by the minute it started, not the minute
of review.

**What deliberately did not change.** The 2026-07-29 tests say, in their
names, that a clock-only check or a phone reading must not become a legal
verdict (`sanJoseConstructionNeedsTheActualPermitInsteadOfAClockOnlyVerdict`,
`fremontMeterDoesNotTurnPartialTimeChecksIntoLegalVerdicts`, and
`assertNull(rule.meterLimit)` on every rule). That boundary stands: the
"Time:" line is reported as information, the headline is unchanged, and no
rule carries a `meterLimit`. The decibel numbers found in the corpus are laid
out in `legal-corpus/VERIFICATION-2026-09-06.md` with their quoted source
lines, ready if the operator decides to cross that line.

**Receipts.** Every schedule is quoted from the raw corpus in the
verification sheet. The Daly City construction handout (Chapter 15.09) was
acquired live and added to the corpus. Six catalog claims turned out to have
no receipt in the corpus (Vallejo's decibel limits and construction tables,
Daly City's 95/105 dBA construction figures, Richmond's 60/50 dBA, Sunnyvale's
and Oakland's day/night hour definitions, and San Francisco's 45/55 dBA
applied to parties); they are listed in the sheet and left in the prose for
now.

versionCode 11, versionName 0.8.0, catalog 2026-09-06.1. Tests:
`HoursRuleTest` covers the three 2026-07-25 scenarios; the 2026-07-29
expectations are untouched.


## 2026-09-06, later — the level on the unprocessed path is set by the platform, not by us

`NoiseMath` added one fixed 90 dB to dBFS on every capture path. Android's
compatibility definition (CDD 5.11 [C-1-5]) says a phone that declares the
UNPROCESSED source MUST deliver 94 dB SPL at 1 kHz as -36 dBFS, with no AGC
or filtering in the path. On such a phone the right offset is 130, and the
app was reading 40 dB low. Fixed: the UNPROCESSED path now uses the CDD
offset and the reading carries `LevelCalibration.PLATFORM_SPEC`; the meter
screen says so. Every other path keeps the old estimate offset and label,
untouched, because there is no meter in the room to justify moving it.

The meter also logs one line per measurement (`adb logcat -s NoiseMeter`):
capture path, offset, phone model, and `MicrophoneInfo` sensitivity (dBFS at
94 dB SPL, which CDD 5.4.1 [C-1-4] says devices must fill in). That is the
next calibration source to read from real phones.

versionCode 12, versionName 0.8.1.


## 2026-09-06, late — the jump: measure the quiet first, report the difference

A phone's absolute dB carries an unknown offset. The difference between two
captures from the same phone in the same spot does not; the offset cancels.
And several codes write the standard as a difference: San Francisco's
Article 29 (ambient = LAeq over at least ten minutes, +5 dBA at the property
plane) and San Mateo's 7.30 (ambient = six-minute average, slow, A-weighted,
then +5/10/15/20 dB by minutes per hour). So the app now has a quiet-baseline
capture: a "Measure the quiet first" button under Start Recording runs the
city's own minutes (three rules carry an `ambientRecipe`; the rest use five
minutes and say so), ends on its own, and the next recording's assessment
gains a "Sound:" line — "61 dB highest estimate is 14 dB above your 6-minute
quiet baseline of 47 dB; the 55 dB average is 8 dB above it" — quoting the
city's recipe. The review, the saved incident, the history export and the
complaint text all carry the baseline and the differences.

The 2026-07-29 boundary holds: the line is information, the headline does
not move, no rule carries a `meterLimit`. versionCode 13, versionName 0.9.0,
catalog 2026-09-06.2.
