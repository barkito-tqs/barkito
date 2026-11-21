import { useState, type FormEvent } from 'react';
import { ApiError, updateSettings, useStore, type Settings } from '../../api/store';
import { Alert, PageHead } from '../../components/ui';
import { RULES } from '../../domain/rules';
import type { TicketType } from '../../domain/types';
import { TICKET_LABEL } from '../../lib/format';

function minusMinutes(hhmm: string, minutes: number): string {
  const [h, m] = hhmm.split(':').map(Number);
  const total = h * 60 + m - minutes;
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

/** US-26 + US-28: rules of use and SLA deadlines. New values apply to the next trips and tickets. */
export function Rules() {
  const saved = useStore((s) => s.settings);
  const [draft, setDraft] = useState<Settings>(saved);
  const [result, setResult] = useState<{ tone: 'ok' | 'danger'; text: string } | null>(null);
  const dirty = JSON.stringify(draft) !== JSON.stringify(saved);

  function submit(event: FormEvent) {
    event.preventDefault();
    try {
      updateSettings(draft);
      setResult({ tone: 'ok', text: 'Saved. The new rules apply to the next trips; trips in progress keep their rules.' });
    } catch (e) {
      setResult({ tone: 'danger', text: e instanceof ApiError ? e.message : 'Something went wrong.' });
    }
  }

  const sla = (type: TicketType, hours: number) => setDraft({ ...draft, slaHours: { ...draft.slaHours, [type]: hours } });

  return (
    <form className="stack" onSubmit={submit} noValidate>
      <PageHead
        eyebrow="Client backoffice"
        title="Rules of Use"
        lead="The rules the city sets for the service. Barkito enforces them on every reservation, unlock and ticket."
        actions={
          <button type="submit" className="btn btn-primary" disabled={!dirty}>
            Save Rules
          </button>
        }
      />
      {result && <Alert tone={result.tone}>{result.text}</Alert>}

      <div className="grid-2">
        <section className="card stack" aria-labelledby="trips-rules">
          <h2 id="trips-rules">Trips</h2>
          <div className="field">
            <label htmlFor="max-trip">Maximum trip duration (minutes)</label>
            <input
              id="max-trip"
              name="maxTripMinutes"
              className="input"
              type="number"
              inputMode="numeric"
              min={15}
              value={draft.maxTripMinutes}
              onChange={(e) => setDraft({ ...draft, maxTripMinutes: Number(e.target.value) })}
            />
            <p className="help">Users are warned 10 minutes before and at the limit; later minutes are overtime. Default {RULES.maxTripMinutes}.</p>
          </div>
          <div className="form-grid cols-2">
            <div className="field">
              <label htmlFor="opens">Opens at</label>
              <input id="opens" name="opensAt" className="input" type="time" value={draft.opensAt} onChange={(e) => setDraft({ ...draft, opensAt: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="closes">Closes at</label>
              <input id="closes" name="closesAt" className="input" type="time" value={draft.closesAt} onChange={(e) => setDraft({ ...draft, closesAt: e.target.value })} />
            </div>
          </div>
          <p className="help">
            Reservations close one hold ({RULES.holdMinutes} min) before closing: last reservation at{' '}
            <strong className="num">{minusMinutes(draft.closesAt, RULES.holdMinutes)}</strong>, last unlock at{' '}
            <strong className="num">{minusMinutes(draft.closesAt, 1)}</strong>.
          </p>
          <div className="form-grid cols-2">
            <div className="field">
              <label htmlFor="age-standard">Minimum age, Standard</label>
              <input
                id="age-standard"
                name="minAgeStandard"
                className="input"
                type="number"
                inputMode="numeric"
                min={16}
                value={draft.minAge.STANDARD}
                onChange={(e) => setDraft({ ...draft, minAge: { ...draft.minAge, STANDARD: Number(e.target.value) } })}
              />
            </div>
            <div className="field">
              <label htmlFor="age-xl">Minimum age, XL</label>
              <input
                id="age-xl"
                name="minAgeXl"
                className="input"
                type="number"
                inputMode="numeric"
                min={16}
                value={draft.minAge.XL}
                onChange={(e) => setDraft({ ...draft, minAge: { ...draft.minAge, XL: Number(e.target.value) } })}
              />
            </div>
          </div>
        </section>

        <section className="card stack" aria-labelledby="sla-rules">
          <h2 id="sla-rules">SLA Deadlines</h2>
          <p className="small muted">Time to close each ticket type. The clock runs within operating hours, except for towing.</p>
          <div className="form-grid cols-2">
            {(Object.keys(draft.slaHours) as TicketType[]).map((type) => (
              <div className="field" key={type}>
                <label htmlFor={`sla-${type}`}>{TICKET_LABEL[type]} (hours)</label>
                <input
                  id={`sla-${type}`}
                  name={`sla${type}`}
                  className="input"
                  type="number"
                  inputMode="decimal"
                  min={0.5}
                  step={0.5}
                  value={draft.slaHours[type]}
                  onChange={(e) => sla(type, Number(e.target.value))}
                />
              </div>
            ))}
          </div>
          <p className="help">Towing also needs a technician on site within 30 minutes. New deadlines apply to tickets opened afterwards.</p>
        </section>
      </div>
    </form>
  );
}
