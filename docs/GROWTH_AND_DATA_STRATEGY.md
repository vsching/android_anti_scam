# Safe Anot? — Growth Strategy & Data Sources

> **Date:** 2026-03-16
> **Core Principle:** Phase 1 is about viral growth and user acquisition. Phase 2 and 3 build on the user base with partnerships and community data.

---

## The Strategic Funnel

```
Phase 1: VIRAL GROWTH          → Free app, built-in sharing, TikTok content
Phase 2: PARTNERSHIPS           → PDRM, GASA, bank integrations, data access
Phase 3: COMMUNITY DATA MOAT    → Crowdsourced scam DB, API licensing, B2B SDK
```

Each phase unlocks the next. You can't get PDRM's attention without users. You can't build a data moat without a community. **Phase 1 is everything.**

---

## Part 1: Viral Growth Mechanisms (Phase 1)

### 1.1 Built-In Viral Loops

Every feature should have a sharing trigger:

| Trigger | Mechanic | Expected K-Factor |
|---------|----------|-------------------|
| **"Protect Your Parents"** | Child installs app on parent's phone → links as guardian → invites siblings to also be guardians | 1 parent = 2-3 guardian installs |
| **Security Score Share** | "My phone security score is 100%! Check yours" → shareable card to WhatsApp/TikTok/IG | Social proof + curiosity |
| **Scam Alert Share** | "Warning: new Maybank phishing scam detected" → shareable alert card | Urgency + helping friends |
| **"I Spotted a Scam" Badge** | After reporting a scam → share badge: "I helped protect 127 people from this scam" | Pride + social currency |
| **Family Leaderboard** | Family group score: "Voon family: 4/4 phones secured" → shareable | Family competition |
| **Fake Domain Checker Result** | "This link is DANGEROUS ⚠️" → shareable verdict card with screenshot | Most viral — people WILL share scary results |

**The #1 viral mechanic:** The **fake domain checker** result card. When someone checks a suspicious link and gets a big red "DANGEROUS" verdict, they will screenshot it and share it on WhatsApp groups and TikTok. This is free organic content.

### 1.2 Referral Program

| Element | Detail |
|---------|--------|
| Invite mechanism | Share unique referral link via WhatsApp/Telegram |
| Reward | Both get +1 month premium features (family monitoring, extra checks) |
| Deep linking | Deferred deep link survives Play Store install → attributes referral |
| Target K-factor | > 1.0 (each user brings at least 1 new user) |
| SDK | Use Branch.io or Firebase Dynamic Links for attribution |

### 1.3 In-App Sharing Surfaces

Every screen should have a share button:
- **Security Score** → "Share my score" → generates image card
- **Scam Alert** → "Warn my contacts" → pre-filled WhatsApp message
- **Fake Domain Result** → "Share this warning" → generates verdict card image
- **After fixing all items** → "My phone is secured! 🛡️" → celebration card
- **Family Guardian** → "Invite family member" → WhatsApp deep link

### 1.4 Advanced Viral Loops (15 Additional Mechanics)

Beyond the basic sharing surfaces, these are the deeper viral mechanics that drive exponential growth:

#### Tier 1 — High Impact, Easy to Build (v1-v1.1)

**1. Trusted Family Reply Assistant**
- **What:** After a DANGEROUS verdict, one-tap sends a polite warning reply back to the WhatsApp group: "This link is dangerous. Please don't click. Check with Safe Anot?"
- **Trigger:** Dangerous or suspicious verdict from a shared link
- **Why users share:** Reduces social friction of warning others — many people don't share because they don't know what to say. Pre-written replies in polite, urgent, and elder-friendly tones (BM/English/Chinese).
- **Growth driver:** The reply travels inside the original scam conversation, where ALL the highest-intent prospects already are. The app link is embedded in the warning.
- **K-factor multiplier:** Very high — goes directly to people who received the same scam.

**2. "Forwarded by Someone You Love" Rescue Card**
- **What:** After a dangerous verdict, generate a share card: "This scam was forwarded to me by someone I care about. Check your family's links before they click."
- **Trigger:** Immediately after a DANGEROUS result
- **Why users share:** Reframes from "I almost got scammed" to "I'm protecting my family." More socially acceptable than bragging.
- **Growth driver:** People in the same WhatsApp group received the exact same scam. Installs happen in clusters.

