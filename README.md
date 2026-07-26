# NoiseFile

[## ⬇️ DOWNLOAD NOISEFILE APK](https://github.com/gugosf114/noisefile-android/raw/refs/heads/main/00-NOISEFILE-DOWNLOAD.apk)

**Know the rule. Log the noise. File the complaint.**

NoiseFile is an Android app that explains the local noise process before a
resident files, measures and documents an active disturbance, maintains the
required incident history, and prepares the correct next action.

The first complete city packet covers three San Jose workflows:

- the official five-incident documentation requirement;
- active party or amplified-music reporting;
- construction-hour guidance and Code Enforcement routing;
- live estimated sound-level measurement;
- start time, duration, minimum, average, and maximum readings;
- an on-device incident history;
- a city selector ready for additional verified rule packets;
- a private neighbor-invite share flow;
- progress toward a filing-ready record.

NoiseFile is deliberately local-first. Recordings and incident history remain
on the device unless the user explicitly exports or shares them.

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
   one exact `jurisdictionId` + `noiseType` lookup at a time. San Jose is
   populated; the other 14 cities in `legal-corpus/` are acquired but not yet
   extracted into the catalog. See
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
