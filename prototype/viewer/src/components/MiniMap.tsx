import React, { useMemo } from 'react';
import { screens } from '../flows';
import { statusColor } from '../theme';

interface MiniMapProps {
  zoom: number;
  panX: number;
  panY: number;
  viewportWidth: number;
  viewportHeight: number;
  onJump: (x: number, y: number) => void;
}

const MINIMAP_W = 200;
const MINIMAP_H = 150;
const SCREEN_W = 220;
const SCREEN_H = 340;

const MiniMap: React.FC<MiniMapProps> = ({ zoom, panX, panY, viewportWidth, viewportHeight, onJump }) => {
  const bounds = useMemo(() => {
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const s of screens) {
      minX = Math.min(minX, s.position.x);
      minY = Math.min(minY, s.position.y);
      maxX = Math.max(maxX, s.position.x + SCREEN_W);
      maxY = Math.max(maxY, s.position.y + SCREEN_H);
    }
    const pad = 100;
    return { minX: minX - pad, minY: minY - pad, maxX: maxX + pad, maxY: maxY + pad };
  }, []);

  const worldW = bounds.maxX - bounds.minX;
  const worldH = bounds.maxY - bounds.minY;
  const scale = Math.min(MINIMAP_W / worldW, MINIMAP_H / worldH);

  // Viewport rectangle in world coords
  const vpX = (-panX / zoom);
  const vpY = (-panY / zoom);
  const vpW = viewportWidth / zoom;
  const vpH = viewportHeight / zoom;

  const handleClick = (e: React.MouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const mx = (e.clientX - rect.left) / scale + bounds.minX;
    const my = (e.clientY - rect.top) / scale + bounds.minY;
    onJump(mx, my);
  };

  return (
    <div style={{
      position: 'absolute',
      bottom: 16,
      right: 16,
      background: 'rgba(255,255,255,0.95)',
      borderRadius: 12,
      padding: 8,
      boxShadow: '0 4px 16px rgba(0,0,0,0.12)',
      border: '1px solid #E2E8F0',
      zIndex: 20,
    }}>
      <svg
        width={MINIMAP_W}
        height={MINIMAP_H}
        style={{ display: 'block', cursor: 'pointer' }}
        onClick={handleClick}
      >
        {/* Screen dots */}
        {screens.map(s => (
          <rect
            key={s.id}
            x={(s.position.x - bounds.minX) * scale}
            y={(s.position.y - bounds.minY) * scale}
            width={SCREEN_W * scale}
            height={SCREEN_H * scale}
            rx={2}
            fill={statusColor[s.status]}
            opacity={0.6}
          />
        ))}

        {/* Viewport rectangle */}
        <rect
          x={(vpX - bounds.minX) * scale}
          y={(vpY - bounds.minY) * scale}
          width={vpW * scale}
          height={vpH * scale}
          fill="none"
          stroke="#4F46E5"
          strokeWidth={1.5}
          rx={2}
          opacity={0.8}
        />
      </svg>
    </div>
  );
};

export default MiniMap;
