import React from 'react';
import { phoneDark } from '../theme';

const ProfileScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Header */}
    <div style={{ color: phoneDark.text, fontSize: 11, fontWeight: 800, padding: '2px 0' }}>
      Profile
    </div>

    {/* Stats grid */}
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
      {[
        { label: 'Links Checked', value: '47', color: '#4F46E5' },
        { label: 'Threats Blocked', value: '3', color: '#DC2626' },
        { label: 'Shield Score', value: '85', color: '#16A34A' },
        { label: 'Days Active', value: '23', color: '#0D9488' },
      ].map(s => (
        <div key={s.label} style={{
          background: phoneDark.card,
          borderRadius: 6,
          padding: '5px 6px',
          textAlign: 'center',
        }}>
          <div style={{ color: s.color, fontSize: 12, fontWeight: 800 }}>{s.value}</div>
          <div style={{ color: phoneDark.textSecondary, fontSize: 5.5, fontWeight: 600 }}>{s.label}</div>
        </div>
      ))}
    </div>

    {/* Guardian card */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '6px 8px',
      border: `1px solid ${phoneDark.border}`,
    }}>
      <div style={{ color: phoneDark.text, fontSize: 8, fontWeight: 700, marginBottom: 2 }}>Family Guardian</div>
      <div style={{ color: '#0D9488', fontSize: 7, fontWeight: 600 }}>Tap to protect your parents</div>
    </div>

    {/* Settings toggles */}
    {['Notifications', 'Auto-scan links'].map(s => (
      <div key={s} style={{
        background: phoneDark.card,
        borderRadius: 6,
        padding: '5px 8px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <span style={{ color: phoneDark.text, fontSize: 7, fontWeight: 600 }}>{s}</span>
        <div style={{ width: 20, height: 11, borderRadius: 6, background: '#16A34A', position: 'relative' }}>
          <div style={{ width: 9, height: 9, borderRadius: '50%', background: '#fff', position: 'absolute', right: 1, top: 1 }} />
        </div>
      </div>
    ))}

    {/* Share button */}
    <div style={{
      background: '#4F46E5',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
    }}>
      Share Score Card
    </div>

    {/* Tab bar */}
    <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-around', borderTop: `1px solid ${phoneDark.border}`, paddingTop: 6 }}>
      {['Shield', 'Check', 'Alerts', 'Profile'].map((tab, i) => (
        <div key={tab} style={{ fontSize: 6, fontWeight: i === 3 ? 800 : 500, color: i === 3 ? '#4F46E5' : phoneDark.textSecondary, textAlign: 'center' }}>
          <div style={{ width: 10, height: 10, borderRadius: 3, background: i === 3 ? '#4F46E5' : phoneDark.border, margin: '0 auto 2px', opacity: i === 3 ? 1 : 0.4 }} />
          {tab}
        </div>
      ))}
    </div>
  </div>
);

export default ProfileScreen;
