import { BatteryWarning, CheckCircle2, Clock, Navigation, QrCode, Ship, Timer } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  ApiError,
  activeReservationOf,
  activeTripOf,
  cancelReservation,
  endTrip,
  useStore,
  type ProblemCategory,
} from '../../api/store';
import { useUser } from '../../auth/session';
import { CanalMap } from '../../components/CanalMap';
import { Alert, Battery, Dialog, Empty, PageHead, TypeChip } from '../../components/ui';
import { PRICES, formatCents, holdFee, tripCharges } from '../../domain/pricing';
import { RULES, batteryAlert, freeBerths, nearestDockWithFreeBerth } from '../../domain/rules';
import type { Trip } from '../../domain/types';
import { formatClock, formatTime } from '../../lib/format';
import { useNow } from '../../lib/useNow';
import { TripReceipt } from './TripReceipt';

const PROBLEMS: { value: ProblemCategory; label: string }[] = [
  { value: 'ENGINE', label: 'Engine' },
  { value: 'HULL', label: 'Hull' },
  { value: 'BATTERY', label: 'Battery' },
  { value: 'OTHER', label: 'Other' },
];

/** Reservation countdown (US-10, US-11) and the trip in progress (US-08, US-13, US-20, US-38). */
export function MyTrip() {
  const user = useUser();
  useStore((s) => s.reservations);
  useStore((s) => s.trips);
  const [ended, setEnded] = useState<Trip | null>(null);
  const reservation = activeReservationOf(user.id);
  const trip = activeTripOf(user.id);

  if (trip) return <TripInProgress trip={trip} onEnded={setEnded} />;
  if (reservation) return <ReservationCountdown />;
  if (ended) {
    return (
      <>
        <PageHead eyebrow="Trip ended" title="Thanks for Sailing with Barkito" />
        <Alert tone="ok">The boat is locked and the deposit is released, minus the amount below.</Alert>
        <TripReceipt trip={ended} />
        <div className="row">
          <Link to="/boats" className="btn btn-primary">
            Find Another Boat
          </Link>
          <Link to="/trips" className="btn btn-secondary">
            See All Trips
          </Link>
        </div>
      </>
    );
  }
  return (
    <>
      <PageHead eyebrow="My trip" title="No Boat Yet" />
      <div className="card">
        <Empty icon={<Ship aria-hidden="true" />} title="You Have No Reservation or Trip">
          <p>Find a boat at a dock near you, or unlock one that is already in front of you.</p>
          <div className="row">
            <Link to="/boats" className="btn btn-primary">
              Find a Boat
            </Link>
            <Link to="/unlock" className="btn btn-secondary">
              Unlock with QR Code
            </Link>
          </div>
        </Empty>
      </div>
    </>
  );
}

