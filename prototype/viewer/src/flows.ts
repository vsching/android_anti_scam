import { ScreenDefinition, Flow } from './types';

export const screens: ScreenDefinition[] = [
  // === E01: Onboarding ===
  {
    id: 'onboarding',
    name: 'Onboarding',
    epic: 'E01',
    status: 'built',
    position: { x: 100, y: 400 },
    states: ['slide-1', 'slide-2', 'slide-3'],
    group: 'Onboarding',
  },

  // === E02: Phone Shield ===
  {
    id: 'shield',
    name: 'Shield',
    epic: 'E02',
    status: 'built',
    position: { x: 500, y: 200 },
    states: ['default', 'scanning'],
    group: 'Main Tabs',
  },
  {
    id: 'fix-detail',
    name: 'Fix Detail',
    epic: 'E02',
    status: 'built',
    position: { x: 500, y: 600 },
    states: ['default'],
    group: 'Main Tabs',
  },

  // === E03: Link Checker ===
  {
    id: 'check',
    name: 'Check',
    epic: 'E03',
    status: 'built',
    position: { x: 800, y: 200 },
    states: ['empty', 'loading', 'safe', 'dangerous', 'suspicious', 'error'],
    group: 'Main Tabs',
  },
  {
    id: 'check-dangerous',
    name: 'Dangerous Result',
    epic: 'E03',
    status: 'built',
    position: { x: 1100, y: 200 },
    states: ['default'],
    group: 'Check Flow',
  },

  // === E04: Scam Alerts ===
  {
    id: 'alerts',
    name: 'Alerts Feed',
    epic: 'E04',
    status: 'built',
    position: { x: 1100, y: 600 },
    states: ['default', 'loading', 'empty'],
    group: 'Main Tabs',
  },
  {
    id: 'alert-detail',
    name: 'Alert Detail',
    epic: 'E04',
    status: 'built',
    position: { x: 1400, y: 600 },
    states: ['default'],
    group: 'Alerts',
  },

  // === E05: Profile ===
  {
    id: 'profile',
    name: 'Profile',
    epic: 'E05',
    status: 'built',
    position: { x: 1400, y: 200 },
    states: ['default'],
    group: 'Main Tabs',
  },
  {
    id: 'share-score',
    name: 'Share Score',
    epic: 'E05',
    status: 'planned',
    position: { x: 1700, y: 100 },
    states: ['square', 'vertical'],
    group: 'Profile',
  },

  // === E06: Dangerous details ===
  {
    id: 'warning-picker',
    name: 'Warning Picker',
    epic: 'E06',
    status: 'planned',
    position: { x: 1100, y: -200 },
    states: ['default'],
    group: 'Check Flow',
  },

  // === E08: Guardian ===
  {
    id: 'guardian-pairing',
    name: 'Guardian Pairing',
    epic: 'E08',
    status: 'planned',
    position: { x: 1700, y: 400 },
    states: ['role-select', 'code-display', 'code-input'],
    group: 'Guardian',
  },
  {
    id: 'guardian-dashboard',
    name: 'Guardian Dashboard',
    epic: 'E08',
    status: 'planned',
    position: { x: 2000, y: 400 },
    states: ['default', 'empty'],
    group: 'Guardian',
  },
  {
    id: 'ward-detail',
    name: 'Ward Detail',
    epic: 'E08',
    status: 'future',
    position: { x: 2300, y: 400 },
    states: ['default'],
    group: 'Guardian',
  },

  // === E09: Scam of the Week ===
  {
    id: 'scam-of-the-week',
    name: 'Scam of the Week',
    epic: 'E09',
    status: 'future',
    position: { x: 1400, y: 1000 },
    states: ['default'],
    group: 'Alerts',
  },
];

export const flows: Flow[] = [
  // Onboarding → main app
  { from: 'onboarding', to: 'shield', action: 'Get Started', type: 'forward' },

  // Tab navigation (Shield is home)
  { from: 'shield', to: 'check', action: 'Tab: Check', type: 'forward' },
  { from: 'shield', to: 'alerts', action: 'Tab: Alerts', type: 'forward' },
  { from: 'shield', to: 'profile', action: 'Tab: Profile', type: 'forward' },

  // Shield → Fix Detail
  { from: 'shield', to: 'fix-detail', action: 'Tap risky app', type: 'forward' },
  { from: 'fix-detail', to: 'shield', action: 'Back', type: 'back' },

  // Check flow
  { from: 'check', to: 'check-dangerous', action: 'Verdict: Dangerous', type: 'error' },

  // Dangerous → sub-flows
  { from: 'check-dangerous', to: 'warning-picker', action: 'Warn Contacts', type: 'forward' },
  { from: 'check-dangerous', to: 'share-score', action: 'Share', type: 'forward' },

  // Profile flows
  { from: 'profile', to: 'share-score', action: 'Share Score', type: 'forward' },
  { from: 'profile', to: 'guardian-pairing', action: 'Protect Family', type: 'forward' },

  // Guardian flow
  { from: 'guardian-pairing', to: 'guardian-dashboard', action: 'Paired', type: 'success' },
  { from: 'guardian-dashboard', to: 'ward-detail', action: 'Tap ward', type: 'forward' },
  { from: 'ward-detail', to: 'guardian-dashboard', action: 'Back', type: 'back' },

  // Alerts flow
  { from: 'alerts', to: 'alert-detail', action: 'Tap alert', type: 'forward' },
  { from: 'alert-detail', to: 'alerts', action: 'Back', type: 'back' },
  { from: 'alerts', to: 'scam-of-the-week', action: 'Tap featured', type: 'forward' },
];
