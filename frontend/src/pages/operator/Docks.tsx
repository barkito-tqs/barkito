import { ChevronRight, Plus } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, registerDock, useStore } from '../../api/store';
import { CanalMap } from '../../components/CanalMap';
import { Alert, Badge, Dialog, PageHead } from '../../components/ui';
import { freeBerths } from '../../domain/rules';
import type { BoatState } from '../../domain/types';

export const NEEDS_ATTENTION: BoatState[] = [
  'CHARGE_REQUIRED',
  'REPAIR_REQUIRED',
  'TOW_REQUIRED',
  'RETURN_REQUIRED',
];

/** US-04: docks and their berths; each dock opens with its boats. Berths in service are set by the municipality (US-27). */
export function Docks() {
  const docks = useStore((s) => s.docks);
  const boats = useStore((s) => s.boats);
  const [open, setOpen] = useState(false);
  const [done, setDone] = useState<string | null>(null);
  const navigate = useNavigate();

  return (
    <>
      <PageHead
        eyebrow="Operations"
        title="Docks and Berths"
        lead="Every trip starts and ends at a dock. Each berth has a charger and holds one boat."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setOpen(true)}>
            <Plus aria-hidden="true" /> Register Dock
          </button>
        }
      />
      {done && <Alert tone="ok">{done} registered with all berths in service.</Alert>}
      <div className="grid-2 grid-split">
        <div className="stack">
          <CanalMap
            label="Docks with the number of moored boats"
            markers={docks.map((d) => {
              const moored = boats.filter((b) => b.dockId === d.id).length;
              return { dock: d, count: moored, tone: freeBerths(d, boats) === 0 ? 'alert' : 'ok' };
            })}
            hrefFor={(id) => `/docks/${id}`}
            onSelect={(id) => navigate(`/docks/${id}`)}
          />
          <p className="small muted">
            Numbers on the pins are boats moored now. Open a dock to manage its boats.
          </p>
        </div>
        <ul className="card card-tight link-rows" aria-label="Docks">
          {docks.map((d) => {
            const free = freeBerths(d, boats);
            const moored = boats.filter((b) => b.dockId === d.id).length;
            const attention = boats.filter(
              (b) => b.dockId === d.id && NEEDS_ATTENTION.includes(b.state),
            ).length;
            return (
              <li key={d.id}>
                <Link to={`/docks/${d.id}`} className="link-row dock-row">
                  <span className="link-row-main">
                    <strong>{d.name}</strong>
                    <span className="small muted">
                      {d.district} · <span className="num">{moored}</span>{' '}
                      {moored === 1 ? 'boat' : 'boats'} · <span className="num">{free}</span> of{' '}
                      <span className="num">{d.berthsInService}</span> berths free
                    </span>
                  </span>
                  {attention > 0 ? (
                    <Badge tone="warn">
                      {attention} {attention === 1 ? 'needs' : 'need'} attention
                    </Badge>
                  ) : free === 0 ? (
                    <Badge tone="danger">Full</Badge>
                  ) : (
                    <span />
                  )}
                  <ChevronRight aria-hidden="true" className="chev" />
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
      <RegisterDockDialog open={open} onClose={() => setOpen(false)} onDone={setDone} />
    </>
  );
}

function RegisterDockDialog({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: (name: string) => void;
}) {
  const [name, setName] = useState('');
  const [district, setDistrict] = useState('');
  const [berths, setBerths] = useState('4');
  const [error, setError] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const dock = registerDock(name, district, Number(berths));
      onDone(dock.name);
      setName('');
      setDistrict('');
      setError(null);
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong.');
    }
  }

  return (
    <Dialog open={open} onClose={onClose} labelledBy="dock-title">
      <form className="stack" onSubmit={submit} noValidate>
        <h2 id="dock-title">Register a Dock</h2>
        <div className="field">
          <label htmlFor="dock-name">Name</label>
          <input
            id="dock-name"
            name="dockName"
            className="input"
            autoComplete="off"
            placeholder="e.g. Fish Market…"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="form-grid cols-2">
          <div className="field">
            <label htmlFor="dock-district">District</label>
            <input
              id="dock-district"
              name="district"
              className="input"
              autoComplete="off"
              placeholder="e.g. Ribeira…"
              value={district}
              onChange={(e) => setDistrict(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="dock-berths">Berths</label>
            <input
              id="dock-berths"
              name="berths"
              className="input"
              type="number"
              inputMode="numeric"
              min={1}
              value={berths}
              onChange={(e) => setBerths(e.target.value)}
            />
          </div>
        </div>
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary">
            Register Dock
          </button>
        </div>
      </form>
    </Dialog>
  );
}
