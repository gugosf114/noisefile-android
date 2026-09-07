# NoiseFile ordinance library

NoiseFile uses a versioned, offline rule catalog. The app never searches the
public web while a resident is documenting an incident.

## Retrieval contract

Every request is reduced to two exact keys:

1. jurisdiction ID;
2. noise category.

The catalog returns one verified workflow only when both keys match. A missing
city or category returns no rule. It must never fall back to a nearby city,
guess from model memory, or silently substitute a general rule.

## Packet format

`app/src/main/assets/rules/catalog-v1.json` contains:

- a schema and catalog version;
- supported and pending jurisdictions;
- normalized workflows;
- official source links and verification dates;
- measurement instructions, filing requirements, responsible agency, and next
  action.
- optional structured meter limits with the required time and measurement
  context.
- optional published schedules (`hoursRule`): construction hours or quiet
  hours by weekday, Saturday and Sunday, each with the context sentence that
  names the code section and what can change the hours. The app compares the
  phone's clock to the schedule and reports the result as information, never
  as a verdict; permits, conditions of approval and the ordinance's own
  wording decide.
- optional ambient recipes (`ambientRecipe`): the minutes the city's code uses
  to measure ambient, plus the rest of the recipe in the code's words. The app
  runs a quiet baseline for those minutes and reports the noise as a
  difference above it — the phone's unknown offset is the same on both
  captures, so it cancels. Reported as information, never as a verdict.

The Android build validates unique IDs, jurisdiction references, supported
schema versions, and the presence of rules for every enabled city.

During capture and review, every workflow shows its verified requirement. The
rule checker evaluates only incident facts that safely match a structured
requirement, such as saved incident-count progress. It separately names any
official measurement, zoning, distance, ambient sound, permit, witness,
documentation, duration, time, or reasonable-person evidence that still
requires the resident's observation or the enforcing agency instead of
inventing an answer from a phone estimate.

## Update pipeline

City packets are prepared outside the production app:

1. collect current official municipal sources;
2. extract the exact operational rule and exceptions;
3. have a human verify the normalized entry against the source;
4. update the catalog version and verification date;
5. run corpus tests and ship the signed catalog with an app update.

This is the first retrieval layer. It is deliberately simpler and safer than a
vector database for a small number of cities.

## Complaint drafting and optional model layer

NoiseFile prepares complaint text deterministically from the verified rule and
the resident's saved incident. It copies that text and opens the best available
official city form or contact route. This works without an account, network
model call, or invented legal facts.

AI may later explain a retrieved rule or polish the complaint's wording. It
receives only:

- fixed NoiseFile instructions;
- the exact retrieved city/category packet;
- the resident's current incident details.

The city packet is placed before variable incident data so providers can reuse
it through prompt caching. Model output must cite the packet and cannot invent
missing rules. Fixed hours, agency routing, and filing requirements remain
deterministic app data rather than model decisions.
