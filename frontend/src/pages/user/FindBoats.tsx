import { ArrowRight, MapPin, ScanLine, Ship } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError, activeReservationOf, activeTripOf, reserve, useStore } from '../../api/store';
import { useUser } from '../../auth/session';
import { CanalMap } from '../../components/CanalMap';
import { Alert, Battery, Dialog, Empty, PageHead, TypeChip } from '../../components/ui';
import { PRICES, formatCents } from '../../domain/pricing';
import { RULES, freeBerths, isBookable } from '../../domain/rules';
import type { Boat, BoatType } from '../../domain/types';
import { BOAT_TYPE_LABEL } from '../../lib/format';

type TypeFilter = BoatType | 'ALL';

const TYPE_FILTERS: { value: TypeFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'STANDARD', label: 'Standard' },
  { value: 'XL', label: 'XL' },
];

/** US-09: boats available at each dock, by type, with the free berths of each dock. */
export function FindBoats() {
  const user = useUser();
  const docks = useStore((s) => s.docks);
  const boats = useStore((s) => s.boats);
  useStore((s) => s.reservations);
  useStore((s) => s.trips);
  const [params, setParams] = useSearchParams();
  const type = (params.get('type') as TypeFilter) || 'ALL';
  const dockId = params.get('dock');
  const [pending, setPending] = useState<Boat | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  const reservation = activeReservationOf(user.id);
  const trip = activeTripOf(user.id);

  const bookable = boats.filter((b) => isBookable(b) && (type === 'ALL' || b.type === type));
  const bookableAt = (id: string) => bookable.filter((b) => b.dockId === id);
  const selectedDock = docks.find((d) => d.id === dockId) ?? null;

  function link(next: { type?: TypeFilter; dock?: string | null }) {
    const p = new URLSearchParams(params);
    const t = next.type ?? type;
    if (t === 'ALL') p.delete('type');
    else p.set('type', t);
    const d = next.dock === undefined ? dockId : next.dock;
    if (d) p.set('dock', d);
    else p.delete('dock');
    return `?${p.toString()}`;
  }

  function confirmReserve() {
    if (!pending) return;
    setBusy(true);
    try {
      reserve(user.id, pending.id);
      setPending(null);
      setError(null);
      navigate('/trip');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong. Try again.');
    } finally {
      setBusy(false);
    }
  }

  const sortedDocks = [...docks].sort((a, b) => bookableAt(b.id).length - bookableAt(a.id).length);

  return (
    <>
      <PageHead
        eyebrow="Ria Nova canals"
        title="Find a Boat Near You"
        lead={`Boats with at least ${RULES.minBattery} % battery can be reserved for ${RULES.holdMinutes} minutes while you walk to the dock, or unlocked straight away with their QR code.`}
        actions={
          <nav className="segmented" aria-label="Boat type">
            {TYPE_FILTERS.map((f) => (
              <Link key={f.value} to={link({ type: f.value })} aria-current={type === f.value ? 'true' : undefined} replace>
                {f.label}
              </Link>
            ))}
          </nav>
        }
      />

      {(reservation || trip) && (
        <Alert tone="info">
          <div className="row between">
            <span>{trip ? 'You are on a trip.' : 'You have a boat reserved.'}</span>
            <Link to="/trip" className="btn btn-sm btn-secondary">
              Open My Trip <ArrowRight aria-hidden="true" />
            </Link>
          </div>
        </Alert>
      )}

      <div className="grid-2 grid-split">
        <div className="stack">
          <CanalMap
            label="Docks on the canal map"
            markers={docks.map((d) => {
              const count = bookableAt(d.id).length;
              return { dock: d, count, tone: count > 0 ? 'ok' : 'empty' };
            })}
            selectedId={dockId}
            hrefFor={(id) => link({ dock: id })}
            onSelect={(id) => setParams(new URLSearchParams(link({ dock: id }).slice(1)), { replace: true })}
          />
          <p className="small muted">
            Numbers on the pins are boats you can book now
            {type !== 'ALL' ? ` (${BOAT_TYPE_LABEL[type]} only)` : ''}.
          </p>
        </div>

        <div className="stack">
          {selectedDock ? (
            <section className="card" aria-labelledby="dock-title">
              <div className="card-head">
                <div>
                  <h2 id="dock-title">{selectedDock.name}</h2>
                  <p>
                    {selectedDock.district} ·{' '}
                    <span className="num">
                      {freeBerths(selectedDock, boats)} of {selectedDock.berthsInService}
                    </span>{' '}
                    berths free
                  </p>
                </div>
                <Link className="btn btn-ghost btn-sm" to={link({ dock: null })} replace>
                  All Docks
                </Link>
              </div>
              {bookableAt(selectedDock.id).length === 0 ? (
                <Empty icon={<Ship aria-hidden="true" />} title="No Boats to Book Here">
                  <p>Pick another dock on the map, or try again in a few minutes while boats charge.</p>
                </Empty>
              ) : (
                <ul className="boat-list">
                  {bookableAt(selectedDock.id).map((b) => (
                    <li key={b.id} className="boat-row">
                      <div className="boat-main">
                        <div className="boat-id">
                          <span className="code" translate="no">
                            {b.code}
                          </span>
                          <TypeChip type={b.type} />
                        </div>
                        <span className="small muted num">
                          {formatCents(PRICES.unlockFee[b.type])} + {formatCents(PRICES.perMinute[b.type])}/min · up to{' '}
                          {RULES.capacity[b.type]} people
                        </span>
                      </div>
                      <Battery level={b.battery} />
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        disabled={Boolean(reservation || trip)}
                        onClick={() => {
                          setError(null);
                          setPending(b);
                        }}
                      >
                        Reserve
                        <span className="visually-hidden"> {b.code}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          ) : (
            <section className="card card-tight" aria-labelledby="docks-title">
              <h2 id="docks-title" className="visually-hidden">
                Docks
              </h2>
              <ul className="dock-list">
                {sortedDocks.map((d) => {
                  const count = bookableAt(d.id).length;
                  const free = freeBerths(d, boats);
                  return (
                    <li key={d.id}>
                      <Link to={link({ dock: d.id })} className="dock-link" replace>
                        <MapPin aria-hidden="true" className={count > 0 ? 'has-boats' : ''} />
                        <span className="dock-name">
                          <strong>{d.name}</strong>
                          <span className="small muted">
                            {d.district} · <span className="num">{free}</span> {free === 1 ? 'berth' : 'berths'} free
                          </span>
                        </span>
                        <span className={`dock-count num${count === 0 ? ' none' : ''}`}>
                          {count === 0 ? 'None' : `${count} ${count === 1 ? 'boat' : 'boats'}`}
                        </span>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </section>
          )}
        </div>
      </div>

      {!trip && (
        <>
          <div className="fab-spacer" aria-hidden="true" />
          <Link to="/unlock" className="fab">
            <ScanLine aria-hidden="true" />
            {reservation ? 'Scan Your Boat to Unlock' : 'Scan to Unlock'}
          </Link>
        </>
      )}

      <Dialog open={pending !== null} onClose={() => setPending(null)} labelledBy="reserve-title">
        {pending && (
          <>
            <h2 id="reserve-title">Reserve {pending.code}?</h2>
            <p className="muted">
              We hold the boat for {RULES.holdMinutes} minutes. You pay {formatCents(PRICES.holdPerMinute[pending.type])} per
              started minute until you unlock it, up to {formatCents(PRICES.holdPerMinute[pending.type] * RULES.holdMinutes)}.
              The hold fee is added to your trip.
            </p>
            <dl className="facts">
              <div>
                <dt>Dock</dt>
                <dd>{docks.find((d) => d.id === pending.dockId)?.name}</dd>
              </div>
              <div>
                <dt>Type</dt>
                <dd>
                  {BOAT_TYPE_LABEL[pending.type]} · up to {RULES.capacity[pending.type]} people
                </dd>
              </div>
              <div>
                <dt>Battery</dt>
                <dd>
                  <Battery level={pending.battery} />
                </dd>
              </div>
            </dl>
            {error && <Alert tone="danger">{error}</Alert>}
            <div className="dialog-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPending(null)}>
                Not Now
              </button>
              <button type="button" className="btn btn-primary" onClick={confirmReserve} disabled={busy}>
                {busy ? 'Reserving…' : `Reserve ${pending.code}`}
              </button>
            </div>
          </>
        )}
      </Dialog>
    </>
  );
}