function ReservationCountdown() {
  const user = useUser();
  const now = useNow();
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const [confirming, setConfirming] = useState(false);
  const [cancelledFee, setCancelledFee] = useState<number | null>(null);
  const reservation = activeReservationOf(user.id);
  if (!reservation) return null;
  const boat = boats.find((b) => b.id === reservation.boatId);
  const dock = docks.find((d) => d.id === boat?.dockId);
  if (!boat) return null;

  const holdMs = RULES.holdMinutes * 60_000;
  const elapsed = now - reservation.startedAt;
  const left = Math.max(0, holdMs - elapsed);
  const feeSoFar = holdFee(boat.type, elapsed / 1000);
  const progress = Math.min(1, elapsed / holdMs);

  return (
    <>
      <PageHead
        eyebrow="Reservation"
        title={`${boat.code} Is Waiting at ${dock?.name}`}
        lead="Walk to the dock and scan the QR code on the boat. If you do not unlock it in time, the reservation expires and you pay the full hold fee."
      />
      <div className="grid-2 grid-split">
        <section className="card countdown-card" aria-labelledby="countdown-title">
          <h2 id="countdown-title" className="visually-hidden">
            Time left
          </h2>
          <div className="ring" style={{ ['--p' as string]: progress }} role="timer" aria-label={`${formatClock(left / 1000)} left`}>
            <div>
              <span className="ring-time num">{formatClock(left / 1000)}</span>
              <span className="ring-label">left to unlock</span>
            </div>
          </div>
          <dl className="facts">
            <div>
              <dt>Boat</dt>
              <dd>
                <span translate="no">{boat.code}</span> <TypeChip type={boat.type} />
              </dd>
            </div>
            <div>
              <dt>Dock</dt>
              <dd>
                {dock?.name} · {dock?.district}
              </dd>
            </div>
            <div>
              <dt>Hold fee so far</dt>
              <dd>{formatCents(feeSoFar)}</dd>
            </div>
          </dl>
          <div className="stack-sm">
            <Link to="/unlock" className="btn btn-primary btn-lg btn-block">
              <QrCode aria-hidden="true" /> Scan the QR Code
            </Link>
            <button type="button" className="btn btn-danger btn-block" onClick={() => setConfirming(true)}>
              Cancel Reservation
            </button>
          </div>
        </section>
        <CanalMap
          label="Dock of your reserved boat"
          markers={docks.map((d) => ({ dock: d, count: d.id === dock?.id ? 1 : 0, tone: d.id === dock?.id ? 'ok' : 'empty' }))}
          selectedId={dock?.id}
        />
      </div>

      <Dialog open={confirming} onClose={() => setConfirming(false)} labelledBy="cancel-title">
        <h2 id="cancel-title">Cancel Your Reservation?</h2>
        <p className="muted">
          You pay the minutes {boat.code} was held: {formatCents(feeSoFar)} so far ({formatCents(PRICES.holdPerMinute[boat.type])} per
          started minute). The boat goes back to other users.
        </p>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={() => setConfirming(false)}>
            Keep Reservation
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setCancelledFee(cancelReservation(user.id));
              setConfirming(false);
            }}
          >
            Cancel and Pay {formatCents(feeSoFar)}
          </button>
        </div>
      </Dialog>
      {cancelledFee !== null && <Alert tone="ok">Reservation cancelled. {formatCents(cancelledFee)} was charged.</Alert>}
    </>
  );
}

