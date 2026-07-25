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
