import { Plus, Ship } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ApiError, registerBoat, useStore } from '../../api/store';
import { BoatRows } from '../../components/BoatRows';
import { PAGE_SIZE, Pagination, usePage } from '../../components/Pagination';
import { Alert, Dialog, Empty, PageHead, Stat } from '../../components/ui';
import type { BoatState, BoatType } from '../../domain/types';

const FILTERS: { value: string; label: string; states: BoatState[] }[] = [
  { value: 'all', label: 'All', states: [] },
  {
    value: 'service',
    label: 'In service',
    states: ['AVAILABLE', 'RESERVED', 'IN_USE', 'CHARGING'],
  },
  {
    value: 'attention',
    label: 'Needs attention',
    states: ['CHARGE_REQUIRED', 'REPAIR_REQUIRED', 'TOW_REQUIRED', 'RETURN_REQUIRED'],
  },
  { value: 'inactive', label: 'Inactive', states: ['INACTIVE', 'RETIRED'] },
];

/** US-03, US-07, US-30: the fleet by state and dock, updated live from telemetry. Each boat opens on its own page. */
export function Fleet() {
  const boats = useStore((s) => s.boats);
  const tickets = useStore((s) => s.tickets);
  const [params] = useSearchParams();
  const filter = FILTERS.find((f) => f.value === params.get('state')) ?? FILTERS[0];
  const [registering, setRegistering] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const shown = boats.filter((b) => filter.states.length === 0 || filter.states.includes(b.state));
  const count = (states: BoatState[]) => boats.filter((b) => states.includes(b.state)).length;
  const { page, pages, rows } = usePage(shown, PAGE_SIZE);
  const overdue = tickets.filter((t) => !t.closedAt && t.dueAt < Date.now()).length;

  return (
    <>
      <PageHead
        eyebrow="Operations"
        title="Fleet"
        lead="Boat states come from telemetry and update live."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setRegistering(true)}>
            <Plus aria-hidden="true" /> Register Boat
          </button>
        }
      />

      <dl className="stats">
        <Stat
          label="Available"
          value={count(['AVAILABLE'])}
          hint={`of ${boats.filter((b) => b.state !== 'RETIRED').length} boats`}
        />
        <Stat
          label="On the water"
          value={count(['IN_USE', 'RESERVED'])}
          hint="trips and reservations"
        />
        <Stat
          label="Need attention"
          value={FILTERS[2].states.reduce((n, s) => n + count([s]), 0)}
          hint="charge, repair, tow, return"
        />
        <Stat
          label="Overdue tickets"
          value={overdue}
          hint={<Link to="/tickets">Open tickets</Link>}
        />
      </dl>

      {message && <Alert tone="ok">{message}</Alert>}

      <div className="row between">
        <nav className="segmented" aria-label="Filter boats">
          {FILTERS.map((f) => (
            <Link
              key={f.value}
              to={f.value === 'all' ? '?' : `?state=${f.value}`}
              aria-current={filter === f ? 'true' : undefined}
              replace
            >
              {f.label}
            </Link>
          ))}
        </nav>
        <span className="small muted num">{shown.length} boats</span>
      </div>

      {shown.length === 0 ? (
        <div className="card">
          <Empty icon={<Ship aria-hidden="true" />} title="No Boats in This View" />
        </div>
      ) : (
        <BoatRows boats={rows} label="Boats" />
      )}
      <Pagination page={page} pages={pages} total={shown.length} noun="boats" />

      <RegisterBoatDialog
        open={registering}
        onClose={() => setRegistering(false)}
        onDone={(code) =>
          setMessage(`${code} registered. It stays inactive until you activate it.`)
        }
      />
    </>
  );
}

function RegisterBoatDialog({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: (code: string) => void;
}) {
  const docks = useStore((s) => s.docks);
  const [code, setCode] = useState('');
  const [type, setType] = useState<BoatType>('STANDARD');
  const [dockId, setDockId] = useState('');
  const [error, setError] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const boat = registerBoat(code, type, dockId);
      onDone(boat.code);
      setCode('');
      setError(null);
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong.');
    }
  }

  return (
    <Dialog open={open} onClose={onClose} labelledBy="register-title">
      <form className="stack" onSubmit={submit} noValidate>
        <h2 id="register-title">Register a Boat</h2>
        <div className="field">
          <label htmlFor="boat-code">Boat code</label>
          <input
            id="boat-code"
            name="boatCode"
            className="input"
            autoComplete="off"
            spellCheck={false}
            placeholder="e.g. B-16…"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? 'register-error' : undefined}
          />
        </div>
        <div className="field">
          <span className="label" id="type-label">
            Type
          </span>
          <div className="segmented" role="radiogroup" aria-labelledby="type-label">
            {(['STANDARD', 'XL'] as BoatType[]).map((t) => (
              <button
                key={t}
                type="button"
                role="radio"
                aria-checked={type === t}
                onClick={() => setType(t)}
              >
                {t === 'XL' ? 'XL · 10 people' : 'Standard · 5 people'}
              </button>
            ))}
          </div>
        </div>
        <div className="field">
          <label htmlFor="boat-dock">Home dock</label>
          <select
            id="boat-dock"
            name="homeDock"
            className="select"
            value={dockId}
            onChange={(e) => setDockId(e.target.value)}
          >
            <option value="">Choose a dock…</option>
            {docks.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        {error && (
          <p
            id="register-error"
            className="error"
            role="alert"
            style={{ color: 'var(--danger)', fontWeight: 600 }}
          >
            {error}
          </p>
        )}
        <p className="small muted">
          A new boat starts inactive. Activate it once telemetry shows it moored and plugged in.
        </p>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary">
            Register Boat
          </button>
        </div>
      </form>
    </Dialog>
  );
}
