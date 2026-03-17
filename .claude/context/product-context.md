---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# Product Context

## What Is Safe Anot?

Scam protection app for Malaysia and Singapore. Helps users check if links are malicious and audit their device for sideload vulnerabilities.

## Target Users

- **Primary**: Elderly and less tech-savvy users vulnerable to scams
- **Secondary**: Family members who want to protect relatives
- **Tertiary**: Tech-savvy users who want quick link verification

## Core Features

| Feature | Description | Epic |
|---------|-------------|------|
| Link Checker | Paste/share a URL, get instant safe/scam verdict | E02 |
| Phone Shield | Audit which apps can sideload APKs, guided fix | E03 |
| Scam Alerts | News feed of latest scam campaigns in MY/SG | E04 |
| Verdict Cards | Shareable image cards with check results | E07 |
| Family Guardian | Remote monitoring of family members' devices | E08 |

## Key Design Decisions

- **Device-first**: 95% of link checks happen on-device (SQLite + Bloom filter)
- **No account required**: App works without sign-up for core features
- **Free forever**: Core features remain free, monetize via premium/guardian
- **Viral by design**: Verdict cards designed for WhatsApp/Telegram sharing

## Market Context

- Malaysia: 25K+ scam cases/year, RM1.2B+ losses
- Singapore: ScamShield exists but is government-run, limited
- Gap: No community-driven, mobile-first scam checker for MY
