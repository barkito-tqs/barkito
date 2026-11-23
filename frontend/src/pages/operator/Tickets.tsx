import { Ticket as TicketIcon } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { reassignTicket, useStore } from '../../api/store';
import { Pagination, usePage } from '../../components/Pagination';
import { Badge, Empty, PageHead, Stat } from '../../components/ui';
import { TICKET_LABEL, formatDuration, formatTime } from '../../lib/format';
import { useNow } from '../../lib/useNow';

/** US-21, US-23, US-24: open tickets with technician and SLA deadline; overdue ones stand out. */
export function Tickets() {
  const now = useNow(30_000);
  const tickets = useStore((s) => s.tickets);
  const technicians = useStore((s) => s.technicians);
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  const [params] = useSearchParams();
  const showClosed = params.get('view') === 'closed';

  const list = tickets
    .filter((t) => (showClosed ? t.closedAt : !t.closedAt))
    .sort((a, b) => (showClosed ? (b.closedAt ?? 0) - (a.closedAt ?? 0) : a.dueAt - b.dueAt));
  const { page, pages, rows } = usePage(list);
  const open = tickets.filter((t) => !t.closedAt);
  const overdue = open.filter((t) => t.dueAt < now);

  return (
    <>
      <PageHead
        eyebrow="Maintenance"
        title="Tickets"
        lead="Tickets open and are assigned automatically when a boat needs a technician. Reassign one when its technician is unavailable."
      />
      <dl className="stats">
        <Stat label="Open" value={open.length} />
        <Stat
          label="Overdue"
          value={overdue.length}
          hint={overdue.length ? 'SLA breached' : 'All within SLA'}
        />
        <Stat
          label="Tow"
          value={open.filter((t) => t.type === 'TOW').length}
          hint="on site in 30 min"
        />
        <Stat
          label="Repair"
          value={open.filter((t) => t.type === 'REPAIR').length}
          hint="24 h SLA"
        />
      </dl>

      <nav className="segmented" aria-label="Ticket view">
        <Link to="?" aria-current={!showClosed ? 'true' : undefined} replace>
          Open
        </Link>
        <Link to="?view=closed" aria-current={showClosed ? 'true' : undefined} replace>
          Closed
        </Link>
      </nav>

      {list.length === 0 ? (
        <div className="card">
          <Empty
            icon={<TicketIcon aria-hidden="true" />}
            title={showClosed ? 'No Closed Tickets Yet' : 'No Open Tickets'}
          />
        </div>
      ) : (
        <div className="table-wrap">
          <table className="table table-cards">
            <thead>
              <tr>
                <th scope="col">Ticket</th>
                <th scope="col">Boat</th>
                <th scope="col">Deadline</th>
                <th scope="col">Technician</th>
                <th scope="col">Last note</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => {
                const boat = boats.find((b) => b.id === t.boatId);
                const dock = docks.find((d) => d.id === boat?.dockId);
                const late = !t.closedAt && t.dueAt < now;
                return (
                  <tr key={t.id} className={late ? 'row-late' : undefined}>
                    <td data-label="Ticket">
                      <span className="cell-stack">
                        <span className="code">{t.id}</span>
                        <span className="small muted">{TICKET_LABEL[t.type]}</span>
                      </span>
                    </td>
                    <td data-label="Boat">
                      <span className="cell-stack">
                        <span className="code" translate="no">
                          {boat?.code}
                        </span>
                        <span className="small muted">{dock?.name ?? 'On the canal'}</span>
                      </span>
                    </td>
                    <td data-label="Deadline">
                      {t.closedAt ? (
                        <Badge tone={t.closedAt <= t.dueAt ? 'ok' : 'danger'}>
                          Closed {formatTime(t.closedAt)} ·{' '}
                          {t.closedAt <= t.dueAt ? 'within SLA' : 'late'}
                        </Badge>
                      ) : late ? (
                        <Badge tone="danger">Overdue by {formatDuration(now - t.dueAt)}</Badge>
                      ) : (
                        <Badge tone={t.dueAt - now < 30 * 60_000 ? 'warn' : 'info'}>
                          {formatTime(t.dueAt)} · in {formatDuration(t.dueAt - now)}
                        </Badge>
                      )}
                    </td>
                    <td data-label="Technician">
                      {t.closedAt ? (
                        technicians.find((x) => x.id === t.technicianId)?.name
                      ) : (
                        <>
                          <label className="visually-hidden" htmlFor={`assign-${t.id}`}>
                            Technician for {t.id}
                          </label>
                          <select
                            id={`assign-${t.id}`}
                            className="select select-compact"
                            value={t.technicianId}
                            onChange={(e) => reassignTicket(t.id, e.target.value)}
                          >
                            {technicians.map((x) => (
                              <option key={x.id} value={x.id}>
                                {x.name}
                              </option>
                            ))}
                          </select>
                        </>
                      )}
                    </td>
                    <td className="small muted note-cell" data-label="Last note">
                      {t.notes.at(-1) ?? '—'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={page} pages={pages} total={list.length} noun="tickets" />
    </>
  );
}
