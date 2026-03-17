// Derived from migrations/0002_seed_alerts.sql — SQL is the single source of truth.
// A test assertion verifies this file stays in sync with the SQL seed.

export interface SeedAlert {
  id: string;
  title: string;
  description: string;
  scam_type: string;
  severity: string;
  region: string;
  report_count: number;
  created_at: string;
}

export const SEED_ALERTS: SeedAlert[] = [
  {
    id: 'alert-001',
    title: 'Fake Maybank TAC SMS',
    description:
      'Scammers sending SMS claiming your TAC has been requested. Links lead to phishing sites that steal banking credentials.',
    scam_type: 'phishing',
    severity: 'high',
    region: 'MY',
    report_count: 1247,
    created_at: '2026-03-01T00:00:00Z',
  },
  {
    id: 'alert-002',
    title: 'LHDN Tax Refund Scam',
    description:
      'Fake messages from "LHDN" offering tax refunds. Asks victims to install a malicious APK to "process" the refund.',
    scam_type: 'phishing',
    severity: 'high',
    region: 'MY',
    report_count: 892,
    created_at: '2026-03-05T00:00:00Z',
  },
  {
    id: 'alert-003',
    title: 'Shopee Lucky Draw Scam',
    description:
      'WhatsApp messages claiming you won a Shopee lucky draw. Link leads to a fake site that harvests personal data.',
    scam_type: 'phishing',
    severity: 'medium',
    region: 'both',
    report_count: 634,
    created_at: '2026-03-08T00:00:00Z',
  },
  {
    id: 'alert-004',
    title: 'DBS OTP Phishing',
    description:
      'SMS pretending to be from DBS Bank requesting OTP verification. Links to a cloned banking portal.',
    scam_type: 'phishing',
    severity: 'high',
    region: 'SG',
    report_count: 521,
    created_at: '2026-03-10T00:00:00Z',
  },
  {
    id: 'alert-005',
    title: 'Parcel Delivery Fee Scam',
    description:
      'Messages claiming a parcel is held and requires a small fee. Payment page steals credit card details.',
    scam_type: 'phishing',
    severity: 'medium',
    region: 'both',
    report_count: 403,
    created_at: '2026-03-12T00:00:00Z',
  },
];
