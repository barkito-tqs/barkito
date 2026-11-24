import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';
import { RULES } from '../domain/rules';
import type { BoatState, BoatType } from '../domain/types';
import { BOAT_TYPE_LABEL, STATE_LABEL, STATE_TONE, type Tone } from '../lib/format';

export function PageHead({
  eyebrow,
  title,
  lead,
  actions,
}: {
  eyebrow?: string;
  title: string;
  lead?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="page-head">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {lead && <p>{lead}</p>}
      </div>
      {actions && <div className="row">{actions}</div>}
    </header>
  );
}

export function Badge({
  tone,
  children,
  plain,
}: {
  tone: Tone;
  children: ReactNode;
  plain?: boolean;
}) {
  return <span className={`badge badge-${tone}${plain ? ' badge-plain' : ''}`}>{children}</span>;
}

export function StateBadge({ state }: { state: BoatState }) {
  return <Badge tone={STATE_TONE[state]}>{STATE_LABEL[state]}</Badge>;
}

export function TypeChip({ type }: { type: BoatType }) {
  return (
    <span className={`chip-type${type === 'XL' ? ' xl' : ''}`}>
      {BOAT_TYPE_LABEL[type].toUpperCase()}
    </span>
  );
}

export function Battery({ level }: { level: number }) {
  const tone =
    level < RULES.operatorAlertBattery + 1 ? 'critical' : level < RULES.minBattery ? 'low' : 'ok';
  return (
    <span className={`battery ${tone}`} aria-label={`Battery ${level} %`}>
      <span className="battery-track" aria-hidden="true">
        <span className="battery-fill" style={{ width: `${Math.max(4, level)}%` }} />
      </span>
      {level}&nbsp;%
    </span>
  );
}

const ALERT_ICON = { info: Info, warn: AlertTriangle, danger: XCircle, ok: CheckCircle2 };

export function Alert({ tone, children }: { tone: keyof typeof ALERT_ICON; children: ReactNode }) {
  const Icon = ALERT_ICON[tone];
  return (
    <div className={`alert alert-${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <Icon aria-hidden="true" />
      <div>{children}</div>
    </div>
  );
}

export function Stat({
  label,
  value,
  hint,
}: {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
}) {
  return (
    <div className="stat">
      <dt>{label}</dt>
      <dd>
        {value}
        {hint && <span className="hint">{hint}</span>}
      </dd>
    </div>
  );
}

export function Empty({
  icon,
  title,
  children,
}: {
  icon: ReactNode;
  title: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      {icon}
      <h2>{title}</h2>
      {children}
    </div>
  );
}

/** Native <dialog>, so focus trapping, Escape and the backdrop come from the browser. */
export function Dialog({
  open,
  onClose,
  labelledBy,
  children,
}: {
  open: boolean;
  onClose: () => void;
  labelledBy: string;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog ref={ref} className="dialog" aria-labelledby={labelledBy} onClose={onClose}>
      {open && <div className="dialog-body">{children}</div>}
    </dialog>
  );
}
