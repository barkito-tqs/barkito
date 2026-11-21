import type { Boat, Dock, Technician, Ticket, Trip, User } from '../domain/types';

/** Seed data for the mock API, based on the personas and scenarios of the Product Specification (2.2). */

export const USERS: User[] = [
  {
    id: 'u-francisca',
    name: 'Francisca Borges',
    email: 'francisca@barkito.example',
    role: 'END_USER',
    birthDate: '1992-04-03',
    hasPaymentMethod: true,
    debtCents: 0,
  },
  {
    id: 'u-armando',
    name: 'Armando Teles',
    email: 'armando@barkito.example',
    role: 'OPERATOR',
    birthDate: '1981-02-11',
    hasPaymentMethod: false,
    debtCents: 0,
  },
  {
    id: 't-tiago',
    name: 'Tiago Ferreira',
    email: 'tiago@barkito.example',
    role: 'TECHNICIAN',
    birthDate: '1997-07-19',
    hasPaymentMethod: false,
    debtCents: 0,
  },
  {
    id: 'u-augusta',
    name: 'Augusta Jesus',
    email: 'augusta@rianova.example',
    role: 'CLIENT_MANAGER',
    birthDate: '1974-09-30',
    hasPaymentMethod: false,
    debtCents: 0,
  },
];

export const DOCKS: Dock[] = [
  { id: 'd-market', name: 'Old Market', district: 'Mercado', x: 190, y: 250, berths: 6, berthsInService: 6 },
  { id: 'd-waterfront', name: 'Waterfront', district: 'Marginal', x: 820, y: 470, berths: 8, berthsInService: 8 },
  { id: 'd-salt', name: 'Salt Quay', district: 'Salinas', x: 470, y: 120, berths: 4, berthsInService: 4 },
  { id: 'd-station', name: 'Station', district: 'Estação', x: 520, y: 360, berths: 6, berthsInService: 5 },
  { id: 'd-university', name: 'University', district: 'Campus', x: 860, y: 170, berths: 4, berthsInService: 4 },
  { id: 'd-arts', name: 'Arts District', district: 'Ribeira', x: 300, y: 500, berths: 4, berthsInService: 3 },
];

function boat(
  code: string,
  type: Boat['type'],
  state: Boat['state'],
  battery: number,
  dockId: string | null,
  berth: number | null,
  unlockCode: string,
  pluggedIn = true,
): Boat {
  return { id: code, code, type, state, battery, dockId, berth, pluggedIn, unlockCode, reservedBy: null };
}

export const BOATS: Boat[] = [
  boat('B-01', 'STANDARD', 'AVAILABLE', 86, 'd-market', 1, '482915'),
  boat('B-02', 'STANDARD', 'CHARGING', 24, 'd-market', 2, '730264'),
  boat('B-03', 'STANDARD', 'REPAIR_REQUIRED', 64, 'd-market', 3, '115873'),
  boat('B-04', 'XL', 'AVAILABLE', 72, 'd-waterfront', 1, '904417'),
  boat('B-05', 'STANDARD', 'AVAILABLE', 55, 'd-waterfront', 2, '263508'),
  boat('B-06', 'STANDARD', 'AVAILABLE', 31, 'd-waterfront', 3, '598146'),
  boat('B-07', 'STANDARD', 'REPAIR_REQUIRED', 70, 'd-station', 1, '371925'),
  boat('B-08', 'XL', 'AVAILABLE', 93, 'd-station', 2, '846203', true),
  boat('B-09', 'STANDARD', 'AVAILABLE', 29, 'd-salt', 1, '627491', false),
  boat('B-10', 'STANDARD', 'AVAILABLE', 64, 'd-university', 1, '159372'),
  boat('B-11', 'XL', 'AVAILABLE', 48, 'd-university', 2, '408836'),
  boat('B-12', 'STANDARD', 'CHARGE_REQUIRED', 18, 'd-waterfront', 4, '782640', false),
  boat('B-14', 'STANDARD', 'INACTIVE', 100, 'd-arts', 1, '336019'),
  boat('B-15', 'XL', 'AVAILABLE', 77, 'd-arts', 2, '917552'),
  boat('B-21', 'STANDARD', 'TOW_REQUIRED', 0, null, null, '245781', false),
];

export const TECHNICIANS: Technician[] = [
  { id: 't-tiago', name: 'Tiago Ferreira', dockIds: ['d-market', 'd-waterfront', 'd-arts'] },
  { id: 't-rui', name: 'Rui Matos', dockIds: ['d-station', 'd-salt', 'd-university'] },
];

const HOUR = 60 * 60 * 1000;

export function seedTickets(now: number): Ticket[] {
  return [
    { id: 'T-101', type: 'CHARGE', boatId: 'B-12', technicianId: 't-tiago', openedAt: now - 40 * 60_000, dueAt: now + 80 * 60_000, closedAt: null, notes: [] },
    { id: 'T-102', type: 'REPAIR', boatId: 'B-03', technicianId: 't-tiago', openedAt: now - 5 * HOUR, dueAt: now + 19 * HOUR, closedAt: null, notes: ['Water inside the hull reported by a user.'] },
    { id: 'T-103', type: 'REPAIR', boatId: 'B-07', technicianId: 't-rui', openedAt: now - 9 * HOUR, dueAt: now + 15 * HOUR, closedAt: null, notes: [] },
    { id: 'T-104', type: 'TOW', boatId: 'B-21', technicianId: 't-tiago', openedAt: now - 12 * 60_000, dueAt: now + 108 * 60_000, closedAt: null, notes: [] },
  ];
}

export function seedTrips(now: number): Trip[] {
  const day = 24 * HOUR;
  return [
    { id: 'TR-31', boatId: 'B-01', userId: 'u-francisca', startedAt: now - 2 * day, endedAt: now - 2 * day + 31 * 60_000, passengers: 1, holdSeconds: 6 * 60, fromDockId: 'd-market', toDockId: 'd-station', totalCents: 100 + 31 * 50 + 150 },
    { id: 'TR-27', boatId: 'B-04', userId: 'u-francisca', startedAt: now - 6 * day, endedAt: now - 6 * day + 100 * 60_000, passengers: 4, holdSeconds: 0, fromDockId: 'd-waterfront', toDockId: 'd-arts', totalCents: 200 + 90 * 100 + 10 * 150 },
  ];
}
