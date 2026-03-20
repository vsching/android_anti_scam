import React, { useState, useCallback, useRef, useEffect } from 'react';
import Toolbar from './components/Toolbar';
import Canvas from './components/Canvas';
import MiniMap from './components/MiniMap';
import { screens } from './flows';

const SCREEN_W = 220;
const SCREEN_H = 340;

const App: React.FC = () => {
  const [zoom, setZoom] = useState(0.7);
  const [panX, setPanX] = useState(200);
  const [panY, setPanY] = useState(200);
  const [selectedScreen, setSelectedScreen] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const [viewportSize, setViewportSize] = useState({ w: 1200, h: 800 });

  useEffect(() => {
    const update = () => {
      if (containerRef.current) {
        setViewportSize({
          w: containerRef.current.clientWidth,
          h: containerRef.current.clientHeight,
        });
      }
    };
    update();
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, []);

  const handleZoomIn = useCallback(() => {
    setZoom(z => Math.min(3, z * 1.2));
  }, []);

  const handleZoomOut = useCallback(() => {
    setZoom(z => Math.max(0.2, z / 1.2));
  }, []);

  const handleFitAll = useCallback(() => {
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const s of screens) {
      minX = Math.min(minX, s.position.x);
      minY = Math.min(minY, s.position.y);
      maxX = Math.max(maxX, s.position.x + SCREEN_W);
      maxY = Math.max(maxY, s.position.y + SCREEN_H);
    }
    const pad = 80;
    const worldW = maxX - minX + pad * 2;
    const worldH = maxY - minY + pad * 2;
    const newZoom = Math.min(
      viewportSize.w / worldW,
      viewportSize.h / worldH,
      1.5
    );
    const newPanX = (viewportSize.w - worldW * newZoom) / 2 - (minX - pad) * newZoom;
    const newPanY = (viewportSize.h - worldW * newZoom) / 2 - (minY - pad) * newZoom + 50;
    setZoom(newZoom);
    setPanX(newPanX);
    setPanY(newPanY);
  }, [viewportSize]);

  const handlePanChange = useCallback((x: number, y: number) => {
    setPanX(x);
    setPanY(y);
  }, []);

  const handleMiniMapJump = useCallback((worldX: number, worldY: number) => {
    setPanX(-worldX * zoom + viewportSize.w / 2);
    setPanY(-worldY * zoom + viewportSize.h / 2);
  }, [zoom, viewportSize]);

  return (
    <div ref={containerRef} style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw' }}>
      <Toolbar
        zoom={zoom}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onFitAll={handleFitAll}
      />
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        <Canvas
          zoom={zoom}
          panX={panX}
          panY={panY}
          selectedScreen={selectedScreen}
          searchQuery={searchQuery}
          onZoomChange={setZoom}
          onPanChange={handlePanChange}
          onSelectScreen={setSelectedScreen}
        />
        <MiniMap
          zoom={zoom}
          panX={panX}
          panY={panY}
          viewportWidth={viewportSize.w}
          viewportHeight={viewportSize.h}
          onJump={handleMiniMapJump}
        />
      </div>
    </div>
  );
};

export default App;
