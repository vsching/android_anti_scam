import React from 'react';
import { statusColor } from '../theme';
import type { ScreenStatus } from '../types';

interface PhoneFrameProps {
  screenId: string;
  name: string;
  epic: string;
  status: ScreenStatus;
  selected?: boolean;
}

const epicLabels: Record<string, string> = {
  E01: 'Onboarding',
  E02: 'Shield',
  E03: 'Check',
  E04: 'Alerts',
  E05: 'Profile',
  E06: 'Dangerous',
  E07: 'Warning',
  E08: 'Guardian',
  E09: 'Scam of Week',
};

// Map screen IDs to HTML filenames
const screenFileMap: Record<string, string> = {
  'onboarding': 'shield.html', // No dedicated onboarding standalone; use shield
  'shield': 'shield.html',
  'check': 'check.html',
  'alerts': 'alerts.html',
  'profile': 'profile.html',
  'fix-detail': 'fix-detail.html',
  'check-dangerous': 'check-dangerous.html',
  'guardian-pairing': 'guardian-pairing.html',
  'guardian-dashboard': 'guardian-dashboard.html',
  'ward-detail': 'guardian-detail.html',
  'guardian-detail': 'guardian-detail.html',
  'alert-detail': 'alerts.html',
  'warning-picker': 'check.html',
  'share-score': 'profile.html',
  'scam-of-the-week': 'alerts.html',
};

// The iframe content is 375px wide (mobile viewport).
// The phone bezel content area is ~168px wide (180 - 12px padding).
// Scale factor: 168 / 375 ≈ 0.448
const IFRAME_WIDTH = 375;
const IFRAME_HEIGHT = 812;
const CONTENT_WIDTH = 168;
const SCALE = CONTENT_WIDTH / IFRAME_WIDTH;
const CONTENT_HEIGHT = Math.round(IFRAME_HEIGHT * SCALE);

const PhoneFrame: React.FC<PhoneFrameProps> = ({ screenId, name, epic, status, selected }) => {
  const filename = screenFileMap[screenId] || 'shield.html';
  const iframeSrc = `/screens/${filename}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
      {/* Phone bezel */}
      <div style={{
        width: 180,
        background: '#1a1a2e',
        borderRadius: 22,
        padding: '8px 6px 10px',
        boxShadow: selected
          ? '0 0 0 3px #4F46E5, 0 12px 32px rgba(0,0,0,0.25)'
          : '0 8px 24px rgba(0,0,0,0.2)',
        cursor: 'pointer',
        transition: 'box-shadow 0.15s ease',
      }}>
        {/* Status bar */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          gap: 3,
          marginBottom: 4,
          padding: '2px 0',
        }}>
          <div style={{ width: 32, height: 3, borderRadius: 2, background: '#2d2d4e' }} />
        </div>

        {/* Screen content via iframe */}
        <div style={{
          borderRadius: 14,
          overflow: 'hidden',
          width: CONTENT_WIDTH,
          height: CONTENT_HEIGHT,
          position: 'relative',
        }}>
          <iframe
            src={iframeSrc}
            title={name}
            style={{
              width: IFRAME_WIDTH,
              height: IFRAME_HEIGHT,
              border: 'none',
              transform: `scale(${SCALE})`,
              transformOrigin: '0 0',
              pointerEvents: 'none',
            }}
            scrolling="no"
            loading="lazy"
          />
        </div>
      </div>

      {/* Label area */}
      <div style={{ textAlign: 'center', maxWidth: 180 }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 6,
          marginBottom: 2,
        }}>
          {/* Status dot */}
          <div style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: statusColor[status],
            flexShrink: 0,
          }} />
          {/* Screen name */}
          <span style={{
            fontSize: 11,
            fontWeight: 700,
            color: '#1E293B',
            whiteSpace: 'nowrap',
          }}>{name}</span>
        </div>
        {/* Epic badge */}
        <div style={{
          display: 'inline-block',
          background: '#EEF2FF',
          color: '#4F46E5',
          fontSize: 9,
          fontWeight: 700,
          padding: '2px 8px',
          borderRadius: 10,
        }}>
          {epic}: {epicLabels[epic] || epic}
        </div>
      </div>
    </div>
  );
};

export default PhoneFrame;
