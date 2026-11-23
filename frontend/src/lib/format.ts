import type { BoatState, BoatType, TicketType } from '../domain/types';

const time = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' });
const dateTime = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});

export function formatTime(ms: number): string {
  return time.format(ms);
}

export function formatDateTime(ms: number): string {
  return dateTime.format(ms);
}

export function formatPercent(ratio: number, fractionDigits = 0): string {
  return new Intl.NumberFormat('en-GB', {
    style: 'percent',
    maximumFractionDigits: fractionDigits,
  }).format(ratio);
}

/** mm:ss, or h:mm:ss past one hour. */
export function formatClock(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mmss = `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  return h > 0 ? `${h}:${mmss}` : mmss;
}

/** "1 h 20 min", "35 min", "overdue by 12 min". */
export function formatDuration(ms: number): string {
  const minutes = Math.round(Math.abs(ms) / 60_000);
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

export const BOAT_TYPE_LABEL: Record<BoatType, string> = { STANDARD: 'Standard', XL: 'XL' };

export const STATE_LABEL: Record<BoatState, string> = {
  INACTIVE: 'Inactive',
  CHARGING: 'Charging',
  AVAILABLE: 'Available',
  RESERVED: 'Reserved',
  IN_USE: 'On a trip',
  CHARGE_REQUIRED: 'Needs charge',
  REPAIR_REQUIRED: 'Needs repair',
  TOW_REQUIRED: 'Needs towing',
  RETURN_REQUIRED: 'Needs return',
  RETIRED: 'Retired',
};

export type Tone = 'ok' | 'warn' | 'danger' | 'info' | 'idle';

export const STATE_TONE: Record<BoatState, Tone> = {
  INACTIVE: 'idle',
  CHARGING: 'info',
  AVAILABLE: 'ok',
  RESERVED: 'info',
  IN_USE: 'info',
  CHARGE_REQUIRED: 'warn',
  REPAIR_REQUIRED: 'danger',
  TOW_REQUIRED: 'danger',
  RETURN_REQUIRED: 'warn',
  RETIRED: 'idle',
};

export const TICKET_LABEL: Record<TicketType, string> = {
  CHARGE: 'Charge',
  REPAIR: 'Repair',
  TOW: 'Tow',
  RETURN: 'Return',
};

export function initials(name: string): string {
  return name
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
