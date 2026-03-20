import React from 'react';
import { phoneDark } from '../theme';

const CheckDangerousScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 8 }}>
    {/* Back */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Result</div>
    </div>

    {/* Red verdict card */}
    <div style={{
      background: '#DC262618',
      borderRadius: 10,
      padding: '10px 8px',
      textAlign: 'center',
      border: '1px solid #DC262640',
    }}>
      <div style={{ fontSize: 20 }}>&#x1F6A8;</div>
      <div style={{ color: '#DC2626', fontSize: 12, fontWeight: 800, letterSpacing: 0.5 }}>DANGEROUS</div>
      <div style={{ color: phoneDark.textSecondary, fontSize: 7, marginTop: 2 }}>Do not click this link</div>
      <div style={{
        background: phoneDark.card,
        borderRadius: 6,
        padding: '4px 6px',
        marginTop: 6,
        fontSize: 6,
        color: phoneDark.textSecondary,
        wordBreak: 'break-all',
      }}>
        maybank-secure-update.xyz
      </div>
    </div>

    {/* Action buttons */}
    <div style={{
      background: '#DC2626',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
    }}>
      Warn Contacts
    </div>
    <div style={{
      background: '#0D9488',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
    }}>
      Protect Family
    </div>
    <div style={{
      background: phoneDark.card,
      color: phoneDark.text,
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
      border: `1px solid ${phoneDark.border}`,
    }}>
      Share Result
    </div>
  </div>
);

export default CheckDangerousScreen;
