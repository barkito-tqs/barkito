import type { MouseEvent } from 'react';
import type { Dock } from '../domain/types';

export interface DockMarker {
  dock: Dock;
  /** Number shown on the pin, e.g. bookable boats. */
  count: number;
  tone: 'ok' | 'empty' | 'alert';
}

/**
 * Stylised map of the Ria Nova canals. Positions are map units (1000 × 600), not coordinates,
 * because the city is fictional; the real map comes with the GBFS feed (US-37) at the earliest.
 */
export function CanalMap({
  markers,
  selectedId,
  onSelect,
  hrefFor,
  label,
  highlight,
}: {
  markers: DockMarker[];
  selectedId?: string | null;
  onSelect?: (dockId: string) => void;
  hrefFor?: (dockId: string) => string;
  label: string;
  /** A second dock to point out, e.g. the nearest free berth (US-08). */
  highlight?: string | null;
}) {
  function handleClick(event: MouseEvent, dockId: string) {
    if (!onSelect) return;
    event.preventDefault();
    onSelect(dockId);
  }

  return (
    <figure className="canal-map">
      <svg viewBox="0 0 1000 600" role="group" aria-label={label}>
        <defs>
          <pattern id="map-grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M40 0H0V40" fill="none" stroke="#dfe7ee" strokeWidth="1" />
          </pattern>
        </defs>
        <rect width="1000" height="600" fill="#f2f6f9" />
        <rect width="1000" height="600" fill="url(#map-grid)" />
        {/* City blocks */}
        <g fill="#e6edf3">
          <rect x="40" y="40" width="250" height="150" rx="14" />
          <rect x="560" y="30" width="220" height="110" rx="14" />
          <rect x="620" y="230" width="140" height="150" rx="14" />
          <rect x="60" y="360" width="170" height="90" rx="14" />
          <rect x="380" y="430" width="300" height="130" rx="14" />
        </g>
        {/* Canals */}
        <g fill="none" strokeLinecap="round" className="canals">
          <path d="M-20 300 C 160 220, 320 330, 470 300 S 760 230, 1020 330" strokeWidth="46" />
          <path d="M470 300 C 480 220, 450 160, 470 90 S 520 -10, 540 -20" strokeWidth="30" />
          <path d="M470 300 C 520 380, 700 420, 820 470 S 960 560, 1020 580" strokeWidth="34" />
          <path d="M150 260 C 210 360, 250 450, 300 500 S 360 620, 380 640" strokeWidth="26" />
          <path d="M700 270 C 760 230, 820 200, 860 170 S 940 100, 1020 80" strokeWidth="26" />
        </g>
        <g fill="none" strokeLinecap="round" className="canals-center">
          <path d="M-20 300 C 160 220, 320 330, 470 300 S 760 230, 1020 330" strokeWidth="2" />
          <path d="M470 300 C 520 380, 700 420, 820 470 S 960 560, 1020 580" strokeWidth="2" />
        </g>

        {markers.map(({ dock, count, tone }) => {
          const selected = dock.id === selectedId;
          const highlighted = dock.id === highlight;
          const name = `${dock.name}: ${count} ${count === 1 ? 'boat' : 'boats'}`;
          return (
            <a
              key={dock.id}
              href={hrefFor?.(dock.id) ?? '#'}
              aria-label={name}
              aria-current={selected ? 'true' : undefined}
              className={`dock-pin tone-${tone}${selected ? ' selected' : ''}${highlighted ? ' highlighted' : ''}`}
              onClick={(e) => handleClick(e, dock.id)}
            >
              <g transform={`translate(${dock.x} ${dock.y})`}>
                {(selected || highlighted) && <circle r="34" className="pin-halo" />}
                <path
                  className="pin-body"
                  d="M0 0 C -6 -10, -22 -20, -22 -36 A 22 22 0 1 1 22 -36 C 22 -20, 6 -10, 0 0 Z"
                />
                <text className="pin-count" y="-30" textAnchor="middle">
                  {count}
                </text>
                <text className="pin-label" y="22" textAnchor="middle">
                  {dock.name}
                </text>
              </g>
            </a>
          );
        })}
      </svg>
    </figure>
  );
}
