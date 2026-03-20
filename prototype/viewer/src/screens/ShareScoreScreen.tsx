import React from 'react';
import { phoneDark } from '../theme';

const ShareScoreScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 8 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Share Score</div>
    </div>

    {/* Format toggle */}
    <div style={{ display: 'flex', gap: 4, justifyContent: 'center' }}>
      {['Square', 'Vertical'].map((f, i) => (
        <div key={f} style={{
          background: i === 0 ? '#4F46E5' : phoneDark.card,
          color: i === 0 ? '#fff' : phoneDark.textSecondary,
          fontSize: 7, fontWeight: 700,
          padding: '4px 10px', borderRadius: 8,
        }}>{f}</div>
      ))}
    </div>

    {/* Score card preview */}
    <div style={{
      background: 'linear-gradient(135deg, #1E1B4B, #312E81)',
      borderRadius: 10,
      padding: '12px 10px',
      textAlign: 'center',
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 6,
    }}>
      <svg width="44" height="44" viewBox="0 0 44 44">
        <circle cx="22" cy="22" r="18" fill="none" stroke="rgba(255,255,255,0.15)" strokeWidth="4" />
        <circle cx="22" cy="22" r="18" fill="none" stroke="#16A34A" strokeWidth="4"
          strokeDasharray={`${0.85 * 113} ${113}`}
          strokeLinecap="round" transform="rotate(-90 22 22)" />
        <text x="22" y="25" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="800">85</text>
      </svg>
      <div style={{ color: '#A5B4FC', fontSize: 7, fontWeight: 700 }}>SAFE ANOT?</div>
      <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 5.5 }}>My phone is 85% protected</div>
    </div>

    {/* Share buttons */}
    <div style={{ display: 'flex', gap: 4 }}>
      <div style={{
        flex: 1, background: '#25D366', color: '#fff',
        fontSize: 7, fontWeight: 700, padding: '6px 0',
        borderRadius: 6, textAlign: 'center',
      }}>WhatsApp</div>
      <div style={{
        flex: 1, background: phoneDark.card, color: phoneDark.text,
        fontSize: 7, fontWeight: 700, padding: '6px 0',
        borderRadius: 6, textAlign: 'center',
        border: `1px solid ${phoneDark.border}`,
      }}>Save Image</div>
    </div>
  </div>
);

export default ShareScoreScreen;
