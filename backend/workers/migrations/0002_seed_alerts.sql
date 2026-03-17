-- E01-005: Seed initial scam alerts for Safe Anot?
-- These are the canonical seed alerts — seed-alerts.ts is derived from this file.

INSERT OR IGNORE INTO alerts (id, title, description, scam_type, severity, region, report_count, created_at)
VALUES
  ('alert-001', 'Fake Maybank TAC SMS',
   'Scammers sending SMS claiming your TAC has been requested. Links lead to phishing sites that steal banking credentials.',
   'phishing', 'high', 'MY', 1247, '2026-03-01T00:00:00Z'),

  ('alert-002', 'LHDN Tax Refund Scam',
   'Fake messages from "LHDN" offering tax refunds. Asks victims to install a malicious APK to "process" the refund.',
   'phishing', 'high', 'MY', 892, '2026-03-05T00:00:00Z'),

  ('alert-003', 'Shopee Lucky Draw Scam',
   'WhatsApp messages claiming you won a Shopee lucky draw. Link leads to a fake site that harvests personal data.',
   'phishing', 'medium', 'both', 634, '2026-03-08T00:00:00Z'),

  ('alert-004', 'DBS OTP Phishing',
   'SMS pretending to be from DBS Bank requesting OTP verification. Links to a cloned banking portal.',
   'phishing', 'high', 'SG', 521, '2026-03-10T00:00:00Z'),

  ('alert-005', 'Parcel Delivery Fee Scam',
   'Messages claiming a parcel is held and requires a small fee. Payment page steals credit card details.',
   'phishing', 'medium', 'both', 403, '2026-03-12T00:00:00Z');
