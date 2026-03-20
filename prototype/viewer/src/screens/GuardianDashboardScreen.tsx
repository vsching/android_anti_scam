import React from 'react';
import { phoneDark } from '../theme';

const GuardianDashboardScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>My Family</div>
    </div>

    {/* Ward cards */}
    {[
      { name: "Mak's Phone", score: 72, status: 'OK', color: '#F59E0B' },
      { name: "Ayah's Phone", score: 45, status: 'Went Dark', color: '#DC2626' },
      { name: "Adik's Phone", score: 91, status: 'OK', color: '#16A34A' },
    ].map(w => (
      <div key={w.name} style={{
        background: phoneDark.card,
        borderRadius: 8,
        padding: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        border: `1px solid ${phoneDark.border}`,
      }}>
        {/* Mini score ring */}
        <svg width="32" height="32" viewBox="0 0 32 32">
          <circle cx="16" cy="16" r="12" fill="none" stroke={phoneDark.border} strokeWidth="3" />
          <circle cx="16" cy="16" r="12" fill="none" stroke={w.color} strokeWidth="3"
            strokeDasharray={`${(w.score / 100) * 75.4} ${75.4}`}
            strokeLinecap="round" transform="rotate(-90 16 16)" />
          <text x="16" y="18" textAnchor="middle" fill={phoneDark.text} fontSize="8" fontWeight="800">{w.score}</text>
        </svg>
        <div style={{ flex: 1 }}>
          <div style={{ color: phoneDark.text, fontSize: 8, fontWeight: 700 }}>{w.name}</div>
          <div style={{ fontSize: 6 }}>
            {w.status === 'Went Dark' ? (
              <span style={{
                background: '#DC262630',
                color: '#DC2626',
                padding: '1px 4px',
                borderRadius: 4,
                fontWeight: 700,
                fontSize: 5.5,
              }}>WENT DARK</span>
            ) : (
              <span style={{ color: '#16A34A', fontWeight: 600 }}>Online</span>
            )}
          </div>
        </div>
        <div style={{ color: phoneDark.textSecondary, fontSize: 8 }}>&rsaquo;</div>
      </div>
    ))}

    {/* Add ward */}
    <div style={{
      border: `1px dashed ${phoneDark.border}`,
      borderRadius: 8,
      padding: '6px',
      textAlign: 'center',
      color: phoneDark.textSecondary,
      fontSize: 7,
      fontWeight: 600,
    }}>
      + Add Family Member
    </div>
  </div>
);

export default GuardianDashboardScreen;
