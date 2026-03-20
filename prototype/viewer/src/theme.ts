/** V5-B Indigo theme tokens — extracted from website/index.html */
export const theme = {
  font: "'Quicksand', sans-serif",

  /* Brand colours */
  indigo: '#4F46E5',
  indigoLight: '#EEF2FF',
  teal: '#0D9488',
  tealLight: '#F0FDFA',
  red: '#DC2626',
  green: '#16A34A',
  amber: '#F59E0B',
  amberLight: '#FFFBEB',

  /* Canvas (light) surfaces */
  bg: '#FAFAFA',
  card: '#FFFFFF',
  text: '#1E293B',
  textSecondary: '#64748B',

  /* Geometry */
  radius: '16px',
  shadowSm: '0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)',
  shadowMd: '0 4px 12px rgba(0,0,0,0.08)',
  shadowLg: '0 12px 32px rgba(0,0,0,0.12)',

  /* Hero gradient (for decorative use) */
  heroGradient: 'linear-gradient(160deg, #1E1B4B 0%, #312E81 50%, #3730A3 100%)',
} as const;

/** Dark theme for phone mockup interiors (the app itself is dark-themed) */
export const phoneDark = {
  bg: '#0D1117',
  card: '#161B22',
  text: '#F0F6FC',
  textSecondary: '#8B949E',
  border: '#30363D',
  accent: '#4F46E5',
  accentGlow: 'rgba(79, 70, 229, 0.25)',
} as const;

/** Status colours for the canvas badges */
export const statusColor: Record<string, string> = {
  built: '#16A34A',
  planned: '#F59E0B',
  future: '#64748B',
};

/** Epic group colours (translucent backgrounds on canvas) */
export const epicColor: Record<string, string> = {
  'E01': 'rgba(79,70,229,0.08)',   // Onboarding — indigo
  'E02': 'rgba(13,148,136,0.08)',   // Shield — teal
  'E03': 'rgba(220,38,38,0.08)',    // Check — red
  'E04': 'rgba(245,158,11,0.08)',   // Alerts — amber
  'E05': 'rgba(79,70,229,0.08)',    // Profile — indigo
  'E06': 'rgba(220,38,38,0.08)',    // Dangerous — red
  'E07': 'rgba(245,158,11,0.08)',   // Warning — amber
  'E08': 'rgba(13,148,136,0.08)',   // Guardian — teal
  'E09': 'rgba(220,38,38,0.08)',    // Scam of the Week — red
};
