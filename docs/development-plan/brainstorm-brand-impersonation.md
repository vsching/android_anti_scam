# Brainstorm: Brand Impersonation Domain Detection

## Problem Statement

Domains like `applemy.com`, `grabsg.com`, `maybankonline.net` bypass current detection because:

- **They are not in scam feeds (yet)** -- these domains are newly registered and have not been reported to any threat intelligence feed. Our pipeline only flags domains that appear in external scam databases.
- **They do not match the current `BANK_KEYWORDS` + suspicious TLD pattern** -- the `checkBankNamePattern` function in `heuristics.ts` does detect brand keywords with extra characters, but only escalates to "dangerous" when the TLD is in the `SUSPICIOUS_TLDS` set (`.xyz`, `.top`, `.buzz`, etc.). A domain like `applemy.com` uses `.com`, which is not suspicious, so it gets classified as merely "suspicious" at 0.7 confidence -- and only if the keyword is in `BANK_KEYWORDS` at all. Global brands like Apple, Google, Netflix are **not** in `BANK_KEYWORDS`, so `applemy.com` gets zero hits.
- **They return "safe" with 0.5 confidence, misleading users** -- when no heuristic check triggers, the engine returns `{ verdict: 'safe', confidence: 0.5, reason: 'No suspicious patterns detected' }`. The website UI shows this with a green "SAFE" badge and a checkmark icon. Users interpret this as an endorsement, not as "we have no data on this domain."

### Real-world examples that would slip through today

| Domain | Why it bypasses detection |
|---|---|
| `applemy.com` | "apple" not in BANK_KEYWORDS; `.com` not suspicious |
| `grabsg.com` | "grab" is in BANK_KEYWORDS, but `.com` is not a suspicious TLD -- returns "suspicious" at 0.7, not "dangerous" |
| `netflixmy.com` | "netflix" not in BANK_KEYWORDS at all |
| `maybankonline.net` | "maybank" is in BANK_KEYWORDS, but `.net` is not suspicious -- returns "suspicious" at 0.7 |
| `singpasslogin.com` | "singpass" is in BANK_KEYWORDS, but `.com` is not suspicious |
| `taborang.com` | No brand keyword match, no suspicious TLD -- returns "safe" at 0.5 |

---

## Current Detection Logic

The heuristic engine (`heuristics.ts`) runs checks in this order and returns the most severe result:

### 1. Allowlist exact match (confidence 1.0 = "safe")
Checks `domain` and `rootDomain` against `LEGITIMATE_DOMAINS`, a hardcoded map of ~40 known-good domains (banks, gov, e-commerce). If found, returns immediately as safe without running any further checks.

### 2. URL shortener check (confidence 0.7 = "suspicious")
Checks if the domain is in the `URL_SHORTENERS` set (~20 domains like `bit.ly`, `tinyurl.com`). Flags as suspicious because destination is unknown.

### 3. Typosquatting / Levenshtein check (confidence 0.65-0.92 = "dangerous")
Computes edit distance between the input domain's base name and each allowlisted domain's base name. Threshold depends on string length:
- Length <= 5: distance <= 1
- Length <= 8: distance <= 2
- Length > 8: distance <= 3

This is good for catching `maybankk2u.com` but misses `applemy.com` because the edit distance from "apple" to "applemy" is 2, but "apple" is not in `LEGITIMATE_DOMAINS` (only `apple.com` is in the pipeline's allowlist, not in the heuristics engine's `LEGITIMATE_DOMAINS`).

### 4. Bank name pattern check (confidence 0.7-0.9 = "suspicious" or "dangerous")
This is the closest check to what we need. It iterates through `BANK_KEYWORDS` and checks if the domain base **contains** a keyword with extra characters. However:
- It only returns "dangerous" (0.9) if the TLD is in `SUSPICIOUS_TLDS`
- On a normal TLD like `.com`, `.net`, `.org`, it returns "suspicious" (0.7)
- Global brands (apple, google, netflix, etc.) are **not** in `BANK_KEYWORDS`

