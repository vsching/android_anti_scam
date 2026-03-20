import React from 'react';
import { phoneDark } from '../theme';

const AlertDetailScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 10, height: 280, display: 'flex', flexDirection: 'column', gap: 6 }}>
    {/* Back + header */}
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <div style={{ color: phoneDark.textSecondary, fontSize: 10 }}>&larr;</div>
        <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 700 }}>Alert</div>
      </div>
      <div style={{
        background: '#4F46E5',
        color: '#fff',
        fontSize: 6,
        fontWeight: 700,
        padding: '3px 8px',
        borderRadius: 6,
      }}>Share</div>
    </div>

    {/* Severity badge */}
    <div style={{
      background: '#DC262618',
      borderRadius: 6,
      padding: '3px 8px',
      display: 'inline-flex',
      alignSelf: 'flex-start',
    }}>
      <span style={{ color: '#DC2626', fontSize: 7, fontWeight: 700 }}>HIGH SEVERITY</span>
    </div>

    {/* Title */}
    <div style={{ color: phoneDark.text, fontSize: 10, fontWeight: 800, lineHeight: 1.3 }}>
      Fake Maybank Login Pages on WhatsApp
    </div>

    {/* Meta */}
    <div style={{ display: 'flex', gap: 8, fontSize: 6, color: phoneDark.textSecondary, fontWeight: 600 }}>
      <span>237 reports</span>
      <span>Selangor</span>
      <span>2 days ago</span>
    </div>

    {/* Content */}
    <div style={{
      background: phoneDark.card,
      borderRadius: 8,
      padding: '8px',
      flex: 1,
      overflow: 'hidden',
    }}>
      <div style={{ color: phoneDark.text, fontSize: 7, lineHeight: 1.6 }}>
        Scammers are sending messages pretending to be Maybank, asking users to "verify" their account via a fake link. The page looks identical to the real Maybank2u login.
      </div>
      <div style={{ color: phoneDark.textSecondary, fontSize: 6, marginTop: 6 }}>
        Known domains:
      </div>
      <div style={{
        background: phoneDark.bg,
        borderRadius: 4,
        padding: '3px 6px',
        marginTop: 3,
        fontSize: 5.5,
        color: '#DC2626',
        fontFamily: 'monospace',
      }}>
        maybank-secure-update.xyz<br/>
        maybank2u-verify.com
      </div>
    </div>

    {/* Bottom action */}
    <div style={{
      background: '#DC2626',
      color: '#fff',
      fontSize: 8,
      fontWeight: 700,
      padding: '6px 0',
      borderRadius: 6,
      textAlign: 'center',
    }}>
      Report This Scam
    </div>
  </div>
);

export default AlertDetailScreen;
