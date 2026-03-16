# Safe Anot? - Market Research Report

> **Date:** 2026-03-16
> **Status:** Initial Research

---

## 1. Market Size & Opportunity

### Mobile Security Market
- Global mobile security market: **USD 7.86 billion (2026)**, projected to reach **USD 97.68 billion by 2033** (CAGR 21.1%)
- Mobile anti-malware market: **USD 8.02 billion (2025)**, projected to reach **USD 26.65 billion by 2033** (CAGR 16.2%)
- Fraud detection & prevention market: **USD 67.12 billion (2026)**, projected to reach **USD 243.72 billion by 2034** (CAGR 17.5%)

Sources: [The Business Research Company](https://www.thebusinessresearchcompany.com/report/mobile-security-global-market-report), [SkyQuest](https://www.skyquestt.com/report/mobile-anti-malware-market), [Fortune Business Insights](https://www.fortunebusinessinsights.com/industry-reports/fraud-detection-and-prevention-market-100231)

### The Scam Problem (Scale)
- **$18-37 billion** in annual losses from cyber-enabled fraud in Southeast Asia alone (UNODC)
- **RM 1.12 billion** lost to online scams in Malaysia
- Meta removed **6.8 million WhatsApp accounts** linked to scam operations in H1 2025
- Scam centers primarily based in SE Asia, targeting victims globally
- Telegram-based marketplace Huione Guarantee facilitated **$27 billion** in transactions (2021-2025), successor platforms processing ~$2B/month

Sources: [Meta/WhatsApp](https://about.fb.com/news/2025/08/new-whatsapp-tools-tips-beat-messaging-scams/), [TechNave](https://technave.com/gadget/RM1-12-billion-lost-to-online-scams-in-Malaysia--NSRC-expands-to-24-7-operations-43760.html), [Eydle](https://www.eydle.com/telegram-scams-on-the-rise-across-southeast-asia/)

---

## 2. Competitor Analysis

### 2.1 Detailed Competitor Profiles

#### ScamShield (Singapore Government)
- **Publisher:** Open Government Products (OGP) + Singapore Police Force (SPF) + NCPC
- **Platform:** Android & iOS
- **Downloads:** ~790K (~13% penetration in Singapore's 5.9M population)
- **Price:** Free
- **Rating:** Available on [Google Play](https://play.google.com/store/apps/details?id=sg.gov.scamshield&hl=en_SG) and [App Store](https://apps.apple.com/sg/app/scamshield/id1497144087)

**Features:**
- Block known scam phone numbers
- Filter scam SMS to junk folder (iOS) / notify about scam SMS (Android)
- Check suspicious messages, links, and phone numbers
- Upload screenshots from SMS, WhatsApp, Telegram for scam checking
- Report scam calls, messages, and links
- Notifications when a reported number is officially confirmed as scam by authorities

**Limitations:**
- Only scans SMS from **unknown numbers** — cannot read messages from saved contacts
- Android version can only **notify** about scam SMS, cannot auto-filter to junk (OS limitation)
- Call blocking depends on device/telco settings — not fully under app's control
- Reporting in-app is **not** the same as filing an official police report
- **Does NOT audit device settings or "Install unknown apps" permissions**
- **Does NOT guide users to harden their device against sideloading**

**Our differentiation:** ScamShield is *reactive* (check after receiving a suspicious message). Safe Anot? is *proactive* (harden the device before a scam APK can ever be installed).

Sources: [ScamShield](https://www.scamshield.gov.sg/about-scamshield/), [ScamShield FAQ](https://ask.gov.sg/scamshield), [NCPC](https://www.ncpc.org.sg/aboutscamshield.html)

---

#### Truecaller
- **Publisher:** Truecaller (True Software Scandinavia AB)
- **Platform:** Android & iOS
- **Downloads:** 500M+
- **Price:** Free with premium tiers

**Features:**
- Caller ID — identify unknown callers from global database
- Spam/scam call blocking with real-time database updates from millions of users
- AI Call Scanner — analyzes voice in real-time to detect AI-synthesized voices (deepfake calls)
- AI Assistant — screens calls with 90%+ spam/scam accuracy
- SMS spam marking with red warning notifications
- Dangerous links in SMS are automatically disabled
- Block callers by country, number sequence, robocalls, unknown Caller ID

**Limitations:**
- Focused exclusively on **calls and SMS**
- No APK/sideload protection whatsoever
- No device security audit capability
- No guidance on disabling "Install unknown apps"
- Heavy app with significant permissions footprint

**Our differentiation:** Truecaller doesn't touch the install-source problem at all. Different threat vector entirely.

Source: [Truecaller on Google Play](https://play.google.com/store/apps/details?id=com.truecaller&hl=en)

---

#### Appdome (B2B SDK)
- **Type:** Enterprise SDK / no-code mobile security platform (not a consumer app)
- **Target:** App developers and enterprises

**Features:**
- Detects when "Install from unknown sources" is enabled on a device
- Prevents malware from leveraging untrusted distribution channels
- Jailbreak/root detection
- No-code integration for mobile app security

**Limitations:**
- **Not a standalone consumer app** — must be integrated into other apps by developers
- Enterprise pricing / B2B sales cycle
- End users don't interact with Appdome directly

**Our differentiation:** Appdome validates our technical approach (detecting unknown-source settings is a real security vector). Safe Anot? brings this capability directly to consumers as a standalone app.

Source: [Appdome Unknown Sources Detection](https://www.appdome.com/how-to/mobile-app-security/jailbreak-root-detection/detect-unknown-sources-protect-android-apps/)

---

#### Google Play Protect (Platform-Level)
- **Publisher:** Google
- **Coverage:** 2.8 billion Android devices, 185 markets
- **Price:** Built into Android (free)

**Features:**
- Scans 350 billion+ apps daily
- Blocked 1.75M policy-violating apps from Play Store in 2025
- Enhanced Fraud Protection: auto-blocks installs from browsers/messaging apps requesting sensitive permissions
- Blocked 266M risky sideload installation attempts in 2025
- Protected users from 872K unique high-risk applications
- New 2025: prevents users from disabling Play Protect during phone calls (anti-social-engineering)
- Real-time code-level scanning on novel apps

**Limitations:**
- Works **silently in the background** — users have no visibility into which apps can sideload
- Cannot show users a dashboard of their install-source risk
- No guided remediation flow
- Enhanced Fraud Protection is automatic, not user-controllable
- Users don't know their risk posture

**Our differentiation:** Play Protect is invisible infrastructure. Safe Anot? makes the risk **visible and actionable** — showing users exactly which apps can sideload APKs and guiding them to fix it. We complement Play Protect, not compete with it.

Sources: [Google Security Blog](https://security.googleblog.com/2026/02/keeping-google-play-android-app-ecosystem-safe-2025.html), [Google Play Protect](https://support.google.com/googleplay/answer/2812853)

---

#### Verify Scams
- **Publisher:** VerifyScams
- **Platform:** Android
- **Downloads:** N/A (small)

**Features:**
- AI-powered scam detection platform
- Check suspicious content for scam patterns

**Limitations:**
- Small/niche app with limited traction
- No device security audit

---

#### APK Protector (Damylola)
- **Publisher:** Damylola
- **Platform:** Android

**Features:**
- Protects APKs from being modded/repackaged

**Limitations:**
- **Developer tool**, not a consumer protection app
- Prevents reverse engineering of APKs, not scam prevention
- Not a competitor in our space

---

### 2.2 Indirect Competitors (Antivirus/Security Suites)

| App | Publisher | Downloads | Relevant Features | Limitations vs Safe Anot? |
|-----|-----------|-----------|-------------------|--------------------------|
| **AVG AntiVirus** | AVG Mobile | 100M+ | Scam site blocking, real-time app scanning, password leak alerts | No install-source audit, heavy, many permissions |
| **Bitdefender** | Bitdefender | 10M+ | Malware scanning, web protection, scam alerts | Post-install only, no sideload prevention |
| **Malwarebytes** | Malwarebytes | 50M+ | Malware detection, ad/tracker blocking | Post-install scanning only |
| **Protectstar Anti Spy** | Protectstar | 1M+ | Spy app detection, AI-based threat scanning | Focused on spyware, not sideload sources |

**Key weakness of all AV apps:** They scan **after** an app is installed. Safe Anot? prevents the risk **before** installation by hardening the device settings.

### 2.3 Government Tools

| Tool | Country | Type | Downloads/Usage | Features | Gap |
|------|---------|------|-----------------|----------|-----|
| **ScamShield** | Singapore | Mobile app | ~790K downloads | Call/SMS blocking, scam checking, reporting | No device hardening |
| **SemakMule** | Malaysia | Web portal | 1.9M+ searches | Check bank accounts & phone numbers against fraud database (193K+ flagged accounts, 164K+ suspicious numbers) | Web only, no app, no device audit |
| **Enhanced Fraud Protection** | Singapore (CSA) | Android OS feature | Pilot (Feb 2024) | Blocks installs requesting sensitive permissions from sideloading sources | Silent/automatic, no user visibility |
| **Safe App Portal** | Singapore (CSA) | Web portal | Pilot (Oct 2025) | Security insights for app developers | Developer-facing, not consumer |

### 2.4 Enterprise MDM Solutions

| Solution | Type | Relevant Capability |
|----------|------|---------------------|
| **Samsung Knox** | MDM | Block unknown sources globally on managed devices |
| **Nomid MDM** | MDM | Disable "Install from Unknown Sources" across device fleets |
| **VMware Workspace ONE** | MDM | Enterprise app management, sideload prevention |
| **Google Android Enterprise** | MDM | Work profile policies, app allowlisting |

**Not competitors** — these require enterprise enrollment. Not available to individual consumers.

### 2.5 Competitive Positioning Matrix

| Capability | ScamShield | Truecaller | AV Apps | Play Protect | MDM | **Safe Anot?** |
|---|---|---|---|---|---|---|
| Block scam calls/SMS | Yes | Yes | Some | No | No | No (not our focus) |
| Check suspicious links/messages | Yes | No | Some | No | No | v2 |
| Audit "Install unknown apps" per source | No | No | No | Silent only | Enterprise only | **Yes** |
| Guide user to disable per app | No | No | No | No | Admin only | **Yes** |
| Play Protect status check | No | No | No | N/A | Some | **Yes** |
| Security score dashboard | No | No | Yes | No | Yes | **Yes** |
| Periodic recheck reminders | No | No | Some | Silent | Yes | **Yes** |
| Zero dangerous permissions | Yes | No | No | N/A | No | **Yes** |
| Works on unmanaged personal devices | Yes | Yes | Yes | Yes | No | **Yes** |

### 2.6 Competitive Gap Summary

**No existing app focuses specifically on auditing and guiding users to disable "Install unknown apps" permissions per source app.** This is our unique positioning:

1. **ScamShield** focuses on call/SMS filtering and scam checking — reactive, not preventive
2. **Truecaller** focuses on caller ID and call blocking — different threat vector
3. **Antivirus apps** focus on malware scanning after install — too late in the kill chain
4. **Google Play Protect** works silently — no user visibility or guided remediation
5. **Government tools** focus on reporting and checking known scam identifiers — not device hardening
6. **Enterprise MDM** can enforce policies — but not available to consumers
7. **Appdome SDK** detects unknown sources — but only as an embedded SDK, not a consumer app

**Safe Anot? fills the pre-install prevention gap** — hardening the device before a scam APK can be installed. We are the only consumer-facing app that makes install-source risk visible and actionable.

---

## 3. Google Play Ecosystem Context (2025)

- Google Play Protect scans **350 billion+ apps daily**
- **1.75 million** policy-violating apps blocked from Play Store in 2025
- **80,000** developer accounts banned
- Enhanced Fraud Protection blocked **266 million** risky sideload installation attempts
- Protected users from **872,000 unique high-risk applications**
- New feature: prevents users from disabling Play Protect during phone calls (anti-social-engineering)

Sources: [Google Security Blog](https://security.googleblog.com/2026/02/keeping-google-play-android-app-ecosystem-safe-2025.html), [Google Blog](https://blog.google/products-and-platforms/platforms/google-play/how-we-kept-google-play-safe-in-2025/)

**Implication:** Google is investing heavily in platform-level protection, but there's still a gap in user-facing audit tools. Safe Anot? complements Play Protect rather than competing with it.

---

## 4. Regulatory Landscape

### Singapore
- **ScamShield app** — government-built, 790K downloads
- **Enhanced Fraud Protection (EFP)** — CSA pilot (Feb 2024) blocks installs requesting sensitive permissions
- **Safe App Portal** — CSA pilot (Oct 2025) provides security insights for app developers
- **Online Criminal Harms Act (OCHA)** — Directives issued to Meta, Apple, Google (Sep 2025, Jan 2026) requiring removal of scam content
- Anti-scam efforts coordinated across SPF, NCPC, CSA, IMDA, MAS

Sources: [MDDI](https://www.mddi.gov.sg/other-pages/anti-scam-efforts/), [ScamShield](https://www.scamshield.gov.sg/), [MHA](https://www.mha.gov.sg/media-room/newsroom/committee-of-supply-debate-2026-on-advancing-the-whole-of-society-effort-to-fight-scams/)

### Malaysia
- **SemakMule portal** — PDRM CCID tool for checking suspected scam accounts/numbers
- **National Scam Response Centre (NSRC)** — 24/7 operations since Jul 2025, under Ministry of Home Affairs
- **Cyber Security Act 2024** — comprehensive cybersecurity governance framework
- **Budget 2025** — RM 20 million allocated to NSRC
- Joint operations with Singapore & Hong Kong police to disrupt scam syndicates

Sources: [SemakMule](https://semakmule.rmp.gov.my/), [Lexology](https://www.lexology.com/library/detail.aspx?g=dad0de92-154a-428a-b3f3-49623632a620)

### Regional (ASEAN)
- UNDP published **Anti-Scam Handbook v2.0** (May 2025)
- Cross-border law enforcement cooperation increasing
- Growing regulatory pressure on messaging platforms (Meta, Telegram)

**Opportunity:** Governments are actively investing in anti-scam measures. Safe Anot? could partner with or complement government initiatives, especially in Malaysia where no equivalent to ScamShield exists as an app.

---

## 5. User Demand Signals

- WhatsApp APK scams are a **top concern in SE Asia** — Meta had to remove 6.8M accounts
- "Pig butchering" scams via WhatsApp/Telegram are a major and growing threat
- Elderly and less tech-savvy users are primary victims
- Malaysia lost RM 1.12 billion to scams — public awareness is high
- Singapore's ScamShield has 790K downloads in a country of 5.9M people (~13% penetration) — showing strong demand
- Google's own data shows 266M blocked sideload attempts — the problem is massive

---

## 6. Monetization Models

Based on how similar apps and services monetize:

| Model | Description | Examples | Fit for Safe Anot? |
|-------|-------------|----------|-------------------|
| **Free (ad-supported)** | Free app with ads | Most AV apps (free tier) | Good for user acquisition |
| **Freemium** | Basic audit free, premium features paid | AVG, Bitdefender | Good — free audit, paid monitoring/alerts |
| **B2B SDK** | SDK for banks/fintechs to check device posture | Specialized security vendors | High value, longer sales cycle |
| **Enterprise/MDM** | Managed device protection for companies | VMware, Knox | Good for SME market |
| **Government partnership** | White-label or integrate with gov anti-scam efforts | ScamShield model | Strong opportunity in MY |
| **Family plan** | Subscription for monitoring family devices | Google Family Link model | Good for elder protection |

### Recommended monetization strategy:
1. **v1: Free** — maximize downloads, build trust and reputation
2. **v2: Freemium** — free audit + paid features (auto-reminders, family monitoring, detailed reports)
3. **v3: B2B SDK** — license to banks/wallets/trading apps for device risk assessment
4. **Parallel: Government partnerships** — approach NACSA/PDRM Malaysia for integration or endorsement

---

## 7. Key Opportunities

1. **No direct competitor** in the "install source audit + guided remediation" space
2. **Malaysia market gap** — no ScamShield equivalent app exists; RM 1.12B in scam losses creates urgency
3. **Government alignment** — both SG and MY governments are actively investing in anti-scam; partnership potential
4. **Complement, don't compete** with Google Play Protect — position as user-facing education + hardening layer
5. **Elder/family protection** angle is underserved and has strong emotional appeal
6. **SE Asia first, global later** — scam problem is acute in the region, but the tool is universally applicable

---

## 8. Risks & Challenges

| Risk | Mitigation |
|------|------------|
| Google builds this into Android natively | Stay ahead with UX, education, and multi-vendor integration |
| Limited technical capability on unmanaged devices | Be transparent about limitations; focus on education + guidance |
| User acquisition cost for security apps | Partner with banks/telcos for distribution; leverage scam news cycle |
| Low monetization in free tier | Build toward B2B SDK as primary revenue stream |
| Privacy concerns from users | Zero dangerous permissions in v1; all data local; transparent privacy policy |