function TripInProgress({ trip, onEnded }: { trip: Trip; onEnded: (t: Trip) => void }) {
  const user = useUser();
  const now = useNow();
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const maxMinutes = useStore((s) => s.settings.maxTripMinutes);
  const boat = boats.find((b) => b.id === trip.boatId);
  const fromDock = docks.find((d) => d.id === trip.fromDockId) ?? docks[0];

  const [dockId, setDockId] = useState('');
  const [pluggedIn, setPluggedIn] = useState(true);
  const [reporting, setReporting] = useState(false);
  const [problem, setProblem] = useState<ProblemCategory>('ENGINE');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);

  if (!boat) return null;
  const seconds = (now - trip.startedAt) / 1000;
  const minutes = seconds / 60;
  const charges = tripCharges(boat.type, seconds, trip.holdSeconds, maxMinutes);
  const alert = batteryAlert(boat.battery);
  // The live position comes from telemetry; until then the trip's start dock stands in for it.
  const nearest = nearestDockWithFreeBerth(fromDock, docks, boats);
  const warnAt = maxMinutes - 10;

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!dockId) {
      setError('Pick the dock where the boat is moored.');
      return;
    }
    try {
      const done = endTrip(user.id, dockId, pluggedIn, reporting ? { category: problem, note } : undefined);
      onEnded(done);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong. Try again.');
    }
  }

  return (
    <>
      <PageHead
        eyebrow="Trip in progress"
        title={`On the Water with ${boat.code}`}
        lead={`Started at ${formatTime(trip.startedAt)} from ${fromDock?.name}, with ${trip.passengers} ${trip.passengers === 1 ? 'person' : 'people'} on board.`}
      />

      {alert !== 'NONE' && (
        <Alert tone="warn">
          <strong>Battery at {boat.battery} %.</strong> Head to the nearest dock with a free berth
          {nearest ? (
            <>
              : <strong>{nearest.name}</strong> ({freeBerths(nearest, boats)} free).
            </>
          ) : (
            '.'
          )}
        </Alert>
      )}
      {minutes >= maxMinutes ? (
        <Alert tone="danger">
          You reached the {maxMinutes}-minute maximum. Every extra minute costs {formatCents(PRICES.overtimePerMinute[boat.type])}.
        </Alert>
      ) : (
        minutes >= warnAt && (
          <Alert tone="warn">
            {Math.ceil(maxMinutes - minutes)} minutes left before overtime ({formatCents(PRICES.overtimePerMinute[boat.type])}/min).
          </Alert>
        )
      )}

      <div className="trip-hero">
        <div className="trip-clock">
          <Timer aria-hidden="true" />
          <span className="num" role="timer" aria-label="Trip time">
            {formatClock(seconds)}
          </span>
        </div>
        <dl className="trip-figures">
          <div>
            <dt>So far</dt>
            <dd className="num">{formatCents(charges.total)}</dd>
          </div>
          <div>
            <dt>Battery</dt>
            <dd>
              <Battery level={boat.battery} />
            </dd>
          </div>
          <div>
            <dt>Overtime from</dt>
            <dd className="num">{maxMinutes} min</dd>
          </div>
        </dl>
      </div>

      <div className="grid-2 grid-split">
        <div className="stack">
          <CanalMap
            label="Docks and free berths"
            markers={docks.map((d) => {
              const free = freeBerths(d, boats);
              return { dock: d, count: free, tone: free > 0 ? 'ok' : 'alert' };
            })}
            selectedId={dockId || null}
            highlight={alert !== 'NONE' ? nearest?.id : null}
            onSelect={(id) => {
              setDockId(id);
              setError(null);
            }}
          />
          <p className="small muted">
            Numbers on the pins are free berths. {alert !== 'NONE' && 'The green pin is the nearest dock with a free berth.'}
          </p>
        </div>

        <form className="card stack" onSubmit={submit} aria-labelledby="end-title" noValidate>
          <h2 id="end-title">End Your Trip</h2>
          <p className="small muted">Moor the boat in a free berth first. The trip ends once telemetry sees it moored.</p>
          <div className="field">
            <label htmlFor="end-dock">Dock</label>
            <select
              id="end-dock"
              name="dock"
              className="select"
              value={dockId}
              onChange={(e) => {
                setDockId(e.target.value);
                setError(null);
              }}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? 'end-error' : undefined}
            >
              <option value="">Choose the dock…</option>
              {docks.map((d) => {
                const free = freeBerths(d, boats);
                return (
                  <option key={d.id} value={d.id} disabled={free === 0}>
                    {d.name} — {free === 0 ? 'full' : `${free} free`}
                  </option>
                );
              })}
            </select>
            {error && (
              <p id="end-error" className="error" role="alert">
                {error}
              </p>
            )}
          </div>
          <label className="check">
            <input type="checkbox" checked={pluggedIn} onChange={(e) => setPluggedIn(e.target.checked)} />
            I plugged the boat into the berth&rsquo;s charger
          </label>
          <label className="check">
            <input type="checkbox" checked={reporting} onChange={(e) => setReporting(e.target.checked)} />
            Report a problem with the boat
          </label>
          {reporting && (
            <fieldset className="problem">
              <legend className="label">What is wrong?</legend>
              <div className="segmented" role="radiogroup" aria-label="Problem category">
                {PROBLEMS.map((p) => (
                  <button
                    key={p.value}
                    type="button"
                    role="radio"
                    aria-checked={problem === p.value}
                    onClick={() => setProblem(p.value)}
                  >
                    {p.label}
                  </button>
                ))}
              </div>
              <div className="field">
                <label htmlFor="problem-note">Note for the technician (optional)</label>
                <textarea
                  id="problem-note"
                  name="problemNote"
                  className="textarea"
                  autoComplete="off"
                  placeholder="e.g. The throttle sticks when turning left…"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </div>
              <p className="help">Reporting a problem does not change the price of your trip.</p>
            </fieldset>
          )}
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
                <dt>{charges.overtimeMinutes} min overtime</dt>
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
              <dt>If you end now</dt>
              <dd>{formatCents(charges.total)}</dd>
            </div>
          </dl>
          <button type="submit" className="btn btn-primary btn-lg btn-block">
            <CheckCircle2 aria-hidden="true" /> End Trip
          </button>
          <p className="small muted row">
            <Clock aria-hidden="true" width={16} height={16} /> A trip in progress at closing time continues normally.
          </p>
        </form>
      </div>
      {alert === 'NONE' && nearest && (
        <p className="small muted row">
          <Navigation aria-hidden="true" width={16} height={16} /> Nearest dock with a free berth: {nearest.name}.
        </p>
      )}
      {alert === 'OPERATOR' && (
        <p className="small muted row">
          <BatteryWarning aria-hidden="true" width={16} height={16} /> The operator was alerted and can help if you get stuck.
        </p>
      )}
    </>
  );
}
