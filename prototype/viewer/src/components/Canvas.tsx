import React, { useRef, useCallback, useMemo } from 'react';
import { screens, flows } from '../flows';
import { epicColor } from '../theme';
import PhoneFrame from './PhoneFrame';
import FlowArrow from './FlowArrow';
import type { ScreenDefinition } from '../types';

const SCREEN_W = 220;
const SCREEN_H = 340;
const FRAME_W = 180;

interface CanvasProps {
  zoom: number;
  panX: number;
  panY: number;
  selectedScreen: string | null;
  searchQuery: string;
  onZoomChange: (z: number) => void;
  onPanChange: (x: number, y: number) => void;
  onSelectScreen: (id: string | null) => void;
}

const Canvas: React.FC<CanvasProps> = ({
  zoom, panX, panY, selectedScreen, searchQuery,
  onZoomChange, onPanChange, onSelectScreen,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);
  const lastMouse = useRef({ x: 0, y: 0 });

  const filteredScreens = useMemo(() => {
    if (!searchQuery.trim()) return screens;
    const q = searchQuery.toLowerCase();
    return screens.filter(s =>
      s.name.toLowerCase().includes(q) ||
      s.epic.toLowerCase().includes(q) ||
      (s.group || '').toLowerCase().includes(q)
    );
  }, [searchQuery]);

  const filteredIds = useMemo(() => new Set(filteredScreens.map(s => s.id)), [filteredScreens]);

  const handleWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault();
    const rect = containerRef.current!.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    const newZoom = Math.min(3, Math.max(0.2, zoom * delta));

    // Zoom centered on cursor
    const newPanX = mouseX - (mouseX - panX) * (newZoom / zoom);
    const newPanY = mouseY - (mouseY - panY) * (newZoom / zoom);

    onZoomChange(newZoom);
    onPanChange(newPanX, newPanY);
  }, [zoom, panX, panY, onZoomChange, onPanChange]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (e.target === containerRef.current || (e.target as HTMLElement).closest('[data-canvas-bg]')) {
      isDragging.current = true;
      lastMouse.current = { x: e.clientX, y: e.clientY };
      onSelectScreen(null);
    }
  }, [onSelectScreen]);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!isDragging.current) return;
    const dx = e.clientX - lastMouse.current.x;
    const dy = e.clientY - lastMouse.current.y;
    lastMouse.current = { x: e.clientX, y: e.clientY };
    onPanChange(panX + dx, panY + dy);
  }, [panX, panY, onPanChange]);

  const handleMouseUp = useCallback(() => {
    isDragging.current = false;
  }, []);

  // Group backgrounds
  const groups = useMemo(() => {
    const map = new Map<string, { minX: number; minY: number; maxX: number; maxY: number; epic: string }>();
    for (const s of screens) {
      const g = s.group || s.epic;
      const entry = map.get(g) || { minX: Infinity, minY: Infinity, maxX: -Infinity, maxY: -Infinity, epic: s.epic };
      entry.minX = Math.min(entry.minX, s.position.x);
      entry.minY = Math.min(entry.minY, s.position.y);
      entry.maxX = Math.max(entry.maxX, s.position.x + SCREEN_W);
      entry.maxY = Math.max(entry.maxY, s.position.y + SCREEN_H);
      map.set(g, entry);
    }
    return Array.from(map.entries()).map(([name, b]) => ({
      name,
      epic: b.epic,
      x: b.minX - 30,
      y: b.minY - 30,
      w: b.maxX - b.minX + 60,
      h: b.maxY - b.minY + 60,
    }));
  }, []);

  const screenMap = useMemo(() => {
    const m = new Map<string, ScreenDefinition>();
    for (const s of screens) m.set(s.id, s);
    return m;
  }, []);

  return (
    <div
      ref={containerRef}
      data-canvas-bg
      onWheel={handleWheel}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      style={{
        width: '100%',
        height: '100%',
        overflow: 'hidden',
        cursor: isDragging.current ? 'grabbing' : 'grab',
        position: 'absolute',
        top: 0,
        left: 0,
        background: '#f0f0f0',
      }}
    >
      <div
        data-canvas-bg
        style={{
          transform: `translate(${panX}px, ${panY}px) scale(${zoom})`,
          transformOrigin: '0 0',
          position: 'absolute',
          top: 0,
          left: 0,
        }}
      >
        {/* Group backgrounds */}
        {groups.map(g => (
          <div key={g.name} style={{
            position: 'absolute',
            left: g.x,
            top: g.y,
            width: g.w,
            height: g.h,
            background: epicColor[g.epic] || 'rgba(100,100,100,0.05)',
            borderRadius: 20,
            border: '1px dashed rgba(100,100,100,0.15)',
          }}>
            <div style={{
              position: 'absolute',
              top: 8,
              left: 12,
              fontSize: 11,
              fontWeight: 700,
              color: '#64748B',
              opacity: 0.7,
            }}>{g.name}</div>
          </div>
        ))}

        {/* Flow arrows (SVG overlay) */}
        <svg
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: 3000,
            height: 2000,
            pointerEvents: 'none',
          }}
        >
          {flows.map((f, i) => {
            const from = screenMap.get(f.from);
            const to = screenMap.get(f.to);
            if (!from || !to) return null;

            const fromCX = from.position.x + FRAME_W / 2 + (SCREEN_W - FRAME_W) / 2;
            const fromCY = from.position.y + SCREEN_H / 2;
            const toCX = to.position.x + FRAME_W / 2 + (SCREEN_W - FRAME_W) / 2;
            const toCY = to.position.y + SCREEN_H / 2;

            // Determine exit/entry edges
            const dx = toCX - fromCX;
            const dy = toCY - fromCY;
            let fromX: number, fromY: number, toX: number, toY: number;

            if (Math.abs(dx) > Math.abs(dy)) {
              // Horizontal connection
              fromX = dx > 0 ? from.position.x + SCREEN_W - 10 : from.position.x + 10;
              fromY = fromCY;
              toX = dx > 0 ? to.position.x + 10 : to.position.x + SCREEN_W - 10;
              toY = toCY;
            } else {
              // Vertical connection
              fromX = fromCX;
              fromY = dy > 0 ? from.position.y + SCREEN_H - 10 : from.position.y + 10;
              toX = toCX;
              toY = dy > 0 ? to.position.y + 10 : to.position.y + SCREEN_H - 10;
            }

            return (
              <FlowArrow
                key={i}
                fromX={fromX}
                fromY={fromY}
                toX={toX}
                toY={toY}
                label={f.action}
                type={f.type}
                zoom={zoom}
              />
            );
          })}
        </svg>

        {/* Screen cards */}
        {screens.map(s => {
          const hidden = !filteredIds.has(s.id);

          return (
            <div
              key={s.id}
              style={{
                position: 'absolute',
                left: s.position.x,
                top: s.position.y,
                width: SCREEN_W,
                opacity: hidden ? 0.15 : 1,
                transition: 'opacity 0.2s ease',
              }}
              onClick={(e) => {
                e.stopPropagation();
                onSelectScreen(s.id === selectedScreen ? null : s.id);
              }}
            >
              <PhoneFrame
                screenId={s.id}
                name={s.name}
                epic={s.epic}
                status={s.status}
                selected={s.id === selectedScreen}
              />
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Canvas;
