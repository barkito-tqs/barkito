import { Anchor, ArrowLeft } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useStore } from '../../api/store';
import { BoatRows } from '../../components/BoatRows';
import { Empty, PageHead, Stat } from '../../components/ui';
import { freeBerths, isBookable } from '../../domain/rules';
import { NEEDS_ATTENTION } from './Docks';

/** US-04 + US-30: one dock with its berths and the boats moored there; each boat opens its own page. */
export function DockDetail() {
  const { dockId } = useParams();
  const dock = useStore((s) => s.docks).find((d) => d.id === dockId);
  const boats = useStore((s) => s.boats);

  if (!dock) {
    return (
      <div className="card">
        <Empty icon={<Anchor aria-hidden="true" />} title="Dock Not Found">
          <Link className="btn btn-primary" to="/docks">
            Back to Docks
          </Link>
        </Empty>
      </div>
    );
  }

  // Boats needing attention first, then by berth.
  const moored = boats
    .filter((b) => b.dockId === dock.id && b.state !== 'RETIRED')
    .sort((a, b) => Number(NEEDS_ATTENTION.includes(b.state)) - Number(NEEDS_ATTENTION.includes(a.state)) || (a.berth ?? 99) - (b.berth ?? 99));

  return (
    <>
      <Link to="/docks" className="btn btn-ghost btn-sm back-link">
        <ArrowLeft aria-hidden="true" /> Docks
      </Link>
      <PageHead eyebrow={dock.district} title={dock.name} />

      <dl className="stats">
        <Stat label="Boats moored" value={moored.length} hint={`${moored.filter(isBookable).length} bookable now`} />
        <Stat label="Free berths" value={freeBerths(dock, boats)} hint={`of ${dock.berthsInService} in service`} />
        <Stat label="Berths in service" value={dock.berthsInService} hint={`of ${dock.berths} physical`} />
        <Stat label="Need attention" value={moored.filter((b) => NEEDS_ATTENTION.includes(b.state)).length} hint="charge, repair, tow, return" />
      </dl>

      {moored.length === 0 ? (
        <div className="card">
          <Empty icon={<Anchor aria-hidden="true" />} title="No Boats Moored Here" />
        </div>
      ) : (
        <BoatRows boats={moored} fromDockId={dock.id} label={`Boats at ${dock.name}`} />
      )}
    </>
  );
}
