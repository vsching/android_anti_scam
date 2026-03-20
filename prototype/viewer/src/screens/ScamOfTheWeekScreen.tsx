import React from 'react';
import { phoneDark } from '../theme';

const ScamOfTheWeekScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Featured</div>
    </div>

    {/* Scam of the Week badge */}
    <div style={{
      background: 'linear-gradient(135deg, #DC2626, #F87171)',
      borderRadius: 8,
      padding: '4px 10px',
      alignSelf: 'flex-start',
    }}>
      <span style={{ color: '#fff', fontSize: 7, fontWeight: 800, letterSpacing: 0.5 }}>SCAM OF THE WEEK</span>
    </div>

    {/* Featured alert card */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 10,
      padding: '10px',
      border: `1px solid #DC262640`,
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      gap: 6,
    }}>
      <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 800, lineHeight: 1.3 }}>
        Fake TNG eWallet Cashback SMS
      </div>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6.5, lineHeight: 1.5 }}>
        "You received RM150 cashback! Claim now before it expires." Links to a phishing page that steals your TNG PIN.
      </div>

      {/* Impact stats */}
      <div style={{ display: 'flex', gap: 4 }}>
        {[
          { label: 'Reports', value: '1,247', color: '#DC2626' },
          { label: 'Money Lost', value: 'RM 89K', color: '#F59E0B' },
        ].map(s => (
          <div key={s.label} style={{
            flex: 1,
            background: phoneDark.bg,
            borderRadius: 6,
            padding: '4px 6px',
            textAlign: 'center',
          }}>
            <div style={{ color: s.color, fontSize: 10, fontWeight: 800 }}>{s.value}</div>
            <div style={{ color: phoneDark.textSecondary, fontSize: 5, fontWeight: 600 }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Fake SMS preview */}
      <div style={{
        background: phoneDark.bg,
        borderRadius: 6,
        padding: '5px 7px',
        borderLeft: `3px solid #F59E0B`,
      }}>
        <div style={{ color: phoneDark.textSecondary, fontSize: 5, fontWeight: 600, marginBottom: 2 }}>Sample SMS:</div>
        <div style={{ color: phoneDark.text, fontSize: 6, fontStyle: 'italic' }}>
          "TNG: You have RM150 cashback! Tap here: tng-reward.xyz/claim"
        </div>
      </div>
    </div>

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
      Share to Warn Others
    </div>
  </div>
);

export default ScamOfTheWeekScreen;