### 5. Suspicious TLD check (confidence 0.6 = "suspicious")
Standalone check for TLDs in `SUSPICIOUS_TLDS`. Low severity, just a yellow flag.

### 6. Punycode / IDN check (confidence 0.65 = "suspicious")
Flags internationalized domain names that could be used for homograph attacks.

### 7. Subdomain brand abuse (confidence 0.85 = "dangerous")
Checks if subdomains contain brand keywords on an unrelated root domain (e.g., `maybank.evil.xyz`).

### Where the gaps are

1. **Missing global brand keywords**: `BANK_KEYWORDS` only covers MY/SG financial and government brands. Global tech brands (Apple, Google, Microsoft, Netflix, Amazon, PayPal, WhatsApp) are absent. The pipeline's `allowlist.py` has these in the `ECOMMERCE` category, but `heuristics.ts` does not use them.

2. **`.com` considered safe for brand+extra patterns**: The `checkBankNamePattern` function only escalates to "dangerous" on suspicious TLDs. But brand impersonation on `.com` is arguably MORE dangerous because users trust `.com` domains.

3. **"safe" verdict for unknown domains is misleading**: The 0.5 confidence fallback says "safe" when it should say "we don't know." The website renders this identically to a confirmed-safe domain.

4. **No country-code suffix detection**: The `{brand}{country}` pattern (applemy, grabsg, netflixid) is extremely common in SEA-targeted scams and has no legitimate use case. Real brands use `brand.com.my` or `brand.com/my`, never `brandmy.com`.

---

## Approaches

### Approach 1: Expand BANK_KEYWORDS to BRAND_KEYWORDS

Add global brands to the keyword list:

```
apple, google, microsoft, amazon, netflix, whatsapp, facebook, instagram,
tiktok, telegram, paypal, spotify, linkedin, twitter, uber, wise, revolut,
binance, coinbase, metamask, trustwallet
```

Any domain containing these keywords + extra characters on any TLD would be flagged.

**Pros:**
- Simple implementation -- just expand the existing array and rename it
- Extends a proven pattern that already works for bank keywords
- Catches `applemy.com`, `netflixlogin.com`, `whatsapp-verify.com` etc.
- Zero changes needed to the detection logic itself

**Cons:**
- Common English words cause false positives: `appleton.com` (city), `amazonia.com` (legitimate business), `uber-cool.com` (non-brand usage), `celcom` substring in unrelated words
- Short keywords like `dbs`, `uob`, `mae`, `m1` already cause false positives -- adding more short global keywords makes this worse
- Does not address the `.com` TLD severity problem -- these would still only be "suspicious" at 0.7

**False positive risk:** Medium-high. Words like "apple", "amazon", "boost" are common English words that appear in non-impersonation domains.

**Implementation effort:** Very low (< 1 hour). Just add strings to an array.

---

### Approach 2: Country-suffix pattern detection

Detect the `{brand}{country_code}.com` pattern specifically. Real brands never register `brandcountry.com` -- they use `brand.com.my` (ccTLD) or `brand.com/my` (path-based). The pattern `{brand}{country_code}` followed by a generic TLD is a strong scam signal.

**Target country suffixes:** `my`, `sg`, `ph`, `id`, `th`, `vn`, `hk`, `tw`, `jp`, `kr`, `in`, `uk`, `us`, `au`, `nz`

**Detection logic:**
1. Build a comprehensive brand list (merge from BANK_KEYWORDS + global brands)
2. For each brand keyword, check if the domain base matches `{keyword}{country_code}` exactly or `{keyword}{country_code}{extra}` (e.g., `applemylogin`)
3. If matched, return "dangerous" at high confidence (0.9+) regardless of TLD

**Examples caught:**
- `applemy.com` -> "apple" + "my" -> dangerous
- `grabsg.net` -> "grab" + "sg" -> dangerous
- `netflixid.xyz` -> "netflix" + "id" -> dangerous
- `maybank2usg.com` -> "maybank2u" + "sg" -> dangerous

