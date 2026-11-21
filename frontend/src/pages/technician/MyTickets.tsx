import { ArrowLeft, CheckCircle2, ChevronRight, MapPin, Wrench } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { addTicketNote, closeTicket, useStore } from '../../api/store';
import { useUser } from '../../auth/session';
import { Badge, Battery, Empty, PageHead, StateBadge, TypeChip } from '../../components/ui';
import type { Ticket } from '../../domain/types';
import { TICKET_LABEL, formatDuration, formatTime } from '../../lib/format';
import { useNow } from '../../lib/useNow';

const TOW_ON_SITE_MS = 30 * 60_000;

function DueBadge({ ticket, now }: { ticket: Ticket; now: number }) {
  if (ticket.dueAt < now) return <Badge tone="danger">Overdue {formatDuration(now - ticket.dueAt)}</Badge>;
  return (
    <Badge tone={ticket.dueAt - now < 30 * 60_000 ? 'warn' : 'info'}>
      Due {formatTime(ticket.dueAt)} · in {formatDuration(ticket.dueAt - now)}
    </Badge>
  );
}

/** US-22: a Field Technician's own tickets, most urgent first. Each one opens on its own page. */
export function MyTickets() {
  const user = useUser();
  const now = useNow(30_000);
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const tickets = useStore((s) => s.tickets)
    .filter((t) => t.technicianId === user.id && !t.closedAt)
    .sort((a, b) => a.dueAt - b.dueAt);
  const closedToday = useStore((s) => s.tickets).filter((t) => t.technicianId === user.id && t.closedAt).length;

  return (
    <>
      <PageHead
        eyebrow={`${tickets.length} open · ${closedToday} closed`}
        title="My Tickets"
        lead="Ordered by SLA deadline. Leave a ticket open with a note when a repair needs the workshop."
      />
      {tickets.length === 0 ? (
        <div className="card">
          <Empty icon={<Wrench aria-hidden="true" />} title="All Clear">
            <p>No open tickets. New ones show up here as soon as a boat needs you.</p>
          </Empty>
        </div>
      ) : (
        <ol className="card card-tight ticket-rows">
          {tickets.map((t) => {
            const boat = boats.find((b) => b.id === t.boatId);
            const dock = docks.find((d) => d.id === boat?.dockId);
            return (
              <li key={t.id}>
                <Link to={`/my-tickets/${t.id}`} className={`ticket-row${t.dueAt < now ? ' late' : ''}`}>
                  <span className="ticket-row-main">
                    <span className="row" style={{ gap: 8 }}>
                      <Badge tone={t.type === 'TOW' || t.type === 'REPAIR' ? 'danger' : 'warn'} plain>
                        {TICKET_LABEL[t.type]}
                      </Badge>
                      <span className="code" translate="no">
                        {boat?.code}
                      </span>
                      <span className="small muted">{t.id}</span>
                    </span>
                    <span className="small muted">{dock?.name ?? 'On the canal'}</span>
                  </span>
                  <DueBadge ticket={t} now={now} />
                  <ChevronRight aria-hidden="true" className="chev" />
                </Link>
              </li>
            );
          })}
        </ol>
      )}
    </>
  );
}

/** US-22 + US-25: one ticket, with the boat, notes, arrival on site for towing, and closing. */
export function TicketDetail() {
  const { ticketId } = useParams();
  const user = useUser();
  const now = useNow(30_000);
  const ticket = useStore((s) => s.tickets).find((t) => t.id === ticketId && t.technicianId === user.id);
  if (!ticket) {
    return (
      <div className="card">
        <Empty icon={<Wrench aria-hidden="true" />} title="Ticket Not Found">
          <p>It may have been closed or reassigned to another technician.</p>
          <Link className="btn btn-primary" to="/my-tickets">
            Back to My Tickets
          </Link>
        </Empty>
      </div>
    );
  }
  return (
    <>
      <Link to="/my-tickets" className="btn btn-ghost btn-sm back-link">
        <ArrowLeft aria-hidden="true" /> My Tickets
      </Link>
      <TicketCard ticket={ticket} now={now} />
    </>
  );
}

function TicketCard({ ticket, now }: { ticket: Ticket; now: number }) {
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const navigate = useNavigate();
  const [note, setNote] = useState('');
  const boat = boats.find((b) => b.id === ticket.boatId);
  const dock = docks.find((d) => d.id === boat?.dockId);
  const late = ticket.dueAt < now;
  const onSiteDue = ticket.openedAt + TOW_ON_SITE_MS;
  const arrived = ticket.notes.some((n) => n.startsWith('Arrived on site'));

  function submit(event: FormEvent) {
    event.preventDefault();
    addTicketNote(ticket.id, note);
    setNote('');
  }

  return (
    <article className={`card ticket-card${late ? ' late' : ''}`}>
      <div className="row between">
        <div className="row">
          <Badge tone={ticket.type === 'TOW' || ticket.type === 'REPAIR' ? 'danger' : 'warn'} plain>
            {TICKET_LABEL[ticket.type]}
          </Badge>
          <span className="code">{ticket.id}</span>
        </div>
        <DueBadge ticket={ticket} now={now} />
      </div>

      {boat && (
        <div className="ticket-boat">
          <span className="row" style={{ gap: 8 }}>
            <span className="code" translate="no">
              {boat.code}
            </span>
            <TypeChip type={boat.type} />
            <StateBadge state={boat.state} />
          </span>
          <span className="row small muted" style={{ gap: 6 }}>
            <MapPin aria-hidden="true" width={16} height={16} />
            {dock ? `${dock.name}${boat.berth ? `, berth ${boat.berth}` : ''}` : 'On the canal'}
          </span>
          <Battery level={boat.battery} />
        </div>
      )}

      {ticket.type === 'TOW' && (
        <div className="row">
          {arrived ? (
            <Badge tone="ok">On site</Badge>
          ) : (
            <>
              <Badge tone={now > onSiteDue ? 'danger' : 'warn'}>Be on site by {formatTime(onSiteDue)}</Badge>
              <button type="button" className="btn btn-sm btn-secondary" onClick={() => addTicketNote(ticket.id, `Arrived on site at ${formatTime(Date.now())}.`)}>
                I Am on Site
              </button>
            </>
          )}
        </div>
      )}

      {ticket.notes.length > 0 && (
        <ul className="notes">
          {ticket.notes.map((n, i) => (
            <li key={i}>{n}</li>
          ))}
        </ul>
      )}

      <form className="note-form" onSubmit={submit}>
        <label className="visually-hidden" htmlFor={`note-${ticket.id}`}>
          Note for {ticket.id}
        </label>
        <input
          id={`note-${ticket.id}`}
          name="note"
          className="input"
          autoComplete="off"
          placeholder="What did you find or do…"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
        <button type="submit" className="btn btn-secondary" disabled={!note.trim()}>
          Add Note
        </button>
      </form>

      <button
        type="button"
        className="btn btn-primary btn-block"
        onClick={() => {
          closeTicket(ticket.id);
          navigate('/my-tickets');
        }}
      >
        <CheckCircle2 aria-hidden="true" /> Close Ticket
      </button>
    </article>
  );
}
