import type { Boat, BoatType, Dock } from './types';

/** Default values of the business rules (Product Specification 3.2). */
export const RULES = {
  /** BR1: minimum age per boat type. */
  minAge: { STANDARD: 18, XL: 21 } as Record<BoatType, number>,
  /** BR4: passengers per boat type. */
  capacity: { STANDARD: 5, XL: 10 } as Record<BoatType, number>,
  /** BR5: minimum battery for a boat to be booked. */
  minBattery: 30,
  /** BR6: operating hours, in minutes after midnight. */
  opensAt: 8 * 60,
  closesAt: 22 * 60,
  /** BR7: low-battery thresholds during a trip. */
  userWarnBattery: 20,
  operatorAlertBattery: 10,
  /** BR8: a reservation holds the boat for 15 minutes. */
  holdMinutes: 15,
  /** BR12: maximum trip duration and its warnings. */
  maxTripMinutes: 90,
  tripWarnMinutes: [80, 90],
} as const;

export function isBookable(boat: Boat): boolean {
  return boat.state === 'AVAILABLE' && boat.battery >= RULES.minBattery;
}

export function freeBerths(dock: Dock, boats: Boat[]): number {
  const moored = boats.filter((b) => b.dockId === dock.id && b.state !== 'RETIRED').length;
  return Math.max(0, dock.berthsInService - moored);
}

function minutesOfDay(date: Date): number {
  return date.getHours() * 60 + date.getMinutes();
}

/** BR6: reservations close one hold (15 min) before closing time: 21:45 accepted, 21:46 refused. */
export function canReserveAt(date: Date): boolean {
  const m = minutesOfDay(date);
  return m >= RULES.opensAt && m <= RULES.closesAt - RULES.holdMinutes;
}

/** BR6: unlocking is accepted until 21:59; at 22:00 it is refused. */
export function canUnlockAt(date: Date): boolean {
  const m = minutesOfDay(date);
  return m >= RULES.opensAt && m < RULES.closesAt;
}

/** BR1: age in whole years on the given date. */
export function ageOn(birthDate: string, on: Date): number {
  const birth = new Date(birthDate);
  let age = on.getFullYear() - birth.getFullYear();
  const beforeBirthday =
    on.getMonth() < birth.getMonth() ||
    (on.getMonth() === birth.getMonth() && on.getDate() < birth.getDate());
  if (beforeBirthday) age -= 1;
  return age;
}

export function meetsMinimumAge(birthDate: string, type: BoatType, on: Date): boolean {
  return ageOn(birthDate, on) >= RULES.minAge[type];
}

/** BR4: passengers cannot exceed the capacity of the boat type. */
export function fitsCapacity(type: BoatType, passengers: number): boolean {
  return passengers >= 1 && passengers <= RULES.capacity[type];
}

export type BatteryAlert = 'NONE' | 'USER' | 'OPERATOR';

/** BR7: ≤ 20 % warns the user; ≤ 10 % also alerts the operator. */
export function batteryAlert(battery: number): BatteryAlert {
  if (battery <= RULES.operatorAlertBattery) return 'OPERATOR';
  if (battery <= RULES.userWarnBattery) return 'USER';
  return 'NONE';
}

/** Straight-line distance between two docks on the map, used to pick the nearest dock. */
function distance(a: Dock, b: { x: number; y: number }): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

/** BR14 / US-08: the nearest dock with a free berth in service. */
export function nearestDockWithFreeBerth(
  from: { x: number; y: number },
  docks: Dock[],
  boats: Boat[],
): Dock | null {
  const candidates = docks.filter((d) => freeBerths(d, boats) > 0);
  if (candidates.length === 0) return null;
  return candidates.reduce((best, d) => (distance(d, from) < distance(best, from) ? d : best));
}