**3. "Could Your Parents Spot the Fake?" Challenge**
- **What:** A quiz game — guess which of two links is real: `maybank2u.com.my` vs `maybank2u-secure-login.xyz`. Shareable scorecard: "I got 5/5. Can your family beat me?"
- **Trigger:** After onboarding, after a safe verdict, or as a daily challenge
- **Why users share:** Competitive + curiosity. People love testing others with deceptively similar links.
- **Growth driver:** Challenge links playable WITHOUT installing (web-based). After playing → "Want full scam checks? Install Safe Anot?"
- **Platforms:** WhatsApp-native first, TikTok-friendly second.

**4. "This Scam Is Spreading Now" Live Counter**
- **What:** When a checked domain matches other recent submissions, show: "This scam was checked 48 times in the last 24 hours in Selangor."
- **Trigger:** Dangerous or suspicious verdict with cluster match
- **Why users share:** Real-time social proof makes the threat feel immediate and worth warning about.
- **Growth driver:** Creates urgency and legitimacy. Recipients think, "This isn't just one weird link. It's everywhere." Great for screenshots and TikTok voiceovers.
- **Tags:** Region + platform tags (WhatsApp, Telegram, Selangor, Singapore).

#### Tier 2 — High Impact, Medium Effort (v2)

**5. "Check Your Family Group" Burst Mode**
- **What:** Quickly check multiple suspicious links from one WhatsApp thread → share a summary card: "3 checked, 2 dangerous, 1 suspicious."
- **Trigger:** After the second suspicious link checked within a short window
- **Why users share:** Turns one scam into a group event. "I checked 3 links from our family chat, 2 were dangerous."
- **Growth driver:** Summary card is shared back into the EXACT thread where the scam is circulating → highest-intent installs.

**6. Family Panic Button**
- **What:** Big red button: "My parent is being scammed RIGHT NOW." Sends urgent WhatsApp to siblings/relatives with app link.
- **Trigger:** Dangerous verdict, APK risk detected, or guardian alert
- **Why users share:** Urgency overrides hesitation. In fear moments, people want backup, not content.
- **Growth driver:** Siblings install because the app is positioned as the coordination tool to protect the parent immediately.
- **Message:** "Please help check Mum's phone now. I found a dangerous scam link. Install this app and join guardian mode."

**7. WhatsApp Status-First Verdict Templates**
- **What:** 9:16 vertical cards designed specifically for WhatsApp Status (not TikTok). Large verdict, minimal text, local tone.
- **Trigger:** Dangerous/suspicious verdicts, scam alerts, family milestones
- **Why users share:** Many won't post to TikTok, but WILL post to WhatsApp Status where their close network sees it.
- **Growth driver:** Status viewers are warm leads: relatives, school parent groups, office contacts. Ideal for MY/SG distribution.
- **CTA:** "Got a weird link? Check with Safe Anot?"

**8. "Scam-Proof Your Parents in 60 Seconds" Challenge**
- **What:** Timed challenge — children secure a parent's phone, post before/after score or completion video.
- **Trigger:** Family visits, festive periods (Hari Raya, CNY, Deepavali), creator campaigns
- **Why users share:** TikTok-native. Transformation + urgency + family love + visible result.
- **Growth driver:** Viewers try it on their own parents. App becomes a family ritual, not just a utility.
- **Hashtag:** #ScamProofYourParents #SafeAnot

**9. Parent-Safe Morning Brief**
- **What:** Daily scam brief for elderly users: one scam type, one fake-vs-real example, one tap to share to children/spouse.
- **Trigger:** Daily at a fixed, non-intrusive time (9am)
- **Why users share:** Flips elderly from passive target to active protector. Parents FORWARD the brief to their children.
- **Growth driver:** Family members receiving briefs install to explain, monitor, or send checks back. Creates daily habit.
- **Design:** Large text, minimal jargon, bilingual, voice-readout support.

