# Scam Intelligence Sources — Crawlable Data Catalog

> **Date:** 2026-03-25
> **Purpose:** Proactive scam pattern detection (detect NEW scam types before users report them)
> **Problem:** Current pipeline only crawls domain blocklists. Knows *which* URLs are bad, but not *what type* of scam is trending or *what messages* scammers are using.

---

## TIER 1 — Government & Official (Easy, High Authority)

### 1. MyCERT Advisories
- **URL:** https://www.mycert.org.my/portal/advisories
- **What:** Cybersecurity advisories including scam/phishing alerts
- **Format:** HTML pages, no obvious RSS feed — need to scrape
- **Frequency:** Regular advisories (595+ published to date)
- **Data:** Advisory ID (MA-XXXX), date, title, description, affected platforms
- **Note:** Each advisory has a unique URL pattern: `mycert.org.my/portal/advisory?id=MA-XXXX`

### 2. BNM Financial Fraud Alerts
- **URL:** https://www.bnm.gov.my/financial-fraud-alerts
- **What:** Unauthorized companies, schemes, fraud warnings
- **Format:** HTML (403 on direct fetch — may need browser/Playwright scraping)
- **Frequency:** Updated regularly
- **Data:** Entity names, scheme types, warning details

### 3. PDRM Scam Alert Archive
- **URL:** https://www.rmp.gov.my/scam-alert
- **What:** Official police scam warnings
- **Format:** HTML with monthly archives dating back to 2012
- **RSS:** https://www.rmp.gov.my/feeds/sebut-harga (may be procurement, not scams — verify)
- **Frequency:** Monthly archives

### 4. PDRM CCID Portal
- **URL:** https://ccid.rmp.gov.my/
- **What:** Commercial Crime Investigation Department portal
- **Data:** Scam types, statistics, case studies

### 5. SemakMule Portal
- **URL:** https://semakmule.rmp.gov.my/
- **What:** Reported bank accounts, phone numbers, company names
- **Data:** 299K+ bank accounts, 233K+ phone numbers, 7.8K+ company names
- **Challenge:** No API, would need to monitor for trend spikes (not bulk scrape)

---

## TIER 2 — News RSS Feeds (Easiest, Fast Updates)

### 6. Malay Mail RSS
- **Malaysia feed:** https://www.malaymail.com/feed/rss/malaysia
- **All news:** https://www.malaymail.com/feed/rss
- **Tech:** https://www.malaymail.com/feed/rss/tech-gadgets
- **Singapore:** https://www.malaymail.com/feed/rss/singapore
- **Filter keywords:** "scam", "fraud", "phishing", "penipuan", "keldai akaun", "tipu", "sindiket"

### 7. Bernama RSS
- **URL:** https://www.bernama.com/en/rssfeed.php
- **What:** National news agency, covers official scam warnings and police statements
- **Filter:** Same keyword filtering approach

### 8. The Star
- **URL:** TBD (verify RSS availability)
- **What:** Major English-language daily, extensive scam coverage

### 9. Sinar Daily / Soya Cincau / Lowyat.NET
- **What:** Tech-focused outlets that cover scam news
- **Lowyat:** Particularly good for Malaysian tech scam coverage
- **Soya Cincau:** Covered MCMC SMS ban, scam app stories

---

## TIER 3 — Social Media (Medium Effort, Real-Time)

### 10. Instagram
- **@malaysiascamalert** — Dedicated Malaysian scam awareness account
- **@scammermalaysia** — 150+ identified scammers compiled
- **@cybercrimealertrmp** — Official PDRM Cyber Crime Alert Instagram
- **Approach:** Instagram Graph API or scraping for post content
- **Challenge:** API access requires Meta app review; scraping against ToS but feasible for public pages
- **Value:** Visual scam examples (screenshots of scam messages), trending patterns

