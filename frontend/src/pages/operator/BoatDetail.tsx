import { ArrowLeft, Archive, Power, PowerOff, Ship } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, activateBoat, deactivateBoat, retireBoat, useStore } from '../../api/store';
import { Alert, Battery, Dialog, Empty, StateBadge, TypeChip } from '../../components/ui';
import type { Boat, BoatState } from '../../domain/types';
import { STATE_LABEL, TICKET_LABEL, formatTime } from '../../lib/format';

type Action = 'activate' | 'deactivate' | 'retire';

const RETIRABLE: BoatState[] = ['INACTIVE', 'REPAIR_REQUIRED', 'TOW_REQUIRED', 'RETURN_REQUIRED'];
const DEACTIVATABLE: BoatState[] = ['AVAILABLE', 'CHARGING'];

const COPY: Record<Action, { title: (b: Boat) => string; body: (b: Boat) => string; confirm: (b: Boat) => string }> = {
  activate: {
    title: (b) => `Activate ${b.code}?`,
    body: () => 'The boat starts charging and appears to users once its battery reaches 30 %. It must be moored in a berth and plugged in.',
    confirm: (b) => `Activate ${b.code}`,
  },
  deactivate: {
    title: (b) => `Deactivate ${b.code}?`,
    body: () => 'The boat disappears from the app for users until you activate it again.',
    confirm: (b) => `Deactivate ${b.code}`,
  },
  retire: {
    title: (b) => `Retire ${b.code} for Good?`,
    body: (b) =>
      `Current state: ${STATE_LABEL[b.state]}. A retired boat leaves the fleet and cannot come back. Retire it only if it is lost or beyond repair.`,
    confirm: (b) => `Retire ${b.code}`,
  },
};

