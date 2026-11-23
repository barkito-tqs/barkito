import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useStore } from '../api/store';
import type { Boat } from '../domain/types';
import { Battery, StateBadge, TypeChip } from './ui';

/** Compact list of boats; each row opens the boat's page. `fromDockId` makes its back link return to that dock. */
export function BoatRows({
  boats,
  fromDockId,
  label,
}: {
  boats: Boat[];
  fromDockId?: string;
  label: string;
}) {
  const docks = useStore((s) => s.docks);
  const dockName = (id: string | null) => docks.find((d) => d.id === id)?.name ?? 'On the canal';

  return (
    <ul className="card card-tight link-rows" aria-label={label}>
      {boats.map((b) => (
        <li key={b.id}>
          <Link
            to={fromDockId ? `/fleet/${b.id}?from=${fromDockId}` : `/fleet/${b.id}`}
            className="link-row fleet-row"
          >
            <span className="link-row-main">
              <span className="row" style={{ gap: 8 }}>
                <span className="code" translate="no">
                  {b.code}
                </span>
                <TypeChip type={b.type} />
              </span>
              <span className="small muted">
                {fromDockId ? (b.berth ? `Berth ${b.berth}` : 'No berth') : dockName(b.dockId)}
                {fromDockId && ` · ${b.pluggedIn ? 'plugged in' : 'unplugged'}`}
              </span>
            </span>
            <StateBadge state={b.state} />
            <Battery level={b.battery} />
            <ChevronRight aria-hidden="true" className="chev" />
          </Link>
        </li>
      ))}
    </ul>
  );
}
