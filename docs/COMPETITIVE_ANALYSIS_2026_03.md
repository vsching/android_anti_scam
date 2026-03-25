# Safe Anot? — Competitive Analysis & Pivot Research

> **Date:** 2026-03-25
> **Status:** Active Research
> **Conclusion:** All core features commoditized. Pivot needed.

---

## 1. Competitive Landscape

### SemakMule (MY Government, Free)
- 37M+ visits since 2020
- Check bank accounts (299K+), phone numbers (233K+), company names (7.8K+)
- Free, no registration, 24/7
- Source: [The Star](https://www.thestar.com.my/news/nation/2025/10/30/semak-mule-portal-records-over-37-million-visits-in-fight-against-online-scams), [The Vibes](https://www.thevibes.com/articles/news/114814/pdrms-semak-mule-portal-attracts-over-37-million-visitors-since-2020)

### Whoscall (PDRM-endorsed)
- Caller ID + spam blocking, officially endorsed by Malaysian police
- Auto-Web Checker — real-time malicious URL flagging
- Content Checker — screenshot scanning for threats
- ID Security Check — data breach alerts
- AI-powered via Google Gemini partnership
- Launching rewards system for scam reporting (Q4 2025)
- "Scam Free Malaysia 2025" campaign with PDRM and J&T Express
- Source: [Malaysiakini](https://www.malaysiakini.com/announcement/743937), [BusinessToday](https://www.businesstoday.com.my/2025/09/28/ai-partnership-powers-next-phase-of-scam-detection/)

### Truecaller Family Protection (Piloting in MY as of Dec 2025)
- Up to 5 family members in trusted group
- Family Admin: set protection levels, manage blocklists, remote call ending (Android)
- Real-time alerts, battery/availability status signals
- Free tier + Premium Family plan (up to 5 people)
- Pilot countries: Sweden, Chile, **Malaysia**, Kenya
- Expanding to India Q1 2026, more regions to follow
- Source: [TechCrunch](https://techcrunch.com/2025/12/09/truecaller-now-lets-users-protect-households-from-scam-calls/), [Yahoo Finance](https://finance.yahoo.com/news/truecaller-family-protection-tool-reshape-042156424.html)

### ScamShield (SG Government)
- Check calls, websites, messages (SMS, WhatsApp, Telegram)
- Screenshot upload for scam detection
- Call/SMS blocking with SPF blacklisted numbers
- Community reporting with feedback loop (notifies when reported number confirmed as scam)
- Suite: app + helpline (1799) + website + WhatsApp alert channel
- Source: [ScamShield](https://www.scamshield.gov.sg/), [GovTech](https://www.tech.gov.sg/products-and-services/for-citizens/scam-prevention/scamshield/)

### Google Android Developer Verification (Announced Aug 2025)
- All apps on certified devices must be from verified developers
- First wave: Brazil, Indonesia, **Singapore**, Thailand (Sep 2026)
- Worldwide rollout: 2027+
- Directly undermines Phone Shield feature
- Source: [Android Developers Blog](https://android-developers.googleblog.com/2025/08/elevating-android-security.html)

### Kaspersky Notification Protection (Launched 2025)
- Scans all Android notifications for malicious links (WhatsApp, Telegram, SMS)
- Uses NotificationListenerService (Play Store approved)
- Replaces scam notifications with warnings before user opens app
- Works with screen off and Do Not Disturb mode
- 166K+ users activated
- Paid product (RM100+/year)
- Source: [Kaspersky Blog](https://www.kaspersky.com/blog/notification-listener-in-kaspersky-for-android/54466/)

---

## 2. Feature Overlap Assessment

| Safe Anot? Feature | Covered By | Gap Remaining |
|---|---|---|
| Link Checker | Whoscall (AI + screenshot), ScamShield, SemakMule | None meaningful |
| Phone Shield | Google OS-level (2026), Whoscall | Obsolete by Sep 2026 (SG), 2027+ (global) |
| Family Guardian | Truecaller Family Protection (piloting in MY) | None — Truecaller does more |
| Scam Alerts | Whoscall, government channels, news | None meaningful |
| SemakMule data | SemakMule itself (37M visits, free) | None — we just wrap their data |

---

## 3. Market Context (2025-2026)

- **RM2.7B** lost to scams in Malaysia in 2025 (up 76% from 2024) — [The Star](https://www.thestar.com.my/business/business-news/2026/03/24/malaysia-scam-losses-rise-to-rm27bil-in-2025-spike-during-festive-seasons)
- **85%** of Malaysian adults encountered a scam, **73%** fell victim — [Fintech News](https://fintechnews.my/54004/security/scams-malaysia/)
- **140** scam attempts per person per year — [Malay Mail](https://www.malaymail.com/amp/news/malaysia/2025/10/02/with-malaysians-each-facing-140-scam-bids-a-year-experts-call-for-urgent-and-concerted-response/193091)
- **56%** of scam attempts via messaging apps (WhatsApp, Telegram, Facebook)
- **Only 13%** of victims recover any money — [GASA](https://gasa.org/knowledge-base/blog/comprehensive-study-reveals-devastating-impact-of-scams-on-malaysian-families-and-mental-health)
- "Malaysia's scam economy is not a public awareness problem but a system design problem" — [Malay Mail](https://www.malaymail.com/news/what-you-think/2026/03/02/malaysias-scam-economy-is-not-a-public-awareness-problem-but-a-system-design-problem-galvin-lee-kuan-sian/210929)
- 5,500+ senior citizens lost RM552M+ to online fraud (2021-2023)
- Children as young as 7 recruited as money mules — [Inquirer](https://globalnation.inquirer.net/289449/scammers-preying-on-children-as-young-as-7-in-malaysia-for-easy-money)

---

## 4. Identified Market Gaps

### Gap 1: Awareness-Action Gap
74% of Malaysians think they can spot scams, yet 73% still fall victim. Existing tools focus on detection (check this link, block this call) but scammers adapt faster than databases. No one does behavioral training/simulation for consumers.

**Proposed solution:** Scam Simulator ("Kena Test") — interactive phishing simulations mimicking real MY scam patterns (fake Maybank TAC, LHDN refund, Shopee delivery). Monthly "Scam IQ Score."

**Data source:** Does NOT need new crawlers. Needs scam pattern templates from existing alerts feed + user reports. LLM can parameterize templates.

### Gap 2: No Real-Time In-Chat Protection
56% of scams arrive via WhatsApp/Telegram. Users must manually copy links to check. No free solution monitors notifications in real-time.

**Proposed solution:** Notification Shield using Android's [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService) — same approach Kaspersky uses (166K+ activated). Scans incoming notifications for URLs, checks against on-device database (existing Bloom filter + SQLite), replaces scam notifications with warnings. No Accessibility Service needed (Play Store compliant).

**Differentiator vs Kaspersky:** Free, privacy-first (no data leaves device), Malaysia-focused database.

### Gap 3: Victim Recovery — The Critical First Hour
Only 13% of victims recover money. NSRC 997 hotline is 8am-8pm only. No app provides a guided emergency response.

**Proposed solution:** "Kena Scam" Emergency Mode — step-by-step wizard:
1. Screenshot everything before scammer deletes chat
2. One-tap dialer to bank fraud hotlines (all MY banks)
3. Call NSRC 997 / police 999
4. Nearest police station locator
5. SemakMule check on the scammer's account
6. Account security checklist

### Gap 4: Privacy-First Alternative
Truecaller and Whoscall both require uploading contacts to their servers. No privacy-first alternative exists in this space.

**Proposed solution:** Market existing on-device architecture as a feature. "Safe Anot? never sees your contacts, messages, or browsing history."

### Gap 5: Children Recruited as Money Mules
Scammers targeting kids as young as 7. No consumer app addresses this.

**Proposed solution:** "Jangan Jadi Keldai" gamified module targeting teens. Scenarios based on real MY cases. School partnership angle.

---

## 5. Pivot Thesis

Reposition from "scam checker" (commoditized) to:

> **"Malaysia's scam survival app"** — real-time protection + emergency response + behavioral training

Three defensible pillars:
1. **Notification Shield** — free Kaspersky alternative, privacy-first, on-device
2. **Kena Scam Emergency Mode** — guided first-hour response (nobody else does this)
3. **Scam Simulator** — behavior change, not just awareness

---

## 6. Phone Shield Technical Limitation

> **Tested:** 2026-03-25
> **Finding:** Phone Shield cannot actually detect permission state. Feature is a guided checklist, not a scanner.

### The Problem

There is **no Android API** to check if another app has "Install unknown apps" permission enabled/disabled. The permission state of other apps is intentionally sandboxed by Android.

| What's possible | API | Status |
|---|---|---|
| Check if an app is **installed** | `PackageManager.getPackageInfo()` | Works (current code does this) |
| Check **own app's** install permission | `canRequestPackageInstalls()` | Works, but irrelevant |
| Check if **WhatsApp/Chrome/etc** has install permission enabled | No API exists | **Impossible** |

### What Phone Shield Actually Does

1. Calls `getPackageInfo()` to detect if each of 20 tracked apps is installed
2. Marks all installed apps as `NEEDS_REVIEW` (not based on actual permission state)
3. Opens system settings via `ACTION_MANAGE_UNKNOWN_APP_SOURCES` intent
4. Shows confirmation dialog: "Did you disable it?"
5. If user clicks "Yes" → marks as `SECURED` — **trusts user input with no verification**
6. No re-verification — once marked `SECURED`, stays `SECURED` forever

### Can It Be Fixed?

| Workaround | Viable? | Why not |
|---|---|---|
| Accessibility Service | No | Play Store restricts heavily; same technique malware uses |
| Device Admin / MDM | No | Enterprise-only, requires device owner privileges |
| Root access | No | Not viable for consumer app |
| `canRequestPackageInstalls()` | No | Only checks your own app's permission, not other apps |

### Implications

- Security score is based on **self-reported data**, not actual device state
- Cannot be marketed as "detection" or "scanning"
- Feature is fundamentally a **guided education checklist** — still helpful, but not a differentiator
- Reinforces need to pivot away from Phone Shield as a core feature

### Relevant Code Files

- `android/.../util/PackageChecker.kt` — only checks installation, not permission state
- `android/.../data/repository/AuditRepositoryImpl.kt` — maps INSTALLED → NEEDS_REVIEW (no permission check)
- `android/.../feature/fix/FixViewModel.kt` — user confirmation flow with no re-verification