/** US-05 + US-06: one boat, with its telemetry, open tickets and lifecycle actions, each confirmed first. */
export function BoatDetail() {
  const { boatId } = useParams();
  const boat = useStore((s) => s.boats).find((b) => b.id === boatId);
  const docks = useStore((s) => s.docks);
  const tickets = useStore((s) => s.tickets);
  const [params] = useSearchParams();
  const fromDock = docks.find((d) => d.id === params.get('from'));
  const [pending, setPending] = useState<Action | null>(null);
  const [result, setResult] = useState<{ tone: 'ok' | 'danger'; text: string } | null>(null);

  if (!boat) {
    return (
      <div className="card">
        <Empty icon={<Ship aria-hidden="true" />} title="Boat Not Found">
          <Link className="btn btn-primary" to="/fleet">
            Back to Fleet
          </Link>
        </Empty>
      </div>
    );
  }

  const dock = docks.find((d) => d.id === boat.dockId);
  const open = tickets.filter((t) => t.boatId === boat.id && !t.closedAt);
  const canActivate = boat.state === 'INACTIVE';
  const canDeactivate = DEACTIVATABLE.includes(boat.state);
  const canRetire = RETIRABLE.includes(boat.state);

  function confirm() {
    if (!pending || !boat) return;
    const run = { activate: activateBoat, deactivate: deactivateBoat, retire: retireBoat }[pending];
    try {
      run(boat.id);
      setResult({ tone: 'ok', text: `${boat.code}: ${pending === 'retire' ? 'retired' : pending === 'activate' ? 'activated' : 'deactivated'}.` });
    } catch (e) {
      setResult({ tone: 'danger', text: e instanceof ApiError ? e.message : 'Something went wrong.' });
    }
    setPending(null);
  }

  return (
    <>
      <Link to={fromDock ? `/docks/${fromDock.id}` : '/fleet'} className="btn btn-ghost btn-sm back-link">
        <ArrowLeft aria-hidden="true" /> {fromDock ? fromDock.name : 'Fleet'}
      </Link>

      <header className="page-head">
        <div>
          <span className="eyebrow">Boat</span>
          <h1 className="row" style={{ gap: 12 }}>
            <span translate="no">{boat.code}</span>
            <TypeChip type={boat.type} />
          </h1>
        </div>
        <StateBadge state={boat.state} />
      </header>

      {result && <Alert tone={result.tone}>{result.text}</Alert>}

      <div className="grid-2">
        <section className="card stack" aria-labelledby="boat-status">
          <h2 id="boat-status">Status</h2>
          <dl className="facts">
            <div>
              <dt>Battery</dt>
              <dd>
                <Battery level={boat.battery} />
              </dd>
            </div>
            <div>
              <dt>Dock</dt>
              <dd>
                {dock ? dock.name : 'On the canal'}
                {boat.berth ? `, berth ${boat.berth}` : ''}
              </dd>
            </div>
            <div>
              <dt>Charger</dt>
              <dd>{boat.pluggedIn ? 'Plugged in' : 'Unplugged'}</dd>
            </div>
            <div>
              <dt>Code below the QR code</dt>
              <dd className="num">{boat.unlockCode}</dd>
            </div>
          </dl>
          {open.length > 0 && (
            <div className="stack-sm">
              <h3>Open Tickets</h3>
              {open.map((t) => (
                <p key={t.id} className="small">
                  <span className="code">{t.id}</span> · {TICKET_LABEL[t.type]} · due {formatTime(t.dueAt)}
                </p>
              ))}
            </div>
          )}
        </section>

        <section className="card stack" aria-labelledby="boat-actions">
          <h2 id="boat-actions">Actions</h2>
          <ActionRow
            icon={<Power aria-hidden="true" />}
            title="Activate"
            text="Put an inactive boat back in service."
            enabled={canActivate}
            disabledText="Only an inactive boat can be activated."
            onClick={() => setPending('activate')}
          />
          <ActionRow
            icon={<PowerOff aria-hidden="true" />}
            title="Deactivate"
            text="Take an available or charging boat out of service."
            enabled={canDeactivate}
            disabledText={
              boat.state === 'IN_USE' || boat.state === 'RESERVED'
                ? 'The boat is on a trip or reserved. Deactivate it once it is back.'
                : 'Only an available or charging boat can be deactivated.'
            }
            onClick={() => setPending('deactivate')}
          />
          <ActionRow
            icon={<Archive aria-hidden="true" />}
            title="Retire"
            text="Remove a lost or irreparable boat from the fleet for good."
            enabled={canRetire}
            disabledText="Only an inactive boat, or one that needs repair, towing or return, can be retired."
            onClick={() => setPending('retire')}
            danger
          />
        </section>
      </div>

      <Dialog open={pending !== null} onClose={() => setPending(null)} labelledBy="action-title">
        {pending && (
          <>
            <h2 id="action-title">{COPY[pending].title(boat)}</h2>
            <p className="muted">{COPY[pending].body(boat)}</p>
            <div className="dialog-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPending(null)}>
                Cancel
              </button>
              <button type="button" className={`btn ${pending === 'retire' ? 'btn-danger-solid' : 'btn-primary'}`} onClick={confirm}>
                {COPY[pending].confirm(boat)}
              </button>
            </div>
          </>
        )}
      </Dialog>
    </>
  );
}

function ActionRow({
  icon,
  title,
  text,
  enabled,
  disabledText,
  onClick,
  danger,
}: {
  icon: ReactNode;
  title: string;
  text: string;
  enabled: boolean;
  disabledText: string;
  onClick: () => void;
  danger?: boolean;
}) {
  return (
    <div className="action-row">
      <span className={`action-icon${danger ? ' danger' : ''}`}>{icon}</span>
      <div className="action-text">
        <strong>{title}</strong>
        <span className="small muted">{enabled ? text : disabledText}</span>
      </div>
      <button type="button" className={`btn btn-sm ${danger ? 'btn-danger' : 'btn-secondary'}`} disabled={!enabled} onClick={onClick}>
        {title}
      </button>
    </div>
  );
}
