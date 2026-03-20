import React from 'react';
import { statusColor } from '../theme';
import type { ScreenStatus } from '../types';

interface PhoneFrameProps {
  children: React.ReactNode;
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

const PhoneFrame: React.FC<PhoneFrameProps> = ({ children, name, epic, status, selected }) => (
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

      {/* Screen content */}
      <div style={{ borderRadius: 14, overflow: 'hidden' }}>
        {children}
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

export default PhoneFrame;
