import { Anchor } from 'lucide-react';
import { useState } from 'react';
import { ApiError, setBerthsInService, useStore } from '../../api/store';
import { Alert, Dialog, PageHead } from '../../components/ui';
import type { Dock } from '../../domain/types';
import { freeBerths } from '../../domain/rules';

interface PendingChange {
  dock: Dock;
  to: number;
}

/**
 * US-27: berths in service per dock. Once they are full, no trip can end at that dock (BR14).
 * A change is a draft until it is saved and then confirmed, so a stray tap never closes berths.
 */
export function Berths() {
  const docks = useStore((s) => s.docks);
  const [pending, setPending] = useState<PendingChange | null>(null);
  const [result, setResult] = useState<{ tone: 'ok' | 'danger'; text: string } | null>(null);

  function confirm() {
    if (!pending) return;
    try {
      setBerthsInService(pending.dock.id, pending.to);
      setResult({ tone: 'ok', text: `${pending.dock.name} now has ${pending.to} berths in service.` });
    } catch (e) {
      setResult({ tone: 'danger', text: e instanceof ApiError ? e.message : 'Something went wrong.' });
    }
    setPending(null);
  }

  return (
    <>
      <PageHead
        eyebrow="Client backoffice"
        title="Berths in Service"
        lead="Reduce the berths at crowded docks. Once a dock's berths in service are full, users end their trip at another dock."
      />
      {result && <Alert tone={result.tone}>{result.text}</Alert>}
      <div className="dock-cards">
        {docks.map((d) => (
          <DockBerthsCard key={`${d.id}-${d.berthsInService}`} dock={d} onSave={(to) => setPending({ dock: d, to })} />
        ))}
      </div>

      <ConfirmBerths pending={pending} onCancel={() => setPending(null)} onConfirm={confirm} />
    </>
  );
}

function DockBerthsCard({ dock, onSave }: { dock: Dock; onSave: (to: number) => void }) {
  const boats = useStore((s) => s.boats);
  const [draft, setDraft] = useState(dock.berthsInService);
  const moored = boats.filter((b) => b.dockId === dock.id && b.state !== 'RETIRED').length;
  const changed = draft !== dock.berthsInService;

  return (
    <section className="card stack dock-card" aria-labelledby={`dock-${dock.id}`}>
      <div className="row between">
        <div>
          <h2 id={`dock-${dock.id}`}>{dock.name}</h2>
          <p className="small muted">{dock.district}</p>
        </div>
        <Anchor aria-hidden="true" className="muted" />
      </div>
      <dl className="facts">
        <div>
          <dt>Physical berths</dt>
          <dd>{dock.berths}</dd>
        </div>
        <div>
          <dt>Boats moored now</dt>
          <dd>{moored}</dd>
        </div>
        <div>
          <dt>Free now</dt>
          <dd>{freeBerths(dock, boats)}</dd>
        </div>
      </dl>
      <div className="row between">
        <span className="label small" id={`berths-${dock.id}`} style={{ fontWeight: 700 }}>
          Berths in service
        </span>
        <div className="stepper" role="group" aria-labelledby={`berths-${dock.id}`}>
          <button type="button" aria-label="One berth fewer" disabled={draft <= 0} onClick={() => setDraft(draft - 1)}>
            −
          </button>
          <output aria-live="polite">{draft}</output>
          <button type="button" aria-label="One berth more" disabled={draft >= dock.berths} onClick={() => setDraft(draft + 1)}>
            +
          </button>
        </div>
      </div>
      <div className="row dock-card-actions">
        <button type="button" className="btn btn-secondary btn-sm" disabled={!changed} onClick={() => setDraft(dock.berthsInService)}>
          Undo
        </button>
        <button type="button" className="btn btn-primary btn-sm" disabled={!changed} onClick={() => onSave(draft)}>
          Save Change
        </button>
      </div>
    </section>
  );
}

function ConfirmBerths({ pending, onCancel, onConfirm }: { pending: PendingChange | null; onCancel: () => void; onConfirm: () => void }) {
  const boats = useStore((s) => s.boats);
  if (!pending) return <Dialog open={false} onClose={onCancel} labelledBy="berths-confirm-title">{null}</Dialog>;
  const { dock, to } = pending;
  const moored = boats.filter((b) => b.dockId === dock.id && b.state !== 'RETIRED').length;
  const fullAfter = to <= moored;

  return (
    <Dialog open onClose={onCancel} labelledBy="berths-confirm-title">
      <h2 id="berths-confirm-title">Change Berths at {dock.name}?</h2>
      <dl className="facts">
        <div>
          <dt>Berths in service now</dt>
          <dd>{dock.berthsInService}</dd>
        </div>
        <div className="total">
          <dt>After the change</dt>
          <dd>{to}</dd>
        </div>
      </dl>
      {fullAfter ? (
        <Alert tone="warn">
          {dock.name} will be full: users can no longer end a trip there until a boat leaves. Trips in progress end at another dock.
        </Alert>
      ) : (
        <p className="muted small">The new value applies at once to users looking for a dock to end their trip.</p>
      )}
      <div className="dialog-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Keep {dock.berthsInService}
        </button>
        <button type="button" className="btn btn-primary" onClick={onConfirm}>
          Confirm {to} Berths
        </button>
      </div>
    </Dialog>
  );
}
