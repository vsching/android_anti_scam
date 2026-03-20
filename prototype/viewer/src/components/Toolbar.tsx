import React from 'react';
import { theme, statusColor } from '../theme';

interface ToolbarProps {
  zoom: number;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitAll: () => void;
}

const Toolbar: React.FC<ToolbarProps> = ({ zoom, searchQuery, onSearchChange, onZoomIn, onZoomOut, onFitAll }) => (
  <div style={{
    background: theme.card,
    borderBottom: '1px solid #E2E8F0',
    padding: '10px 20px',
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    fontFamily: theme.font,
    zIndex: 30,
    boxShadow: theme.shadowSm,
    flexShrink: 0,
  }}>
    {/* Title */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
      <div style={{
        width: 28, height: 28, borderRadius: '8px 8px 10px 10px',
        background: `linear-gradient(135deg, ${theme.indigo}, #6366F1)`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L3 7v7c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-5z" fill="#fff"/>
        </svg>
      </div>
      <span style={{ fontSize: 15, fontWeight: 800, color: theme.text }}>
        Safe Anot? <span style={{ fontWeight: 500, color: theme.textSecondary }}>UI Flow Canvas</span>
      </span>
    </div>

    {/* Search */}
    <div style={{ flex: 1, maxWidth: 260 }}>
      <input
        type="text"
        placeholder="Search screens..."
        value={searchQuery}
        onChange={e => onSearchChange(e.target.value)}
        style={{
          width: '100%',
          padding: '7px 12px',
          borderRadius: 10,
          border: '1px solid #E2E8F0',
          fontSize: 12,
          fontFamily: theme.font,
          fontWeight: 600,
          outline: 'none',
          color: theme.text,
          background: theme.bg,
        }}
      />
    </div>

    {/* Zoom controls */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 }}>
      <button onClick={onZoomOut} style={btnStyle}>-</button>
      <span style={{ fontSize: 12, fontWeight: 700, color: theme.text, minWidth: 42, textAlign: 'center' }}>
        {Math.round(zoom * 100)}%
      </span>
      <button onClick={onZoomIn} style={btnStyle}>+</button>
      <button onClick={onFitAll} style={{ ...btnStyle, fontSize: 10, padding: '4px 10px' }}>Fit All</button>
    </div>

    {/* Legend */}
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexShrink: 0 }}>
      {[
        { label: 'Built', status: 'built' },
        { label: 'Planned', status: 'planned' },
        { label: 'Future', status: 'future' },
      ].map(l => (
        <div key={l.status} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: statusColor[l.status] }} />
          <span style={{ fontSize: 10, fontWeight: 600, color: theme.textSecondary }}>{l.label}</span>
        </div>
      ))}
    </div>
  </div>
);

const btnStyle: React.CSSProperties = {
  width: 28,
  height: 28,
  borderRadius: 8,
  border: '1px solid #E2E8F0',
  background: '#fff',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 700,
  color: '#1E293B',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontFamily: "'Quicksand', sans-serif",
};

export default Toolbar;
