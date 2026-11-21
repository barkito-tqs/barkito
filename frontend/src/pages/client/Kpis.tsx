import { Download } from 'lucide-react';
import { useStore } from '../../api/store';
import { Alert, PageHead, Stat } from '../../components/ui';
import { isBookable } from '../../domain/rules';
import { formatPercent } from '../../lib/format';

/**
 * US-31 + US-32. The business KPIs are computed from the fleet; trips per day, time to repair and the
 * technical metrics are sample values until the trip history and the MetricsSource port exist.
 */
const SAMPLE_TRIPS_PER_DAY = [
  { day: 'Mon', trips: 46 },
  { day: 'Tue', trips: 52 },
  { day: 'Wed', trips: 49 },
  { day: 'Thu', trips: 58 },
  { day: 'Fri', trips: 71 },
  { day: 'Sat', trips: 94 },
  { day: 'Sun', trips: 83 },
];

const SAMPLE_TECH = { p95LatencyMs: 182, errorRate: 0.004, telemetryLagS: 2.1 };
const SLO = { p95LatencyMs: 500, errorRate: 0.01, telemetryLagS: 5 };
const SAMPLE_TIME_TO_REPAIR_H = 9.5;

function exportCsv() {
  const rows = [['day', 'trips'], ...SAMPLE_TRIPS_PER_DAY.map((d) => [d.day, String(d.trips)])];
  const blob = new Blob([rows.map((r) => r.join(',')).join('\n')], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'barkito-trips-per-day.csv';
  a.click();
  URL.revokeObjectURL(url);
}

export function Kpis() {
  const boats = useStore((s) => s.boats).filter((b) => b.state !== 'RETIRED' && b.state !== 'INACTIVE');
  const inUse = boats.filter((b) => b.state === 'IN_USE' || b.state === 'RESERVED').length;
  const available = boats.filter(isBookable).length;
  const max = Math.max(...SAMPLE_TRIPS_PER_DAY.map((d) => d.trips));
  const total = SAMPLE_TRIPS_PER_DAY.reduce((s, d) => s + d.trips, 0);

  return (
    <>
      <PageHead
        eyebrow="Municipality of Ria Nova"
        title="Service KPIs"
        lead="Evidence for the monthly report to the city council."
        actions={
          <button type="button" className="btn btn-secondary" onClick={exportCsv}>
            <Download aria-hidden="true" /> Export CSV
          </button>
        }
      />

      <dl className="stats">
        <Stat label="Utilisation now" value={formatPercent(boats.length ? inUse / boats.length : 0)} hint={`${inUse} of ${boats.length} boats out`} />
        <Stat label="Availability" value={formatPercent(boats.length ? (available + inUse) / boats.length : 0)} hint="bookable or in use" />
        <Stat label="Trips per day" value={Math.round(total / SAMPLE_TRIPS_PER_DAY.length)} hint="last 7 days" />
        <Stat label="Time to repair" value={`${SAMPLE_TIME_TO_REPAIR_H} h`} hint="SLA 24 h" />
      </dl>

      <div className="grid-2">
        <section className="card" aria-labelledby="trips-title">
          <div className="card-head">
            <h2 id="trips-title">Trips per Day</h2>
            <p>Last 7 days · {total} trips</p>
          </div>
          <ol className="bars" aria-label="Trips per day, last 7 days">
            {SAMPLE_TRIPS_PER_DAY.map((d) => (
              <li key={d.day}>
                <span className="bar-value num">{d.trips}</span>
                <span className="bar" style={{ height: `${(d.trips / max) * 100}%` }} aria-hidden="true" />
                <span className="bar-label">{d.day}</span>
              </li>
            ))}
          </ol>
        </section>

        <section className="card stack" aria-labelledby="tech-title">
          <div className="card-head">
            <h2 id="tech-title">Technical Metrics</h2>
            <p>Against the service level objectives</p>
          </div>
          <Metric label="API latency (p95)" value={`${SAMPLE_TECH.p95LatencyMs} ms`} ratio={SAMPLE_TECH.p95LatencyMs / SLO.p95LatencyMs} target={`< ${SLO.p95LatencyMs} ms`} />
          <Metric label="Error rate" value={formatPercent(SAMPLE_TECH.errorRate, 1)} ratio={SAMPLE_TECH.errorRate / SLO.errorRate} target={`< ${formatPercent(SLO.errorRate)}`} />
          <Metric label="Telemetry lag" value={`${SAMPLE_TECH.telemetryLagS} s`} ratio={SAMPLE_TECH.telemetryLagS / SLO.telemetryLagS} target={`< ${SLO.telemetryLagS} s`} />
        </section>
      </div>
      <Alert tone="info">Trips per day, time to repair and the technical metrics show sample values until the backend provides them.</Alert>
    </>
  );
}

function Metric({ label, value, ratio, target }: { label: string; value: string; ratio: number; target: string }) {
  const tone = ratio < 0.7 ? 'ok' : ratio < 1 ? 'warn' : 'danger';
  return (
    <div className="metric">
      <div className="row between">
        <span className="small" style={{ fontWeight: 700 }}>
          {label}
        </span>
        <span className="num" style={{ fontWeight: 800 }}>
          {value} <span className="muted small">target {target}</span>
        </span>
      </div>
      <div className={`meter meter-${tone}`} aria-hidden="true">
        <span style={{ width: `${Math.min(100, ratio * 100)}%` }} />
      </div>
    </div>
  );
}
