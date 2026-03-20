import React from 'react';
import type { FlowType } from '../types';

interface FlowArrowProps {
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
  label: string;
  type: FlowType;
  zoom: number;
}

const typeColors: Record<FlowType, string> = {
  forward: '#4F46E5',
  back: '#64748B',
  error: '#DC2626',
  success: '#16A34A',
};

const FlowArrow: React.FC<FlowArrowProps> = ({ fromX, fromY, toX, toY, label, type, zoom }) => {
  const color = typeColors[type];
  const dx = toX - fromX;

  // Control points for a smooth curve
  const cx1 = fromX + dx * 0.4;
  const cy1 = fromY;
  const cx2 = toX - dx * 0.4;
  const cy2 = toY;

  const path = `M ${fromX} ${fromY} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${toX} ${toY}`;

  // Midpoint for label
  const mx = (fromX + toX) / 2;
  const my = (fromY + toY) / 2 - 8;

  // Arrow head angle
  const angle = Math.atan2(toY - cy2, toX - cx2);
  const arrowLen = 8;
  const a1x = toX - arrowLen * Math.cos(angle - 0.4);
  const a1y = toY - arrowLen * Math.sin(angle - 0.4);
  const a2x = toX - arrowLen * Math.cos(angle + 0.4);
  const a2y = toY - arrowLen * Math.sin(angle + 0.4);

  const fontSize = Math.max(9, 11 / zoom);

  return (
    <g>
      <path
        d={path}
        fill="none"
        stroke={color}
        strokeWidth={Math.max(1, 1.5 / zoom)}
        strokeDasharray={type === 'back' ? '4 3' : undefined}
        opacity={0.6}
      />
      {/* Arrow head */}
      <polygon
        points={`${toX},${toY} ${a1x},${a1y} ${a2x},${a2y}`}
        fill={color}
        opacity={0.6}
      />
      {/* Label */}
      {zoom > 0.4 && (
        <text
          x={mx}
          y={my}
          textAnchor="middle"
          fill={color}
          fontSize={fontSize}
          fontWeight={600}
          fontFamily="'Quicksand', sans-serif"
          opacity={0.8}
        >
          {label}
        </text>
      )}
    </g>
  );
};

export default FlowArrow;