**Pros:**
- Very high precision -- the `{brand}{country}` pattern has essentially zero legitimate use
- Catches the exact attack pattern that is most common in SEA-targeted scams
- Works regardless of TLD (catches `.com`, `.net`, `.org` domains too)
- Low false positive rate because it requires both a known brand AND a country suffix

**Cons:**
- Does not catch non-country-suffix impersonation like `apple-login.com` or `maybanksecure.com`
- Requires maintaining a country code list
- Two-letter country codes could theoretically match legitimate domain suffixes (rare in practice)
- `id` is a common English suffix (e.g., `rapidapi.com` -- but this would need "rapid" to be a brand keyword first, so false positive risk is limited)

**False positive risk:** Very low. The combination of known brand + country code is highly specific.

**Implementation effort:** Low (2-4 hours). New function, new country code list, merge into check pipeline.

---

### Approach 3: Change "safe" to "unknown/unverified" for low confidence

When confidence is 0.5 (no checks matched and domain is not in any allowlist), show "unverified" instead of "safe" in the UI. This is the most honest UX -- we are not saying the domain is dangerous, just that we cannot vouch for it.

**Changes needed:**
- `heuristics.ts`: Change the fallback verdict from `'safe'` to a new `'unverified'` verdict (or keep `'safe'` but add a flag like `verified: false`)
- `check.html`: Add a fourth result state with a neutral color (grey/blue) and messaging like "We haven't verified this domain. Proceed with caution."
- API response: Include the new verdict or a `verified` boolean
- Android app: Handle the new verdict in the result screen

**Pros:**
- Most honest UX -- does not mislead users about domains we know nothing about
- Zero false positives -- we are not calling anything dangerous, just being transparent
- Catches ALL unknown domains, not just brand impersonation
- Shifts user behavior: users learn that "not flagged" does not equal "endorsed"
- Easy to explain to users: "We only vouch for domains we've verified"

**Cons:**
- The vast majority of legitimate domains are also not in our allowlist, so most checks would return "unverified" -- this could cause alert fatigue
- Users may stop trusting the tool if everything comes back as "unverified"
- Requires changes across multiple surfaces (website, API, Android app)
- Does not actively protect users from brand impersonation -- just removes false confidence

**False positive risk:** None (it is not calling anything dangerous). However, the "unverified" label on legitimate domains could erode trust.

**Implementation effort:** Medium (1-2 days). Requires coordinated changes across backend, website, and Android app.

---

### Approach 4: Levenshtein/fuzzy matching against allowlist

Expand the existing typosquatting check to also compare against the full allowlist from `allowlist.py` (which includes global brands). Check if a domain is suspiciously similar to any allowlisted domain.

**Current gap:** The typosquatting check in `heuristics.ts` only compares against `LEGITIMATE_DOMAINS` (40 domains), not the full allowlist from `allowlist.py` (100+ domains including `apple.com`, `netflix.com`, etc.).

**Enhanced logic:**
1. Import or sync the full allowlist into the heuristics engine
2. For fuzzy matching, compare the full domain (not just base) to catch TLD swaps: `maybank2u.com.my` vs `maybank2u.com` vs `maybank2u-login.com`
3. Consider normalized Levenshtein (distance / max length) for better comparison across different-length strings
4. Add domain-part-aware comparison: split on `.` and `-` and compare segments

**Examples caught:**
- `maybank2u-login.com` -> close to `maybank2u.com.my` -> dangerous
- `appleid.com.my` -> close to `apple.com` -> suspicious (but this might be legitimate Apple Malaysia?)

**Pros:**
- Catches typosquatting variants against a much larger brand set
- Leverages existing Levenshtein infrastructure
- Can catch creative misspellings and dash-insertion attacks

**Cons:**
- Levenshtein is not great for prefix/suffix additions (adding "my" to "apple" is distance 2, which may or may not trigger depending on threshold)
- `applemy` vs `apple` has distance 2, but `apple` has length 5, so threshold is 1 -- this would NOT be caught
- Increasing thresholds to catch these cases would dramatically increase false positives
- Does not understand semantic patterns (brand + country code) -- just character similarity
- Performance concern if comparing against 100+ domains with Levenshtein for every request