### 11. Xiaohongshu / RedNote (小红书)
- **Context:** 4.3M users in MY+SG combined, growing rapidly
- **Scam content:** Users share scam warnings, fake product alerts, investment scam exposés
- **Challenge:** No official API, content mostly in Chinese — need Chinese language processing
- **Taiwan concern:** Taiwan suspended access citing 1,706 fraud cases since 2024
- **Value:** Chinese-speaking community scam patterns (large demographic in MY/SG)
- **Approach:** Scraping or manual curation; search hashtags: #骗局 #诈骗 #马来西亚骗局
- **Source:** [Marketing Interactive](https://www.marketing-interactive.com/unlocking-the-4-3-million-opportunity-on-xiaohongshu-for-malaysia-and-singapore)

### 12. Reddit
- **r/malaysia** — Active discussions about scams, victim stories
- **r/singaporefi** — Financial scam discussions
- **Approach:** Reddit API (PRAW for Python) or scraping services (Apify)
- **Search terms:** "scam", "kena scam", "penipuan", "phishing"
- **Value:** Detailed victim narratives with scam mechanics described
- **Rate limits:** Reddit API has rate limits; Apify/ScraperAPI as alternatives

### 13. Facebook
- **Cyber Crime Alert RMP** — https://www.facebook.com/CyberCrimeAlertRMP/ (176K+ likes, official PDRM)
- **Amaran Scam** — Recommended by BNM for fraud awareness
- **Scam Alert Malaysia** — Community groups
- **Challenge:** Facebook Graph API heavily restricted; requires app review
- **Value:** Most Malaysians are on Facebook — where scam warnings spread fastest
- **Note:** NSRC has NO official social media — any NSRC accounts on social media are fraudulent

### 14. Telegram
- **What:** Scam reporting channels/groups exist in MY
- **Approach:** Telegram Bot API for public channels
- **Value:** Raw forwarded scam messages (the actual scam content users receive)

### 15. Twitter/X
- **Hashtags:** #scammalaysia, #penipuan, #awarescam
- **Value:** Real-time scam reports
- **Approach:** X API (paid tiers required)

---

## Recommended Crawl Architecture

### Phase 1 — Build First (Weeks 1-2)
```
News RSS (Malay Mail, Bernama)
  + MyCERT Advisories (HTML scrape)
  + PDRM CCID Facebook page
         |
         v
    Keyword Filter (scam/fraud/phishing/penipuan/keldai)
         |
         v
    LLM Classification
    -> scam_type, platform, target_demographic, region, sample_message
         |
         v
    Scam Pattern Database (NEW table)
```

### Phase 2 — Add Social (Weeks 3-4)
```
Instagram (@malaysiascamalert, @cybercrimealertrmp)
  + Reddit (r/malaysia, r/singaporefi)
  + Xiaohongshu (Chinese-language scam posts)
         |
         v
    Same pipeline -> Scam Pattern Database
```

### Phase 3 — Real-Time (Month 2+)
```
Telegram channels
  + Twitter/X streams
  + Facebook Graph API (if approved)
         |
         v
    Real-time alerting -> Push notifications to users
```

---

## Languages to Support
- **English** — Primary for news sources
- **Bahasa Malaysia** — PDRM, government sources, Facebook
- **Chinese** — Xiaohongshu, Chinese-language scam posts targeting MY/SG Chinese community
- **Tamil** — Emerging: scam calls targeting Indian community

---

## LLM Classification Output Schema

```json
{
  "scam_type": "fake_bank_sms | investment | romance | parcel | tax_refund | job | loan | ...",
  "platform": "whatsapp | telegram | sms | facebook | instagram | phone_call | website",
  "target_demographic": "elderly | general | chinese_community | students | job_seekers",
  "region": "MY | SG | both",
  "severity": "high | medium | low",
  "sample_message_pattern": "Your {bank} account has been...",
  "red_flags": ["urgency", "asks_for_TAC", "unknown_link", "..."],
  "first_seen": "2026-03-25",
  "source_url": "https://..."
}
```
