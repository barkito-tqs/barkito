import {
  batteryAlert,
  canReserveAt,
  canUnlockAt,
  fitsCapacity,
  freeBerths,
  isBookable,
  meetsMinimumAge,
  nearestDockWithFreeBerth,
} from './rules';
import type { Boat, Dock } from './types';

function boat(overrides: Partial<Boat> = {}): Boat {
  return {
    id: 'b1',
    code: 'B-01',
    type: 'STANDARD',
    state: 'AVAILABLE',
    battery: 80,
    dockId: 'd1',
    berth: 1,
    pluggedIn: true,
    unlockCode: '000000',
    reservedBy: null,
    ...overrides,
  };
}

function dock(overrides: Partial<Dock> = {}): Dock {
  return {
    id: 'd1',
    name: 'Dock',
    district: 'Centre',
    x: 0,
    y: 0,
    berths: 4,
    berthsInService: 4,
    ...overrides,
  };
}

function at(hours: number, minutes: number): Date {
  return new Date(2026, 10, 20, hours, minutes);
}

describe('bookable boats (BR5)', () => {
  it('refuses 29 % and accepts 30 %', () => {
    expect(isBookable(boat({ battery: 29 }))).toBe(false);
    expect(isBookable(boat({ battery: 30 }))).toBe(true);
  });

  it('refuses a boat that is not AVAILABLE', () => {
    expect(isBookable(boat({ state: 'RESERVED' }))).toBe(false);
  });
});

describe('operating hours (BR6)', () => {
  it('accepts a reservation at 21:45 and refuses it at 21:46', () => {
    expect(canReserveAt(at(21, 45))).toBe(true);
    expect(canReserveAt(at(21, 46))).toBe(false);
  });

  it('accepts an unlock at 21:59 and refuses it at 22:00', () => {
    expect(canUnlockAt(at(21, 59))).toBe(true);
    expect(canUnlockAt(at(22, 0))).toBe(false);
  });

  it('refuses an unlock at 07:59 and accepts it at 08:00', () => {
    expect(canUnlockAt(at(7, 59))).toBe(false);
    expect(canUnlockAt(at(8, 0))).toBe(true);
  });
});

describe('minimum age (BR1) and capacity (BR4)', () => {
  const on = new Date(2026, 10, 20);

  it('refuses 17 and accepts 18 on a Standard boat', () => {
    expect(meetsMinimumAge('2009-11-21', 'STANDARD', on)).toBe(false);
    expect(meetsMinimumAge('2008-11-20', 'STANDARD', on)).toBe(true);
  });

  it('refuses 20 and accepts 21 on an XL boat', () => {
    expect(meetsMinimumAge('2006-01-01', 'XL', on)).toBe(false);
    expect(meetsMinimumAge('2005-11-20', 'XL', on)).toBe(true);
  });

  it('accepts 5 and refuses 6 passengers on Standard; 10 / 11 on XL', () => {
    expect(fitsCapacity('STANDARD', 5)).toBe(true);
    expect(fitsCapacity('STANDARD', 6)).toBe(false);
    expect(fitsCapacity('XL', 10)).toBe(true);
    expect(fitsCapacity('XL', 11)).toBe(false);
  });
});

describe('low battery (BR7)', () => {
  it('warns the user at 20 % but not at 21 %', () => {
    expect(batteryAlert(21)).toBe('NONE');
    expect(batteryAlert(20)).toBe('USER');
  });

  it('alerts the operator at 10 % but not at 11 %', () => {
    expect(batteryAlert(11)).toBe('USER');
    expect(batteryAlert(10)).toBe('OPERATOR');
  });
});

describe('berths and nearest dock (BR14)', () => {
  it('counts free berths in service', () => {
    expect(freeBerths(dock({ berthsInService: 2 }), [boat(), boat({ id: 'b2' })])).toBe(0);
    expect(freeBerths(dock({ berthsInService: 3 }), [boat()])).toBe(2);
  });

  it('skips a closer dock that is full', () => {
    const full = dock({ id: 'near', x: 10, y: 0, berthsInService: 1 });
    const free = dock({ id: 'far', x: 100, y: 0 });
    const boats = [boat({ dockId: 'near' })];
    expect(nearestDockWithFreeBerth({ x: 0, y: 0 }, [full, free], boats)?.id).toBe('far');
  });

  it('returns null when every dock is full', () => {
    const full = dock({ berthsInService: 1 });
    expect(nearestDockWithFreeBerth({ x: 0, y: 0 }, [full], [boat()])).toBeNull();
  });
});
