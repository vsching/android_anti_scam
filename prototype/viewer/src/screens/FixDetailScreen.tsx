import React from 'react';
import { phoneDark } from '../theme';

const FixDetailScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 8 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
      <div style={{ color: phoneDark.text, fontSize: 11, fontWeight: 800 }}>Fix: Chrome</div>
    </div>

    {/* App icon placeholder */}
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: 'linear-gradient(135deg, #DC2626, #F87171)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: '#fff', fontSize: 14, fontWeight: 800,
      }}>C</div>
    </div>

    {/* Permission toggle */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '8px 8px',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      border: `1px solid #DC262640`,
    }}>
      <div>
        <div style={{ color: phoneDark.text, fontSize: 8, fontWeight: 700 }}>Install unknown apps</div>
        <div style={{ color: '#DC2626', fontSize: 6, fontWeight: 600 }}>Currently ALLOWED</div>
      </div>
      <div style={{ width: 20, height: 11, borderRadius: 6, background: '#DC2626', position: 'relative' }}>
        <div style={{ width: 9, height: 9, borderRadius: '50%', background: '#fff', position: 'absolute', right: 1, top: 1 }} />
      </div>
    </div>

    {/* Why section */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '8px 8px',
    }}>
      <div style={{ color: '#F59E0B', fontSize: 7, fontWeight: 700, marginBottom: 3 }}>Why is this risky?</div>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6.5, lineHeight: 1.5 }}>
        When enabled, scam apps can be installed without going through the Play Store. Scammers exploit this to install banking trojans.
      </div>
    </div>

    {/* Step-by-step */}
    <div style={{
      background: '#16A34A18',
      borderRadius: 8,
      padding: '6px 8px',
    }}>
      <div style={{ color: '#16A34A', fontSize: 7, fontWeight: 700, marginBottom: 2 }}>How to fix</div>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6.5 }}>
        Settings &rarr; Apps &rarr; Chrome &rarr; Install unknown apps &rarr; Toggle OFF
      </div>
    </div>
  </div>
);

export default FixDetailScreen;
