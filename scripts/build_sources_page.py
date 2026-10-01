#!/usr/bin/env python3
"""Rebuild docs/sources.html from the catalog (every rule's official source, hours and limit citations)
plus the legal-corpus manifest for the original cities. Run after any catalog change."""
import json, html, re
from collections import OrderedDict

cat = json.load(open('app/src/main/assets/rules/catalog-v1.json'))
man = json.load(open('legal-corpus/manifest.json'))
names = {j['id']: j['displayName'] for j in cat['jurisdictions']}
county = {}
for e in man['entries']:
    county.setdefault(e['city'], e.get('county') or '')

per = OrderedDict()
for cid in sorted(names, key=lambda k: names[k]):
    per[cid] = OrderedDict()   # url -> (label, meta)

def add(cid, url, label, meta):
    if not url or not url.startswith('http'):
        return
    url = url.strip()
    if url not in per[cid]:
        per[cid][url] = (label.strip(), meta.strip())

# Manifest entries first (richer labels) for the original cities.
byname = {v: k for k, v in names.items()}
for e in man['entries']:
    cid = byname.get(e['city'])
    if not cid:
        continue
    meta = ' · '.join(x for x in [e.get('chapter_or_section', ''), e.get('issuing_authority', '')] if x)
    add(cid, e.get('canonical_url', ''), e.get('official_title', ''), meta)

# Every rule's own source, plus the hours and limit citations.
kind = {'BARKING_DOG': 'animal rule', 'PARTY_MUSIC': 'general-noise rule', 'CONSTRUCTION': 'construction rule'}
for r in cat['rules']:
    cid = r['jurisdictionId']
    add(cid, r['officialSourceUrl'], r['officialSourceLabel'], f"{kind[r['noiseType']]} · verified {r['verifiedDate']}")
    for key in ('hoursRule', 'meterLimit', 'ambientRecipe'):
        blk = r.get(key)
        if blk and blk.get('sourceCitation'):
            add(cid, r['officialSourceUrl'], r['officialSourceLabel'], blk['sourceCitation'])
    # Doors that are official pages (forms, procedure pages, packets) are sources too.
    for u, l in ((r.get('actionUri'), r.get('actionLabel')), (r.get('secondaryActionUri'), r.get('secondaryActionLabel'))):
        if u and u.startswith('https://'):
            add(cid, u, f"{l} - {names[cid]} official page", names[cid])

total = sum(len(v) for v in per.values())
nav = ' '.join(f'<a href="#{cid}">{html.escape(names[cid])}</a>' for cid in per)
sections = []
for cid, items in per.items():
    cty = county.get(names[cid], '')
    lis = ''.join(
        f'<li><a href="{html.escape(u)}" rel="noopener">{html.escape(lbl)}</a><br><span class="meta">{html.escape(meta)}</span></li>'
        for u, (lbl, meta) in items.items()
    )
    small = f' <small>{html.escape(cty)}</small>' if cty else ''
    sections.append(f'<h2 id="{cid}">{html.escape(names[cid])}{small}</h2><ul>{lis}</ul>')

page = open('docs/sources.html').read()
head, rest = page.split('<body>', 1)
body = f'''<body>
  <main>
    <h1>Official sources</h1>
    <p class="tagline">Where every NoiseFile rule comes from.</p>
    <p class="notice"><strong>NoiseFile is made by WiM Labs, a small company.</strong> It is not part of any city or government. Every rule the app shows is quoted from the public code or official page of that city. The {total} sources below cover all {len(per)} cities. Catalog version {html.escape(cat['catalogVersion'])}.</p>
    <nav>{nav}</nav>
    {''.join(sections)}
    <p style="margin-top:48px"><a href="index.html">NoiseFile home</a> · <a href="privacy.html">Privacy policy</a> · <a href="https://github.com/gugosf114/noisefile-android/blob/main/legal-corpus/manifest.json">Machine-readable manifest (JSON)</a></p>
  </main>
</body>
</html>
'''
open('docs/sources.html', 'w').write(head + body)
print(f'{len(per)} cities, {total} sources')
