import { Anchor, ArrowLeft, Plus } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, addBerths, useStore } from '../../api/store';
import { BoatRows } from '../../components/BoatRows';
import { Alert, Dialog, Empty, PageHead, Stat } from '../../components/ui';
import { RULES, berthsLeftToAdd, freeBerths, isBookable } from '../../domain/rules';
import type { Dock } from '../../domain/types';
import { NEEDS_ATTENTION } from './Docks';

/**
 * US-04 + US-30: one dock with its berths and the boats moored there; each boat opens its own page.
 * BRK-321: the operator adds berths to the dock, up to the maximum per dock.
 */
export function DockDetail() {
  const { dockId } = useParams();
  const dock = useStore((s) => s.docks).find((d) => d.id === dockId);
  const boats = useStore((s) => s.boats);
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState<string | null>(null);

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
    .sort(
      (a, b) =>
        Number(NEEDS_ATTENTION.includes(b.state)) - Number(NEEDS_ATTENTION.includes(a.state)) ||
        (a.berth ?? 99) - (b.berth ?? 99),
    );

  return (
    <>
      <Link to="/docks" className="btn btn-ghost btn-sm back-link">
        <ArrowLeft aria-hidden="true" /> Docks
      </Link>
      <PageHead
        eyebrow={dock.district}
        title={dock.name}
        actions={
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => setAdding(true)}
            disabled={berthsLeftToAdd(dock) === 0}
            title={
              berthsLeftToAdd(dock) === 0
                ? `A dock has at most ${RULES.maxBerthsPerDock} berths.`
                : undefined
            }
          >
            <Plus aria-hidden="true" /> Add Berths
          </button>
        }
      />
      {added && <Alert tone="ok">{added}</Alert>}

      <dl className="stats">
        <Stat
          label="Boats moored"
          value={moored.length}
          hint={`${moored.filter(isBookable).length} bookable now`}
        />
        <Stat
          label="Free berths"
          value={freeBerths(dock, boats)}
          hint={`of ${dock.berthsInService} in service`}
        />
        <Stat
          label="Berths in service"
          value={dock.berthsInService}
          hint={`of ${dock.berths} physical`}
        />
        <Stat
          label="Need attention"
          value={moored.filter((b) => NEEDS_ATTENTION.includes(b.state)).length}
          hint="charge, repair, tow, return"
        />
      </dl>

      {moored.length === 0 ? (
        <div className="card">
          <Empty icon={<Anchor aria-hidden="true" />} title="No Boats Moored Here" />
        </div>
      ) : (
        <BoatRows boats={moored} fromDockId={dock.id} label={`Boats at ${dock.name}`} />
      )}
      <AddBerthsDialog
        dock={dock}
        open={adding}
        onClose={() => setAdding(false)}
        onDone={setAdded}
      />
    </>
  );
}

function AddBerthsDialog({
  dock,
  open,
  onClose,
  onDone,
}: {
  dock: Dock;
  open: boolean;
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const [count, setCount] = useState('1');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const left = berthsLeftToAdd(dock);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const updated = await addBerths(dock.id, Number(count));
      const first = dock.berths + 1;
      const last = updated.berths;
      const added =
        last === first
          ? `Berth ${first}`
          : `Berths ${first} ${last === first + 1 ? 'and' : 'to'} ${last}`;
      onDone(`${added} added to ${updated.name}, in service.`);
      setCount('1');
      setError(null);
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} labelledBy="berths-title">
      <form className="stack" onSubmit={submit} noValidate>
        <h2 id="berths-title">Add Berths to {dock.name}</h2>
        <p className="small muted">
          The dock has <span className="num">{dock.berths}</span> berths and can take{' '}
          <span className="num">{left}</span> more. New berths continue the numbering and start in
          service.
        </p>
        <div className="field">
          <label htmlFor="berths-count">Berths to add</label>
          <input
            id="berths-count"
            name="count"
            className="input"
            type="number"
            inputMode="numeric"
            min={1}
            max={left}
            value={count}
            onChange={(e) => setCount(e.target.value)}
          />
        </div>
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            Add Berths
          </button>
        </div>
      </form>
    </Dialog>
  );
}
