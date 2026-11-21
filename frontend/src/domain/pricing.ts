import { RULES } from './rules';
import type { BoatType } from './types';

/** Prices in cents (BR8, BR11, BR12, BR13). */
export const PRICES = {
  unlockFee: { STANDARD: 100, XL: 200 } as Record<BoatType, number>,
  perMinute: { STANDARD: 50, XL: 100 } as Record<BoatType, number>,
  overtimePerMinute: { STANDARD: 75, XL: 150 } as Record<BoatType, number>,
  holdPerMinute: { STANDARD: 25, XL: 50 } as Record<BoatType, number>,
  deposit: { STANDARD: 3000, XL: 6000 } as Record<BoatType, number>,
} as const;

/** Every started minute counts as a full minute (BR11). */
export function startedMinutes(seconds: number): number {
  return seconds <= 0 ? 0 : Math.ceil(seconds / 60);
}

/** BR8: the hold fee is the started minutes of hold, capped at 15 minutes. */
export function holdFee(type: BoatType, holdSeconds: number): number {
  const minutes = Math.min(startedMinutes(holdSeconds), RULES.holdMinutes);
  return minutes * PRICES.holdPerMinute[type];
}

export interface TripCharges {
  unlockFee: number;
  normalMinutes: number;
  normalCost: number;
  overtimeMinutes: number;
  overtimeCost: number;
  holdFee: number;
  total: number;
}

/** BR11 + BR12 + BR8: unlock fee, normal minutes, overtime after the maximum duration and the hold fee. */
export function tripCharges(
  type: BoatType,
  tripSeconds: number,
  holdSeconds = 0,
  maxTripMinutes: number = RULES.maxTripMinutes,
): TripCharges {
  const minutes = startedMinutes(tripSeconds);
  const normalMinutes = Math.min(minutes, maxTripMinutes);
  const overtimeMinutes = Math.max(0, minutes - maxTripMinutes);
  const unlockFee = PRICES.unlockFee[type];
  const normalCost = normalMinutes * PRICES.perMinute[type];
  const overtimeCost = overtimeMinutes * PRICES.overtimePerMinute[type];
  const hold = holdSeconds > 0 ? holdFee(type, holdSeconds) : 0;
  return {
    unlockFee,
    normalMinutes,
    normalCost,
    overtimeMinutes,
    overtimeCost,
    holdFee: hold,
    total: unlockFee + normalCost + overtimeCost + hold,
  };
}

const euro = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

export function formatCents(cents: number): string {
  return euro.format(cents / 100);
}