**10. "Before You Pay, Check" Invoice Mode**
- **What:** Special mode for links related to deliveries, bills, tax refunds, parking fines, bank actions. High-drama warnings.
- **Trigger:** Detection of payment/refund/parcel/OTP/suspension keywords in checked content
- **Why users share:** These are the EXACT emotional states where people panic and consult others.
- **Growth driver:** "If anyone gets a parcel/refund/payment link like this, check it here first." Broadens use case beyond hardcore security users.
- **Verdict cards:** Tailored by scenario: "Fake parcel fee scam", "Fake LHDN tax refund", with recognizable local brands.

#### Tier 3 — Network Effects (v3, needs user base)

**11. Neighborhood Scam Heatmap**
- **What:** Live map/feed: "Scams reported near you today" by city/state/district.
- **Trigger:** App open, after link check, daily push: "12 dangerous links reported in PJ today."
- **Why users share:** Fear becomes LOCAL. "This is happening in our area" is far more shareable than generic news.
- **Growth driver:** Screenshots shared in neighborhood, condo, mosque, church WhatsApp groups → hyperlocal installs.

**12. Family Safety Streaks**
- **What:** Shared family streak for consecutive days with no risky settings + at least one scam check completed.
- **Trigger:** Daily open, successful checks, guardian compliance
- **Why users share:** Accountability + family competition without public leaderboards.
- **Growth driver:** To keep the streak alive, users pull in parents/siblings. One inactive member pressures others to install.
- **Message:** "Your family is on a 9-day scam-safe streak!"

**13. Scam Hunter Identity**
- **What:** Reputation titles: "Family Protector", "Group Saver", "Scam Hunter", earned by checks shared and warnings that led to installs.
- **Trigger:** Successful shares, confirmed scam reports, referred installs
- **Why users share:** Social status. Being the "tech-savvy child who warns everyone" is a real identity.
- **Growth driver:** Badge signals utility, not spam. Share: "Protected 7 people from suspicious links this month."

**14. Community-Verified Scam Library**
- **What:** Browseable feed of real scams submitted by users, ranked by "most shared", "most reported", "targeting seniors."
- **Trigger:** Home feed, after checks, notifications about trending scams
- **Why users share:** "Look, this exact scam hit someone else already." Community content feels alive.
- **Growth driver:** More users = more scam examples = better feed. True network effect.

**15. Scam Comeback Stories**
- **What:** Users submit short stories: "My dad almost clicked this fake Maybank link." App turns them into anonymized, stylized story cards.
- **Trigger:** After dangerous verdict or after fixing a risky setting
- **Why users share:** Real stories outperform generic warnings. Relatable family situations drive emotional sharing.
- **Growth driver:** Viewers relate and install to test their own parents' phones.
- **Export:** TikTok/IG Reels/WhatsApp Status templates.

#### Viral Loop Priority Matrix

