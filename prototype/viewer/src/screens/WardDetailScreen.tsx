import React from 'react';
import { phoneDark } from '../theme';

const WardDetailScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
        <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Mak's Phone</div>
      </div>
      <div style={{ color: '#DC2626', fontSize: 6, fontWeight: 700 }}>Unlink</div>
    </div>

    {/* Large score ring */}
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <svg width="56" height="56" viewBox="0 0 56 56">
        <circle cx="28" cy="28" r="22" fill="none" stroke={phoneDark.border} strokeWidth="4" />
        <circle cx="28" cy="28" r="22" fill="none" stroke="#F59E0B" strokeWidth="4"
          strokeDasharray={`${0.72 * 138.2} ${138.2}`}
          strokeLinecap="round" transform="rotate(-90 28 28)" />
        <text x="28" y="30" textAnchor="middle" fill={phoneDark.text} fontSize="14" fontWeight="800">72</text>
      </svg>
    </div>

    {/* Status cards */}
    <div style={{ display: 'flex', gap: 4 }}>
      {[
        { label: 'Risky Apps', value: '2', color: '#DC2626' },
        { label: 'Last Scan', value: '3h ago', color: '#16A34A' },
      ].map(s => (
        <div key={s.label} style={{
          flex: 1,
          background: phoneDark.card,
          borderRadius: 6,
          padding: '5px 6px',
          textAlign: 'center',
        }}>
          <div style={{ color: s.color, fontSize: 11, fontWeight: 800 }}>{s.value}</div>
          <div style={{ color: phoneDark.textSecondary, fontSize: 5.5, fontWeight: 600 }}>{s.label}</div>
        </div>
      ))}
    </div>

    {/* 7-day chart (simplified bar chart) */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '6px 8px',
    }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6, fontWeight: 600, marginBottom: 4 }}>7-Day Score</div>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 28 }}>
        {[65, 68, 72, 70, 75, 72, 72].map((v, i) => (
          <div key={i} style={{
            flex: 1,
            height: `${(v / 100) * 28}px`,
            background: i === 6 ? '#F59E0B' : `#F59E0B60`,
            borderRadius: 2,
          }} />
        ))}
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 2 }}>
        {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
          <span key={i} style={{ fontSize: 4.5, color: phoneDark.textSecondary, flex: 1, textAlign: 'center' }}>{d}</span>
        ))}
      </div>
    </div>

    {/* Notify button */}
    <div style={{
      background: '#F59E0B',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
    }}>
      Send Reminder to Fix
    </div>
  </div>
);

export default WardDetailScreen;
