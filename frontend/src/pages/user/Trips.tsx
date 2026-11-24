import { History } from 'lucide-react';
import { Link } from 'react-router-dom';
import { activeTripOf, useStore } from '../../api/store';
import { useUser } from '../../auth/session';
import { Empty, PageHead, Stat } from '../../components/ui';
import { formatCents } from '../../domain/pricing';
import { formatClock } from '../../lib/format';
import { useNow } from '../../lib/useNow';
import { TripReceipt } from './TripReceipt';

/** US-29: active and past trips with their charges. */
export function Trips() {
  const user = useUser();
  const now = useNow();
  const trips = useStore((s) => s.trips).filter((t) => t.userId === user.id);
  const past = trips.filter((t) => t.endedAt !== null).sort((a, b) => b.startedAt - a.startedAt);
  const active = activeTripOf(user.id);
  const spent = past.reduce((sum, t) => sum + (t.totalCents ?? 0), 0);
  const minutes = past.reduce((sum, t) => sum + ((t.endedAt ?? 0) - t.startedAt) / 60_000, 0);

  return (
    <>
      <PageHead
        eyebrow="History"
        title="Your Trips"
        lead="Every trip with its unlock fee, minutes, overtime and hold fee, so you know what you paid and why."
      />

      <dl className="stats">
        <Stat label="Trips" value={past.length} />
        <Stat label="Time on the water" value={`${Math.round(minutes)} min`} />
        <Stat label="Spent" value={formatCents(spent)} />
        <Stat
          label="Active trip"
          value={active ? formatClock((now - active.startedAt) / 1000) : 'None'}
          hint={active ? <Link to="/trip">Open my trip</Link> : undefined}
        />
      </dl>

      {past.length === 0 ? (
        <div className="card">
          <Empty icon={<History aria-hidden="true" />} title="No Trips Yet">
            <p>Your trips show up here once you end them at a dock.</p>
          </Empty>
        </div>
      ) : (
        <div className="receipts">
          {past.map((t) => (
            <TripReceipt key={t.id} trip={t} />
          ))}
        </div>
      )}
    </>
  );
}