| Priority | Loop | K-Factor | Effort | Phase |
|----------|------|----------|--------|-------|
| P0 | Trusted family reply assistant (#1) | Very High | Low | v1 |
| P0 | "Forwarded by Someone You Love" card (#2) | High | Low | v1 |
| P0 | WhatsApp Status verdict templates (#7) | High | Low | v1 |
| P1 | "Spot the Fake" challenge (#3) | High | Medium | v1.1 |
| P1 | Live scam counter (#4) | High | Medium | v1.1 |
| P1 | "Scam-proof in 60s" TikTok challenge (#8) | High | Low | v1.1 |
| P1 | "Before you pay, check" mode (#10) | Medium | Medium | v1.1 |
| P2 | Family group burst mode (#5) | High | Medium | v2 |
| P2 | Family panic button (#6) | Medium | Medium | v2 |
| P2 | Parent morning brief (#9) | Medium | Medium | v2 |
| P2 | Family safety streaks (#12) | Medium | Medium | v2 |
| P3 | Neighborhood heatmap (#11) | High | High | v3 |
| P3 | Scam hunter identity (#13) | Medium | Medium | v3 |
| P3 | Community scam library (#14) | High | High | v3 |
| P3 | Scam comeback stories (#15) | Medium | Medium | v3 |

---

## Part 2: TikTok & Content Strategy

### 2.1 Why TikTok

- SE Asia = nearly **1/4 of TikTok's global ad audience** (largest regional base)
- Indonesia +71.7% ad-reach growth, Vietnam +86.8% in early 2025
- TikTok already partners with Singapore MHA/NCPC/SPF on scam prevention content
- Young adults (25-40) are the **decision-makers who install apps on parents' phones**
- Scam content is inherently shareable — emotional, scary, relatable

### 2.2 Content Pillars

| Pillar | Format | Example |
|--------|--------|---------|
| **"Is This Real?"** | Duet/stitch with scam messages | Show a real WhatsApp scam message → check it in Safe Anot? → reveal verdict |
| **"Check Your Parents' Phone"** | POV/lifestyle | "POV: You visit your parents and check their phone security score... it's 14% 😱" |
| **"Scam of the Week"** | Educational short | 30-sec breakdown of trending scam with real screenshots (redacted) |
| **"Fake vs Real"** | Side-by-side comparison | Show fake Maybank domain vs real one. "Can you spot the difference?" |
| **"I Almost Got Scammed"** | UGC storytelling | Real people sharing their near-miss stories (invite users to submit) |
| **"My Mak's Phone"** | Malaysian/Singaporean humor | Relatable content about parents clicking everything on WhatsApp |
| **"Domain Red Flag"** | Quick tip | "If the URL has 'maybank' but ends in '.xyz' — it's a SCAM" |

### 2.3 TikTok Execution Plan

| Phase | Timeline | Actions |
|-------|----------|---------|
| **Seed** | Pre-launch | Create 10 "Is This Real?" videos. Build anticipation. |
| **Launch** | Week 1-2 | Launch app + "Check Your Parents' Phone Challenge". Hashtag: #SafeAnot #ProtectYourParents |
| **Grow** | Month 1-3 | Weekly "Scam of the Week" series. Partner with 5-10 Malaysian/SG micro-influencers (10K-100K followers). |
| **Sustain** | Ongoing | UGC submissions ("I spotted a scam" stories). Duet with scam news. React to trending scam cases. |

### 2.4 Other Channels

| Channel | Strategy |
|---------|----------|
| **WhatsApp Status** | Shareable security score cards, scam alert images |
| **Facebook Groups** | Malaysian community groups (scam awareness, parenting, senior citizens) |
| **Instagram Reels** | Same content as TikTok, repurposed |
| **X (Twitter)** | Real-time scam alerts, engage with #scam #penipuan trending topics |
| **YouTube Shorts** | "Fake vs Real" domain comparisons, app demos |
| **Telegram Channels** | Malaysian/SG scam alert channels — share our data |
| **Reddit** | r/malaysia, r/singapore — scam awareness posts |

---

## Part 3: Fake Domain Detection (Key User-Facing Feature)

### 3.1 Why This Is Critical

Fake domains are the **#1 tool scammers use** to steal banking credentials in Malaysia:
- Fake Maybank2u login pages
- Fake CIMB, Public Bank, Affin Bank, BSN, RHB, Bank Islam, Hong Leong Bank domains
- Fake government domains (LHDN tax, JPJ, MySejahtera)
- Fake e-commerce (Shopee, Lazada) checkout pages

Malaysian banks have confirmed:
- [Maybank warns about fake executive emails and phishing domains](https://www.maybank2u.com.my/maybank2u/malaysia/en/personal/security_alert/phishing_scam.page)
- [8 Malaysian banks targeted by malicious fake apps](https://therecord.media/hackers-use-malicious-apps-to-target-customers-of-8-malaysian-banks-researchers-say)
- [CIMB phishing awareness page](https://www.cimb.com.my/en/personal/help-support/security-and-fraud/security-and-fraud-awareness/phishing.html)

### 3.2 Feature: "Is This Link Safe?" Domain Checker

**How it works:**

1. User **pastes a URL** or **shares it from WhatsApp/browser** to Safe Anot?
2. App **analyzes the domain** against multiple checks:

| Check | Method | Example |
|-------|--------|---------|
| **Known scam domain** | Match against Phishing.Database + Scam-Blocklist + Phishing Army | `maybank-secure-login.com` → KNOWN SCAM |
| **Bank impersonation** | Fuzzy match against legitimate bank domains | `maybank2u.com.my` (real) vs `maybank2u-verify.com` (fake) |
| **Government impersonation** | Match against official `.gov.my` / `.gov.sg` domains | `lhdn-refund.com` → FAKE (real is `hasil.gov.my`) |
| **Newly registered domain** | Check WHOIS age (if online) or flag unknown domains | Domain < 30 days old → SUSPICIOUS |
| **APK download link** | Regex: `.*\.apk$` or APK hosting patterns | `download.example.com/app.apk` → HIGH RISK |
| **URL shortener** | Detect bit.ly, tinyurl, etc. in banking/official context | `bit.ly/maybank-update` → SUSPICIOUS |
| **Typosquatting** | Levenshtein distance from known legitimate domains | `maybannk2u.com` → LIKELY FAKE |
| **Suspicious TLD** | Flag unusual TLDs for banking context | `.xyz`, `.top`, `.buzz`, `.click`, `.loan` → RED FLAG |

3. App shows a **clear verdict:**

```
🔴 DANGEROUS — Known scam domain
   This domain impersonates Maybank.
   Real Maybank website: maybank2u.com.my

   ⚠️ Do NOT enter any passwords or personal information.

   [Share This Warning]  [Report Scam]
```

or

```
🟢 SAFE — Verified official domain
   This is the official Maybank website.

   [OK]
```

### 3.3 Legitimate Domain Allowlist (Malaysia + Singapore)

We maintain a curated list of real domains:

**Malaysian Banks:**
| Bank | Official Domain(s) |
|------|-------------------|
| Maybank | `maybank2u.com.my`, `maybank.com.my`, `maybank.com` |
| CIMB | `cimb.com.my`, `cimbclicks.com.my` |
| Public Bank | `pbebank.com`, `publicbank.com.my` |
| RHB | `rhbgroup.com`, `rhb.com.my` |
| Hong Leong | `hlb.com.my`, `hlbb.hongleong.com.my` |
| Bank Islam | `bankislam.com.my` |
| Affin Bank | `affinbank.com.my`, `affinonline.com` |
| AmBank | `ambank.com.my`, `ambankgroup.com` |
| BSN | `mybsn.com.my`, `bsn.com.my` |
| Bank Rakyat | `bankrakyat.com.my` |

**Singapore Banks:**
| Bank | Official Domain(s) |
|------|-------------------|
| DBS | `dbs.com.sg`, `posb.com.sg` |
| OCBC | `ocbc.com` |
| UOB | `uob.com.sg` |
| Standard Chartered | `sc.com` |

**Malaysian Government:**
| Agency | Official Domain(s) |
|--------|-------------------|
| LHDN (Tax) | `hasil.gov.my` |
| JPJ (Transport) | `jpj.gov.my` |
| PDRM (Police) | `rmp.gov.my` |
| EPF/KWSP | `kwsp.gov.my` |
| MySejahtera | `mysejahtera.malaysia.gov.my` |
| MyGovernment | `malaysia.gov.my` |

**E-Commerce:**
| Platform | Official Domain(s) |
|----------|-------------------|
| Shopee | `shopee.com.my`, `shopee.sg` |
| Lazada | `lazada.com.my`, `lazada.sg` |
| Grab | `grab.com` |
| Touch 'n Go | `tngdigital.com.my` |

### 3.4 Why This Feature Goes Viral

When a user checks a suspicious link and sees:

> 🔴 **DANGEROUS — This domain impersonates Maybank**

They will:
1. **Screenshot it**
2. **Share it in their WhatsApp family group** — "See! I told you don't click!"
3. **Post it on TikTok/Facebook** — "Almost got scammed!"
4. **Tell friends** — "Download Safe Anot?, it caught this"

This is the **single most viral feature** we can build. Every scam attempt becomes free marketing for our app.

---

## Part 4: Scam Pattern Data Sources

### 4.1 Phase 1 — Free, No Partnership Needed (Ship with v1)

#### Phishing/Scam Domain Lists (Open Source)

| Source | Data Size | Update Frequency | Format | License |
|--------|-----------|-------------------|--------|---------|
| [Phishing.Database](https://github.com/Phishing-Database/Phishing.Database) | 100K+ domains | Continuous | Text lists | Open source |
| [Destroylist](https://github.com/phishdestroy/destroylist) | 770K+ domains | Hourly | JSON API (free, no key) | Open source |
| [Scam-Blocklist](https://github.com/jarelllama/Scam-Blocklist) | Thousands | Daily | Text lists | Open source |
| [Phishing Army](https://phishing.army/) | Aggregated | Every 6 hours | Text blocklist | Free |
| [Phishing Filter](https://github.com/curbengh/phishing-filter) | Curated | Twice daily | Multiple formats | Open source |
| [Threat Hostlist](https://github.com/PeterDaveHello/threat-hostlist) | Thousands | Regular | Host lists | Open source |
| [OpenPhish](https://openphish.com/) | Active phishing URLs | Regular | Free community feed | Free tier |

**Total: 500K-1M+ scam domains available for free.** We bundle a compressed snapshot (~5MB) in the APK and delta-sync weekly.

#### Scam Keyword Patterns (We Build)

Curated list of social engineering trigger phrases in English + Malay + Chinese:

**English:**
- "Install this update manually"
- "Download our secure app"
- "Your account will be suspended"
- "KYC verification required"
- "Bank security update — install now"
- "Your parcel is held — verify identity"
- "Investment opportunity — limited time"
- "Click here to claim your refund"

**Malay:**
- "Muat turun aplikasi keselamatan" (Download security app)
- "Akaun anda akan digantung" (Your account will be suspended)
- "Pasang aplikasi ini segera" (Install this app immediately)
- "Kemas kini keselamatan bank" (Bank security update)
- "Bungkusan anda ditahan" (Your parcel is held)
- "Peluang pelaburan terhad" (Limited investment opportunity)
- "Tuntut bayaran balik anda" (Claim your refund)

**Chinese (Simplified):**
- "请立即安装安全更新" (Please install security update immediately)
- "您的账户将被冻结" (Your account will be frozen)
- "下载此应用以验证身份" (Download this app to verify identity)
- "限时投资机会" (Limited time investment opportunity)

#### URL Risk Patterns (Regex)

```
# Direct APK downloads
.*\.apk$
.*\.apk\?.*

# URL shorteners in banking/official context
(bit\.ly|tinyurl\.com|t\.co|goo\.gl|is\.gd|rb\.gy)/.*

# Suspicious TLDs for banking impersonation
.+\.(xyz|top|buzz|click|loan|win|gq|ml|cf|tk|ga)$

# Bank name + random domain patterns
(maybank|cimb|rhb|publicbank|hlb|bsn|ambank|affin).*\.(com|net|org|info|xyz|top)$

# Government impersonation
(lhdn|jpj|kwsp|epf|pdrm).*\.(com|net|org|info|xyz)$

# Common scam URL structures
.*(verify|secure|update|login|confirm|unlock|suspended).*\.(com|net|xyz)$
```

#### Phone Number Data

| Source | Access | Data |
|--------|--------|------|
| [SkipCalls API](https://skipcalls.com/tools/spam-check-api) | Free REST API, no auth | 1M+ reported spam/scam numbers |
| Our own curated list | Manual | Known Malaysian/SG scam hotline impersonation numbers |

### 4.2 Phase 2 — Partnerships (After User Base Established)

| Partner | What We Get | What We Offer | How to Approach |
|---------|-------------|---------------|-----------------|
| **PDRM CCID (SemakMule)** | 193K+ flagged bank accounts, 164K+ suspicious phone numbers, real-time fraud database | User reach for scam alerts, data from user reports | Follow the Whoscall model — they got access in Jan 2023. Contact CCID directly. Reference: [Whoscall-PDRM partnership](https://cybersecurityasia.net/7-6-drop-in-scam-calls-whoscall-effort/) |
| **NACSA Malaysia** | Government endorsement, scam intelligence | Free distribution to Malaysian citizens, scam awareness reach | Propose as national anti-scam companion app |
| **GASA / Scam.org** | Global Signal Exchange scam data, AI-powered verification, 50+ languages | User reports feed into global database | Contact GASA: [gasa.org](https://gasa.org/). They actively seek partners. [Scam.org launched with OpenAI](https://www.newswire.com/news/global-anti-scam-alliance-launches-scam-org-with-openai-and-key-22743819) |
| **GovTech Singapore (ScamShield)** | ScamShield scam data, government backing | Complementary coverage (we do device hardening, they do call/SMS) | Position as complementary, not competitive |
| **Malaysian Banks** | Verified domain lists, fake domain reports, co-branding | In-app bank verification, customer protection | Approach security/fraud teams at Maybank, CIMB, Public Bank |
| **Telcos (Maxis, Celcom, Digi, U Mobile)** | Pre-install deals, scam number data | Reduced scam complaints, customer value-add | Propose as bundled security app |

### 4.3 Phase 3 — Community Data Moat

| Data Source | How It Grows | Value |
|-------------|-------------|-------|
| **User-reported scam numbers** | Every report adds to DB | Crowdsourced caller ID for MY/SG |
| **User-reported scam domains** | Every "Is This Safe?" check where user confirms scam | Real-time scam domain discovery |
| **User-reported scam messages** | Screenshots + OCR extraction | Scam pattern training data |
| **Fake domain discovery** | Automated monitoring of newly registered domains impersonating Malaysian banks | Early warning system |
| **Community verification** | "Was this helpful?" feedback on verdicts | Improves accuracy over time |

**The moat:** Once we have 100K+ users reporting scams, our database becomes the most comprehensive Malaysia-specific scam intelligence source. This is licensable to banks, telcos, and government agencies.

---

## Part 5: Phase 1 Launch Checklist

### Must-Have for Launch (Viral Mechanics)

- [ ] **Security score shareable card** — beautiful image generated in-app, one-tap share to WhatsApp
- [ ] **"Is This Link Safe?" checker** — paste URL or share from WhatsApp, get verdict card
- [ ] **Verdict share card** — "🔴 DANGEROUS" or "🟢 SAFE" card with app branding, one-tap share
- [ ] **"Protect Your Parents" onboarding flow** — emotional hook: "Set up protection for someone you love"
- [ ] **Family Guardian invite** — WhatsApp deep link to install Safe Anot?
- [ ] **Scam of the Week notification** — weekly push with trending scam
- [ ] **Referral link** — "Invite friends, both get premium features"

### Must-Have for TikTok Launch

- [ ] 10 seed videos before app launch
- [ ] #SafeAnot #ProtectYourParents hashtag campaign
- [ ] 5 Malaysian micro-influencers (parenting/tech/finance niche, 10K-100K followers)
- [ ] "Check Your Parents' Phone" challenge template
- [ ] Fake vs Real domain comparison video series
- [ ] In-app "Share to TikTok" for verdict cards

### Growth Metrics to Track

| Metric | Target (Month 1) | Target (Month 3) |
|--------|-------------------|-------------------|
| Downloads | 10,000 | 50,000 |
| K-factor (viral coefficient) | > 0.8 | > 1.2 |
| Daily Active Users | 2,000 | 15,000 |
| "Is This Safe?" checks per day | 500 | 5,000 |
| Scam reports submitted | 100 | 2,000 |
| TikTok video views (total) | 500,000 | 5,000,000 |
| Guardian links created | 1,000 | 10,000 |

---

## Part 6: Revenue Timeline

| Phase | Timeline | Revenue Model | Target |
|-------|----------|---------------|--------|
| Phase 1 | Month 1-6 | **Free** — grow user base | 50K-100K downloads |
| Phase 2a | Month 6-12 | **Freemium** — family plan RM 9.90/month (3 guardians + unlimited checks + alert history) | 2% conversion = 1K-2K paying users |
| Phase 2b | Month 6-12 | **Government grant** — apply to NACSA/MDEC for anti-scam innovation funding | RM 50K-200K |
| Phase 3a | Month 12-18 | **B2B API** — scam intelligence API for banks/telcos | 2-3 enterprise clients |
| Phase 3b | Month 12-18 | **Bank partnerships** — white-label domain checker for banking apps | Per-device licensing |
| Phase 4 | Month 18+ | **Data licensing** — anonymized scam trend data to government/research | Recurring revenue |
