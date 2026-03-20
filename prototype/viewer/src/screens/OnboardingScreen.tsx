import React from 'react';
import { phoneDark } from '../theme';

const OnboardingScreen: React.FC = () => (
  <div style={{ background: phoneDark.bg, borderRadius: 12, padding: 14, height: 280, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
    {/* Shield icon */}
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
      <path d="M24 4L6 12v12c0 11.1 7.7 21.5 18 24 10.3-2.5 18-12.9 18-24V12L24 4z" fill="#4F46E5" opacity="0.9"/>
      <path d="M20 24l4 4 8-8" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>

    <div style={{ color: phoneDark.text, fontSize: 13, fontWeight: 800, textAlign: 'center', lineHeight: 1.3 }}>
      Safe Anot?
    </div>
    <div style={{ color: phoneDark.textSecondary, fontSize: 9, textAlign: 'center', lineHeight: 1.4 }}>
      Protect your family from scams in Malaysia & Singapore
    </div>

    {/* Slide dots */}
    <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
      <div style={{ width: 16, height: 4, borderRadius: 2, background: '#4F46E5' }} />
      <div style={{ width: 4, height: 4, borderRadius: 2, background: phoneDark.border }} />
      <div style={{ width: 4, height: 4, borderRadius: 2, background: phoneDark.border }} />
    </div>

    {/* Get Started button */}
    <div style={{
      background: '#4F46E5',
      color: '#fff',
      fontSize: 10,
      fontWeight: 700,
      padding: '8px 28px',
      borderRadius: 10,
      marginTop: 8,
      textAlign: 'center',
    }}>
      Get Started
    </div>
  </div>
);

export default OnboardingScreen;