**False positive risk:** Medium. Depends heavily on threshold tuning. Aggressive thresholds catch more impersonation but also flag innocent domains.

**Implementation effort:** Medium (1-2 days). Need to sync allowlist, tune thresholds, benchmark performance.

---

### Approach 5: Combined approach (Recommended)

Combine Approach 2 (country-suffix detection) + Approach 3 (unverified verdict) + selective parts of Approach 1 (expanded brand list). This gives layered protection without false positives.

**Layer 1 -- Expand brand keywords (from Approach 1, with safeguards)**
- Add global brands to the keyword list but ONLY brands with 5+ characters to avoid short-word false positives
- Apply the existing `checkBankNamePattern` logic to the expanded list
- Keep the current behavior: suspicious on normal TLDs, dangerous on suspicious TLDs

**Layer 2 -- Country-suffix pattern detection (from Approach 2)**
- New check function: `checkCountrySuffixPattern`
- If domain matches `{brand}{country_code}` on ANY TLD, return "dangerous" at 0.92 confidence
- This catches `applemy.com`, `grabsg.com`, `netflixid.xyz` with high precision
- Very low false positive risk because the pattern is so specific

**Layer 3 -- "Unverified" verdict for unknown domains (from Approach 3)**
- Change the 0.5-confidence fallback from "safe" to "unverified"
- Website and app show neutral messaging: "This domain is not in our database. Be cautious with personal information."
- Users still see clear green for known-safe domains (confidence 1.0 from allowlist match)

**Pros:**
- Defense in depth: three layers catching different attack vectors
- Country-suffix detection catches the exact SEA-targeted pattern with near-zero false positives
- "Unverified" verdict eliminates the misleading "safe" label for unknown domains
- Expanded brand list catches non-country impersonation too (with appropriate severity)
- Each layer is independently useful and can be shipped incrementally

**Cons:**
- Most implementation effort of all options
- "Unverified" verdict requires UI changes across all surfaces
- Still will not catch highly creative impersonation that does not follow known patterns

**False positive risk:** Low. Country-suffix detection is very precise. Expanded brand list may generate some false "suspicious" results but not false "dangerous" results. "Unverified" is not a false positive by definition.

**Implementation effort:** Medium-high (3-5 days total). Can be shipped in phases:
- Phase 1: Country-suffix detection (2 hours, highest impact)
- Phase 2: Expand brand keywords (1 hour, incremental improvement)
- Phase 3: "Unverified" verdict (2-3 days, requires multi-surface changes)

---

## Recommendation

**Ship Approach 2 (country-suffix detection) first as a standalone change.** It is the highest-impact, lowest-risk improvement:

1. It directly addresses the `applemy.com` / `grabsg.com` pattern that motivated this brainstorm
2. It has near-zero false positive risk because legitimate brands never use the `brandcountry.com` pattern
3. It can be implemented in under 2 hours as a new function in `heuristics.ts`
4. It works on ALL TLDs including `.com`, solving the current gap where `.com` domains get under-penalized

**Then ship Approach 1 (expanded brand list) as a fast follow.** Adding global brand keywords to the existing list is trivial and catches `apple-login.com` style attacks that country-suffix detection misses. Apply a minimum keyword length of 5 characters to avoid false positives from short words.

**Defer Approach 3 (unverified verdict) to a later sprint.** It is the right long-term solution but requires coordinated changes across backend, website, and Android app. It is also a UX decision that needs design input -- what does the "unverified" screen look like? What is the messaging? This should be planned as a proper feature, not a quick fix.

**Skip Approach 4 (fuzzy matching expansion) for now.** The existing Levenshtein check works well for typosquatting. Expanding it to the full allowlist adds complexity and performance concerns without catching patterns that Approaches 1 and 2 miss. Revisit if we see scam domains that use creative misspellings of global brands.
