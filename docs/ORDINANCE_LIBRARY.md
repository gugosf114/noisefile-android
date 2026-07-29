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
