# NoiseFile repository guidance

- GitHub `gugosf114/noisefile-android` and its `main` branch are the source of truth.
- Build a native Android app with Kotlin and Jetpack Compose.
- Keep all ordinance and procedure content in a versioned, offline rule catalog. The production app must not search the public web for rules.
- Every stored rule needs an official source URL, effective or verification date, jurisdiction, and plain-language guidance.
- Never invent a limit, enforcement step, response time, or filing requirement.
- Call measurements "estimated" unless the device has been calibrated against suitable equipment.
- Audio, incident history, addresses, and witness information remain local/private by default.
- Optimize every screen for one-handed phone use, imperfect eyesight, large text, and obvious primary actions.
- Use a small shared design system. Avoid tiny legal copy, dashboard clutter, fake precision, and decorative feature density.
- Validate changes with unit tests, lint, an Android build, and real-device screenshots when UI, audio, permissions, or system behavior changes.
- Hobby-app changes land on `main` after verification.
