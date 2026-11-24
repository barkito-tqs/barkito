import { useStore } from '../../api/store';
import { TypeChip } from '../../components/ui';
import { PRICES, formatCents, tripCharges } from '../../domain/pricing';
import type { Trip } from '../../domain/types';
import { formatDateTime, formatDuration } from '../../lib/format';

/** Trip charges broken down: unlock fee, normal minutes, overtime and hold fee (US-29, BR11, BR12). */
export function TripReceipt({ trip }: { trip: Trip }) {
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const maxMinutes = useStore((s) => s.settings.maxTripMinutes);
  const boat = boats.find((b) => b.id === trip.boatId);
  if (!boat || trip.endedAt === null) return null;
  const charges = tripCharges(
    boat.type,
    (trip.endedAt - trip.startedAt) / 1000,
    trip.holdSeconds,
    maxMinutes,
  );
  const from = docks.find((d) => d.id === trip.fromDockId)?.name ?? '—';
  const to = docks.find((d) => d.id === trip.toDockId)?.name ?? '—';

  return (
    <article className="card receipt">
      <header className="row between">
        <div>
          <h3>
            {from} → {to}
          </h3>
          <p className="small muted">
            {formatDateTime(trip.startedAt)} · {formatDuration(trip.endedAt - trip.startedAt)}
          </p>
        </div>
        <span className="row">
          <span className="code" translate="no">
            {boat.code}
          </span>
          <TypeChip type={boat.type} />
        </span>
      </header>
      <dl className="facts">
        <div>
          <dt>Unlock fee</dt>
          <dd>{formatCents(charges.unlockFee)}</dd>
        </div>
        <div>
          <dt>
            {charges.normalMinutes} min × {formatCents(PRICES.perMinute[boat.type])}
          </dt>
          <dd>{formatCents(charges.normalCost)}</dd>
        </div>
        {charges.overtimeMinutes > 0 && (
          <div>
            <dt>
              {charges.overtimeMinutes} min overtime ×{' '}
              {formatCents(PRICES.overtimePerMinute[boat.type])}
            </dt>
            <dd>{formatCents(charges.overtimeCost)}</dd>
          </div>
        )}
        {charges.holdFee > 0 && (
          <div>
            <dt>Hold fee</dt>
            <dd>{formatCents(charges.holdFee)}</dd>
          </div>
        )}
        <div className="total">
          <dt>Total</dt>
          <dd>{formatCents(charges.total)}</dd>
        </div>
      </dl>
    </article>
  );
}
