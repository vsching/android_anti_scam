import React from 'react';
import { phoneDark } from '../theme';

const ShieldScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 8 }}>
    {/* Score ring */}
    <div style={{ display: 'flex', justifyContent: 'center', padding: '6px 0' }}>
      <svg width="72" height="72" viewBox="0 0 72 72">
        <circle cx="36" cy="36" r="30" fill="none" stroke={phoneDark.border} strokeWidth="5" />
        <circle cx="36" cy="36" r="30" fill="none" stroke="#16A34A" strokeWidth="5"
          strokeDasharray={`${0.85 * 188.5} ${188.5}`}
          strokeLinecap="round" transform="rotate(-90 36 36)" />
        <text x="36" y="34" textAnchor="middle" fill={phoneDark.text} fontSize="16" fontWeight="800">85</text>
        <text x="36" y="44" textAnchor="middle" fill={phoneDark.textSecondary} fontSize="7" fontWeight="600">SAFE</text>
      </svg>
    </div>

    {/* App list items */}
    {['Chrome', 'WhatsApp', 'Telegram'].map((app, i) => (
      <div key={app} style={{
        background: phoneDark.card,
        borderRadius: 8,
        padding: '6px 8px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{
            width: 16, height: 16, borderRadius: 4,
            background: i === 0 ? '#DC2626' : '#16A34A',
            opacity: 0.8,
          }} />
          <span style={{ color: phoneDark.text, fontSize: 8, fontWeight: 600 }}>{app}</span>
        </div>
        <span style={{ color: i === 0 ? '#DC2626' : '#16A34A', fontSize: 7, fontWeight: 700 }}>
          {i === 0 ? 'RISKY' : 'OK'}
        </span>
      </div>
    ))}

    {/* Bottom tab bar */}
    <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-around', borderTop: `1px solid ${phoneDark.border}`, paddingTop: 6 }}>
      {['Shield', 'Check', 'Alerts', 'Profile'].map((tab, i) => (
        <div key={tab} style={{ fontSize: 6, fontWeight: i === 0 ? 800 : 500, color: i === 0 ? '#4F46E5' : phoneDark.textSecondary, textAlign: 'center' }}>
          <div style={{ width: 10, height: 10, borderRadius: 3, background: i === 0 ? '#4F46E5' : phoneDark.border, margin: '0 auto 2px', opacity: i === 0 ? 1 : 0.4 }} />
          {tab}
        </div>
      ))}
    </div>
  </div>
);

export default ShieldScreen;
