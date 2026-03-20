import React from 'react';
import { phoneDark } from '../theme';

const AlertsScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Header */}
    <div style={{ color: phoneDark.text, fontSize: 11, fontWeight: 800, padding: '2px 0' }}>
      Scam Alerts
    </div>

    {/* Region filter chips */}
    <div style={{ display: 'flex', gap: 4 }}>
      {['All', 'KL', 'Selangor', 'Penang'].map((r, i) => (
        <div key={r} style={{
          background: i === 0 ? '#4F46E5' : phoneDark.card,
          color: i === 0 ? '#fff' : phoneDark.textSecondary,
          fontSize: 6,
          fontWeight: 700,
          padding: '3px 6px',
          borderRadius: 10,
          border: i === 0 ? 'none' : `1px solid ${phoneDark.border}`,
        }}>{r}</div>
      ))}
    </div>

    {/* Pull-to-refresh indicator */}
    <div style={{ textAlign: 'center', color: phoneDark.textSecondary, fontSize: 6 }}>
      Pull to refresh
    </div>

    {/* Alert cards */}
    {[
      { title: 'Fake Maybank Login', reports: 237, region: 'Selangor', severity: 'high' },
      { title: 'LHDN Tax Refund SMS', reports: 189, region: 'Nationwide', severity: 'high' },
      { title: 'Shopee Lucky Draw', reports: 156, region: 'KL', severity: 'medium' },
    ].map(a => (
      <div key={a.title} style={{
        background: phoneDark.card,
        borderRadius: 8,
        padding: '6px 8px',
        borderLeft: `3px solid ${a.severity === 'high' ? '#DC2626' : '#F59E0B'}`,
      }}>
        <div style={{ color: phoneDark.text, fontSize: 7.5, fontWeight: 700, marginBottom: 2 }}>{a.title}</div>
        <div style={{ display: 'flex', gap: 8, fontSize: 6, color: phoneDark.textSecondary }}>
          <span>{a.reports} reports</span>
          <span>{a.region}</span>
        </div>
      </div>
    ))}

    {/* Tab bar */}
    <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-around', borderTop: `1px solid ${phoneDark.border}`, paddingTop: 6 }}>
      {['Shield', 'Check', 'Alerts', 'Profile'].map((tab, i) => (
        <div key={tab} style={{ fontSize: 6, fontWeight: i === 2 ? 800 : 500, color: i === 2 ? '#4F46E5' : phoneDark.textSecondary, textAlign: 'center' }}>
          <div style={{ width: 10, height: 10, borderRadius: 3, background: i === 2 ? '#4F46E5' : phoneDark.border, margin: '0 auto 2px', opacity: i === 2 ? 1 : 0.4 }} />
          {tab}
        </div>
      ))}
    </div>
  </div>
);

export default AlertsScreen;
