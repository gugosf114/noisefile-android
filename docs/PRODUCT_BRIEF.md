# NoiseFile product brief

Last updated: 2026-07-25

## Promise

**Know the rule. Log the noise. File the complaint.**

NoiseFile is a US consumer Android app for understanding and documenting
neighborhood noise and taking the correct local action.

The workflow is:

1. Determine the user's jurisdiction and noise source.
2. Explain the applicable quiet hours, measurement rule, filing requirements,
   responsible agency, and likely enforcement path.
3. Measure and document the disturbance.
4. Maintain the incident history required by the jurisdiction.
5. Prepare the correct official form or documented report.
6. Direct the user to the correct submission or escalation channel.

The app improves an ordinary complaint by organizing facts the user would
otherwise submit from memory. It is not a courtroom or legal-opinion product.

## Product decisions

- Start with major Bay Area jurisdictions.
- Store a verified, frozen copy of each official rule and procedure.
- The production app does not search the public web for rules.
- Use structured data instead of a legal-analysis AI.
- Check source material periodically for amendments.
- Audio and case information remain on-device unless explicitly shared.
- Retrieve rules by exact jurisdiction and noise category from a versioned
  offline catalog. Missing coverage must return unavailable rather than a
  nearby or model-generated answer.
- Win through local coverage, excellent design, correct routing, and effortless
  filing—not through decorative AI.

## Rule catalog

Each covered workflow needs:

- jurisdiction and geographic scope;
- noise type;
- daytime and nighttime hours;
- published limits or plainly audible standard;
- measurement location, duration, weighting, and response when specified;
- common exceptions and permit/variance handling;
- incident-count or verification requirements;
- enforcement stages;
- responsible department;
- official form, portal, phone number, or submission route;
- official source URL;
- effective date and last verification date.

## Incident record

Save:

- date and start time;
- duration;
- address or approximate location;
- minimum, average, and maximum estimated level;
- noise type and impact;
- notes;
- optional private audio;
- optional photographs;
- filing and response history.

The output is a **documented noise report**, not courtroom evidence. A report
may state that a private recording exists without assuming a city portal
accepts audio.

## Up-front guidance

Before recording, show **What happens in your city**:

- who handles the complaint;
- what the first report normally triggers;
- whether repeated or independently verified incidents are required;
- the difference between a complaint and verified violation;
- what the resident must document;
- when warnings, inspection, fines, mediation, landlord action, or other
  escalation may begin;
- the correct next channel.

Requirements belong at the beginning, before the user wastes time documenting
the wrong information.

## Enforcement Reality

Keep two layers visibly separate:

1. **Official process:** what the agency says should happen.
2. **Reported follow-through:** what objective public outcome data and dated
   community reports suggest actually happens.

When supported, say plainly:

> You can still file, but adjust your expectations. This jurisdiction has a
> weak or inconsistent record of following through on noise complaints.

Show source count, time period, update date, and confidence. Never promise that
enforcement will or will not occur.

## Progressive escalation

The incident and filing history can recommend—but never automatically perform—
the next reasonable step.

Example:

> You have filed five complaints about this location in two months. Since the
> disturbance is happening again, consider calling the police non-emergency
> line. Keep your documented history available.

Ordinary noise uses non-emergency channels. Mention 911 only for an immediate
threat, violence, or genuine emergency.

## Construction mode

For the selected address, show:

- weekday, Saturday, Sunday, and holiday hours;
- professional, homeowner, street-work, and public-project distinctions;
- decibel or plainly audible rules;
- active-permit and variance exceptions;
- emergency-work exceptions;
- permit-check route;
- responsible department and filing channel.

## Differentiator 1: Ordinance-Aware Capture

Do not merely show a generic decibel number. Turn the stored local procedure
into a guided capture:

- where to measure;
- how long to measure;
- the applicable time window;
- weighting and response when supported;
- a same-position quiet baseline;
- explicit tonal, impulsive, or recurring-noise modifiers.

Example:

> This rule considers noise lasting more than five minutes in an hour. You have
> documented 3 minutes 42 seconds.

When useful:

> The disturbance averaged 14 dB above the normal background level measured
> from the same position.

Commercial promise:

> **The meter that knows how your city measures.**

## Differentiator 2: Neighbor Verify

The primary user can send a private, expiring link to another affected neighbor
while a disturbance is happening. No installation is required.

With permission, the neighbor can:

- independently confirm the same event;
- add time and approximate location;
- describe its impact;
- take a short measurement or private recording;
- provide a name or signature required by a local process.

Keep each observation separate:

> Three separate households documented the same disturbance between 11:42 p.m.
> and 11:49 p.m.

No public accused-property map, automatic neighbor contact, silently shared
identity, or falsely averaged cross-phone reading.

Neighbor Verify strengthens the report and creates an organic acquisition loop:
every witness link introduces another person who has the same problem.
