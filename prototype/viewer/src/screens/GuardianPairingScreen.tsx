import React from 'react';
import { phoneDark } from '../theme';

const GuardianPairingScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Family Guardian</div>
    </div>

    {/* Two role cards */}
    <div style={{ display: 'flex', gap: 4 }}>
      {[
        { role: 'Protector', desc: 'Monitor family', icon: '\uD83D\uDEE1\uFE0F', color: '#0D9488' },
        { role: 'Ward', desc: 'Get protected', icon: '\uD83D\uDC64', color: '#4F46E5' },
      ].map(r => (
        <div key={r.role} style={{
          flex: 1,
          background: phoneDark.card,
          borderRadius: 8,
          padding: '8px 6px',
          textAlign: 'center',
          border: `1px solid ${phoneDark.border}`,
        }}>
          <div style={{ fontSize: 16 }}>{r.icon}</div>
          <div style={{ color: r.color, fontSize: 8, fontWeight: 700, marginTop: 2 }}>{r.role}</div>
          <div style={{ color: phoneDark.textSecondary, fontSize: 5.5, marginTop: 1 }}>{r.desc}</div>
        </div>
      ))}
    </div>

    {/* Code display */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '8px',
      textAlign: 'center',
      border: `1px dashed ${phoneDark.border}`,
    }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6, marginBottom: 4 }}>Your pairing code</div>
      <div style={{
        color: '#4F46E5', fontSize: 18, fontWeight: 800,
        letterSpacing: 4, fontFamily: 'monospace',
      }}>
        A7X-9K2
      </div>
    </div>

    {/* Code input */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '6px 8px',
      border: `1px solid ${phoneDark.border}`,
    }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6, marginBottom: 3 }}>Or enter a code</div>
      <div style={{
        background: phoneDark.bg,
        borderRadius: 6,
        padding: '6px 8px',
        fontSize: 7,
        color: phoneDark.textSecondary,
        border: `1px solid ${phoneDark.border}`,
      }}>
        Enter code...
      </div>
    </div>

    {/* Pair button */}
    <div style={{
      background: '#0D9488',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '7px 0',
      borderRadius: 6,
      textAlign: 'center',
      marginTop: 'auto',
    }}>
      Pair Device
    </div>
  </div>
);

export default GuardianPairingScreen;
