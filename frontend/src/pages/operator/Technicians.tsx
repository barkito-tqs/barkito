import { useState } from 'react';
import { setTechnicianDocks, useStore } from '../../api/store';
import { Alert, Dialog, PageHead } from '../../components/ui';
import type { Dock, Technician } from '../../domain/types';
import { initials } from '../../lib/format';

interface PendingChange {
  technician: Technician;
  dockIds: string[];
}

/**
 * US-39: the docks each Field Technician covers, used to assign tickets automatically.
 * Ticks are a draft until they are saved and then confirmed, so a stray tap never moves coverage.
 */
export function Technicians() {
  const technicians = useStore((s) => s.technicians);
  const docks = useStore((s) => s.docks);
  const [pending, setPending] = useState<PendingChange | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const uncovered = docks.filter((d) => !technicians.some((t) => t.dockIds.includes(d.id)));

  function confirm() {
    if (!pending) return;
    setTechnicianDocks(pending.technician.id, pending.dockIds);
    setDone(`${pending.technician.name} now covers ${pending.dockIds.length} ${pending.dockIds.length === 1 ? 'dock' : 'docks'}.`);
    setPending(null);
  }

  return (
    <>
      <PageHead
        eyebrow="Maintenance"
        title="Field Technicians"
        lead="A ticket goes to the technician who covers the boat's dock. A dock with no technician sends its tickets to the nearest covered dock. Changes apply to tickets opened afterwards."
      />
      {done && <Alert tone="ok">{done}</Alert>}
      {uncovered.length > 0 && (
        <Alert tone="warn">
          No technician covers {uncovered.map((d) => d.name).join(', ')}. Their tickets go to the nearest covered dock.
        </Alert>
      )}
      <div className="grid-2">
        {technicians.map((t) => (
          <TechnicianCard key={`${t.id}-${t.dockIds.join()}`} technician={t} docks={docks} onSave={(dockIds) => setPending({ technician: t, dockIds })} />
        ))}
      </div>

      <Dialog open={pending !== null} onClose={() => setPending(null)} labelledBy="coverage-title">
        {pending && <ConfirmCoverage pending={pending} docks={docks} onCancel={() => setPending(null)} onConfirm={confirm} />}
      </Dialog>
    </>
  );
}

function TechnicianCard({ technician, docks, onSave }: { technician: Technician; docks: Dock[]; onSave: (dockIds: string[]) => void }) {
  const tickets = useStore((s) => s.tickets);
  const [draft, setDraft] = useState<string[]>(technician.dockIds);
  const open = tickets.filter((x) => x.technicianId === technician.id && !x.closedAt).length;
  const changed = draft.length !== technician.dockIds.length || draft.some((d) => !technician.dockIds.includes(d));

  return (
    <section className="card stack" aria-labelledby={`tech-${technician.id}`}>
      <div className="row">
        <span className="avatar" aria-hidden="true">
          {initials(technician.name)}
        </span>
        <div>
          <h2 id={`tech-${technician.id}`}>{technician.name}</h2>
          <p className="small muted">
            {open} open {open === 1 ? 'ticket' : 'tickets'} · {technician.dockIds.length} {technician.dockIds.length === 1 ? 'dock' : 'docks'}
          </p>
        </div>
      </div>
      <fieldset className="dock-checks">
        <legend className="visually-hidden">Docks covered by {technician.name}</legend>
        {docks.map((d) => (
          <label key={d.id} className="check">
            <input
              type="checkbox"
              checked={draft.includes(d.id)}
              onChange={(e) => setDraft(e.target.checked ? [...draft, d.id] : draft.filter((x) => x !== d.id))}
            />
            {d.name}
          </label>
        ))}
      </fieldset>
      <div className="row dock-card-actions">
        <button type="button" className="btn btn-secondary btn-sm" disabled={!changed} onClick={() => setDraft(technician.dockIds)}>
          Undo
        </button>
        <button type="button" className="btn btn-primary btn-sm" disabled={!changed} onClick={() => onSave(draft)}>
          Save Changes
        </button>
      </div>
    </section>
  );
}

function ConfirmCoverage({
  pending,
  docks,
  onCancel,
  onConfirm,
}: {
  pending: PendingChange;
  docks: Dock[];
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const { technician, dockIds } = pending;
  const name = (id: string) => docks.find((d) => d.id === id)?.name ?? id;
  const added = dockIds.filter((d) => !technician.dockIds.includes(d));
  const removed = technician.dockIds.filter((d) => !dockIds.includes(d));

  return (
    <>
      <h2 id="coverage-title">Change Docks for {technician.name}?</h2>
      <dl className="facts">
        {added.length > 0 && (
          <div>
            <dt>Starts covering</dt>
            <dd>{added.map(name).join(', ')}</dd>
          </div>
        )}
        {removed.length > 0 && (
          <div>
            <dt>Stops covering</dt>
            <dd>{removed.map(name).join(', ')}</dd>
          </div>
        )}
      </dl>
      <p className="muted small">New tickets follow the new coverage. Open tickets keep their technician unless you reassign them.</p>
      <div className="dialog-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Keep Current Docks
        </button>
        <button type="button" className="btn btn-primary" onClick={onConfirm}>
          Confirm Changes
        </button>
      </div>
    </>
  );
}
