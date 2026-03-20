import React from 'react';
import { phoneDark } from '../theme';

const WarningPickerScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Warn Contacts</div>
    </div>

    {/* Language toggle */}
    <div style={{ display: 'flex', gap: 4 }}>
      {['English', 'BM', 'Chinese'].map((l, i) => (
        <div key={l} style={{
          background: i === 0 ? '#4F46E5' : phoneDark.card,
          color: i === 0 ? '#fff' : phoneDark.textSecondary,
          fontSize: 6, fontWeight: 700,
          padding: '3px 6px', borderRadius: 8,
          border: i === 0 ? 'none' : `1px solid ${phoneDark.border}`,
        }}>{l}</div>
      ))}
    </div>

    {/* Template cards */}
    {[
      { tone: 'Polite', emoji: '\uD83D\uDE4F', preview: 'Hi, this link might be unsafe...', color: '#0D9488' },
      { tone: 'Urgent', emoji: '\u26A0\uFE0F', preview: 'WARNING: Do NOT click this!', color: '#DC2626' },
      { tone: 'Elder-friendly', emoji: '\uD83D\uDC74', preview: 'Mak/Ayah, jangan tekan link...', color: '#F59E0B' },
    ].map(t => (
      <div key={t.tone} style={{
        background: phoneDark.card,
        borderRadius: 8,
        padding: '8px 8px',
        borderLeft: `3px solid ${t.color}`,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 3 }}>
          <span style={{ fontSize: 10 }}>{t.emoji}</span>
          <span style={{ color: t.color, fontSize: 8, fontWeight: 700 }}>{t.tone}</span>
        </div>
        <div style={{ color: phoneDark.textSecondary, fontSize: 6.5, fontStyle: 'italic' }}>
          "{t.preview}"
        </div>
      </div>
    ))}

    {/* Send via */}
    <div style={{
      background: '#4F46E5',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
      marginTop: 'auto',
    }}>
      Send via WhatsApp
    </div>
  </div>
);

export default WarningPickerScreen;
