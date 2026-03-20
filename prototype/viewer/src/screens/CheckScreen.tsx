import React from 'react';
import { phoneDark } from '../theme';

const CheckScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 8 }}>
    {/* Header */}
    <div style={{ color: phoneDark.text, fontSize: 11, fontWeight: 800, textAlign: 'center', padding: '4px 0' }}>
      Check Link
    </div>

    {/* URL input */}
    <div style={{
      background: phoneDark.card,
      border: `1px solid ${phoneDark.border}`,
      borderRadius: 8,
      padding: '8px 8px',
      fontSize: 8,
      color: phoneDark.textSecondary,
    }}>
      Paste URL here...
    </div>

    {/* CHECK button */}
    <div style={{
      background: '#4F46E5',
      color: '#fff',
      fontSize: 10,
      fontWeight: 800,
      padding: '8px 0',
      borderRadius: 8,
      textAlign: 'center',
      letterSpacing: 1,
    }}>
      CHECK
    </div>

    {/* States preview — show all result types as small cards */}
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 4 }}>
      {[
        { label: 'SAFE', color: '#16A34A', icon: '\u2713' },
        { label: 'DANGEROUS', color: '#DC2626', icon: '\u2717' },
        { label: 'SUSPICIOUS', color: '#F59E0B', icon: '!' },
      ].map(v => (
        <div key={v.label} style={{
          background: `${v.color}18`,
          borderRadius: 6,
          padding: '4px 8px',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}>
          <div style={{
            width: 14, height: 14, borderRadius: '50%',
            background: v.color, color: '#fff',
            fontSize: 8, fontWeight: 800,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>{v.icon}</div>
          <span style={{ color: v.color, fontSize: 7, fontWeight: 700 }}>{v.label}</span>
        </div>
      ))}
    </div>

    {/* Loading spinner placeholder */}
    <div style={{
      display: 'flex', alignItems: 'center', gap: 6,
      padding: '4px 8px',
      background: phoneDark.card,
      borderRadius: 6,
    }}>
      <svg width="12" height="12" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="10" fill="none" stroke={phoneDark.border} strokeWidth="3" />
        <path d="M12 2a10 10 0 0 1 10 10" fill="none" stroke="#4F46E5" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span style={{ color: phoneDark.textSecondary, fontSize: 7 }}>Checking...</span>
    </div>

    {/* Tab bar */}
    <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-around', borderTop: `1px solid ${phoneDark.border}`, paddingTop: 6 }}>
      {['Shield', 'Check', 'Alerts', 'Profile'].map((tab, i) => (
        <div key={tab} style={{ fontSize: 6, fontWeight: i === 1 ? 800 : 500, color: i === 1 ? '#4F46E5' : phoneDark.textSecondary, textAlign: 'center' }}>
          <div style={{ width: 10, height: 10, borderRadius: 3, background: i === 1 ? '#4F46E5' : phoneDark.border, margin: '0 auto 2px', opacity: i === 1 ? 1 : 0.4 }} />
          {tab}
        </div>
      ))}
    </div>
  </div>
);

export default CheckScreen;
