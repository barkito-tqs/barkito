import { useSyncExternalStore } from 'react';
import { holdFee, tripCharges } from '../domain/pricing';
import {
  RULES,
  batteryAlert,
  canReserveAt,
  canUnlockAt,
  fitsCapacity,
  freeBerths,
  isBookable,
  meetsMinimumAge,
} from '../domain/rules';
import type {
  Boat,
  BoatType,
  Dock,
  Reservation,
  Technician,
  Ticket,
  TicketType,
  Trip,
  User,
} from '../domain/types';
import { BOATS, DOCKS, TECHNICIANS, USERS, seedTickets, seedTrips } from './mockData';

/**
 * In-memory stand-in for the REST API and the WebSocket push (Product Specification 4.3).
 * Every action mirrors a future endpoint and throws ApiError with the same status code,
 * so the screens can be wired to the real backend without changing their error handling.
 */

export class ApiError extends Error {
  constructor(
    readonly status: 400 | 403 | 404 | 409,
    message: string,
  ) {
    super(message);
  }
}

export type ProblemCategory = 'ENGINE' | 'HULL' | 'BATTERY' | 'OTHER';

export interface Notice {
  id: number;
  audience: 'USER' | 'OPERATOR';
  tone: 'info' | 'warning' | 'danger';
  text: string;
  at: number;
}

export interface Settings {
  maxTripMinutes: number;
  opensAt: string;
  closesAt: string;
  minAge: Record<BoatType, number>;
  slaHours: Record<TicketType, number>;
}

export interface State {
  users: User[];
  docks: Dock[];
  boats: Boat[];
  reservations: Reservation[];
  trips: Trip[];
  tickets: Ticket[];
  technicians: Technician[];
  settings: Settings;
  notices: Notice[];
  /** Expired reservations per user, for BR9. */
  expiredReservations: Record<string, number[]>;
  /** Alerts already sent per trip, so each BR7 alert goes out once. */
  sentAlerts: Record<string, string[]>;
}

const now = Date.now();

let state: State = {
  users: USERS,
  docks: DOCKS,
  boats: BOATS,
  reservations: [],
  trips: seedTrips(now),
  tickets: seedTickets(now),
  technicians: TECHNICIANS,
  settings: {
    maxTripMinutes: RULES.maxTripMinutes,
    opensAt: '08:00',
    closesAt: '22:00',
    minAge: { ...RULES.minAge },
    slaHours: { TOW: 2, CHARGE: 2, RETURN: 4, REPAIR: 24 },
  },
  notices: [],
  expiredReservations: {},
  sentAlerts: {},
};

const listeners = new Set<() => void>();

function set(next: Partial<State>) {
  state = { ...state, ...next };
  listeners.forEach((l) => l());
}

export function getState(): State {
  return state;
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useStore<T>(selector: (s: State) => T): T {
  return useSyncExternalStore(subscribe, () => selector(state));
}

let noticeId = 0;
function notify(audience: Notice['audience'], tone: Notice['tone'], text: string): Notice {
  const notice = { id: ++noticeId, audience, tone, text, at: Date.now() };
  return notice;
}

export function dismissNotice(id: number) {
  set({ notices: state.notices.filter((n) => n.id !== id) });
}

function findBoat(boatId: string): Boat {
  const boat = state.boats.find((b) => b.id === boatId);
  if (!boat) throw new ApiError(404, 'This boat does not exist.');
  return boat;
}

function findUser(userId: string): User {
  const user = state.users.find((u) => u.id === userId);
  if (!user) throw new ApiError(404, 'Unknown account.');
  return user;
}

function patchBoat(boatId: string, patch: Partial<Boat>): Boat[] {
  return state.boats.map((b) => (b.id === boatId ? { ...b, ...patch } : b));
}

export function activeReservationOf(userId: string): Reservation | undefined {
  return state.reservations.find((r) => r.userId === userId);
}

export function activeTripOf(userId: string): Trip | undefined {
  return state.trips.find((t) => t.userId === userId && t.endedAt === null);
}

/** BR2, BR3: checks shared by reserving and unlocking. */
function checkAccount(user: User) {
  if (!user.hasPaymentMethod) {
    throw new ApiError(
      409,
      'Add a payment method in your wallet before you reserve or unlock a boat.',
    );
  }
  if (user.debtCents > 0) {
    throw new ApiError(
      409,
      'Settle your pending debt in your wallet before you reserve or unlock a boat.',
    );
  }
  if (activeReservationOf(user.id) || activeTripOf(user.id)) {
    throw new ApiError(409, 'You already have an active boat. End it before you take another one.');
  }
}

function checkAge(user: User, type: BoatType) {
  if (!meetsMinimumAge(user.birthDate, type, new Date())) {
    const min = state.settings.minAge[type];
    throw new ApiError(
      409,
      `${type === 'XL' ? 'XL' : 'Standard'} boats need a skipper aged ${min} or over.`,
    );
  }
}

const DAY_MS = 24 * 60 * 60 * 1000;
const MAX_EXPIRED_PER_DAY = 2;

/** US-10: reserve a boat for up to 15 minutes. */
export function reserve(userId: string, boatId: string): Reservation {
  const user = findUser(userId);
  const boat = findBoat(boatId);
  checkAccount(user);
  const recentExpired = (state.expiredReservations[userId] ?? []).filter(
    (t) => Date.now() - t < DAY_MS,
  );
  if (recentExpired.length >= MAX_EXPIRED_PER_DAY) {
    throw new ApiError(
      409,
      'Two reservations expired in the last 24 hours, so reservations are blocked until midnight. You can still unlock a boat at the dock.',
    );
  }
  if (!canReserveAt(new Date())) {
    throw new ApiError(
      409,
      `Reservations are closed. You can still unlock a boat at the dock until ${state.settings.closesAt}.`,
    );
  }
  if (!isBookable(boat)) {
    throw new ApiError(
      409,
      'This boat was just taken or its battery is too low. Pick another boat.',
    );
  }
  checkAge(user, boat.type);
  const reservation: Reservation = { id: `R-${Date.now()}`, boatId, userId, startedAt: Date.now() };
  set({
    reservations: [...state.reservations, reservation],
    boats: patchBoat(boatId, { state: 'RESERVED', reservedBy: userId }),
  });
  return reservation;
}

/** US-11: cancelling pays the started minutes of hold. Returns the fee in cents. */
export function cancelReservation(userId: string): number {
  const reservation = activeReservationOf(userId);
  if (!reservation) throw new ApiError(404, 'You have no active reservation.');
  const boat = findBoat(reservation.boatId);
  const fee = holdFee(boat.type, (Date.now() - reservation.startedAt) / 1000);
  set({
    reservations: state.reservations.filter((r) => r.id !== reservation.id),
    boats: patchBoat(boat.id, { state: 'AVAILABLE', reservedBy: null }),
  });
  return fee;
}

function expireReservations() {
  const holdMs = RULES.holdMinutes * 60_000;
  const expired = state.reservations.filter((r) => Date.now() - r.startedAt >= holdMs);
  if (expired.length === 0) return;
  let boats = state.boats;
  const expiredReservations = { ...state.expiredReservations };
  const notices = [...state.notices];
  for (const r of expired) {
    boats = boats.map((b) =>
      b.id === r.boatId ? { ...b, state: 'AVAILABLE', reservedBy: null } : b,
    );
    expiredReservations[r.userId] = [...(expiredReservations[r.userId] ?? []), Date.now()];
    const boat = state.boats.find((b) => b.id === r.boatId);
    notices.push(
      notify(
        'USER',
        'warning',
        `Your reservation of ${boat?.code} expired. The full hold fee was charged.`,
      ),
    );
  }
  set({
    reservations: state.reservations.filter((r) => !expired.includes(r)),
    boats,
    expiredReservations,
    notices,
  });
}

/** US-12: unlock a boat by scanning its QR code or typing the code printed below it. */
export function unlock(userId: string, code: string, passengers: number): Trip {
  const user = findUser(userId);
  const boat = state.boats.find(
    (b) => b.unlockCode === code.trim() || b.code === code.trim().toUpperCase(),
  );
  if (!boat)
    throw new ApiError(
      404,
      'This code does not match any boat. Check the code below the QR code and try again.',
    );

  const reservation = activeReservationOf(userId);
  const ownReservation = reservation?.boatId === boat.id;
  if (boat.state === 'RESERVED' && !ownReservation) {
    throw new ApiError(
      409,
      `${boat.code} is reserved by someone else and cannot be unlocked. Pick another boat at this dock.`,
    );
  }
  if (reservation && !ownReservation) {
    throw new ApiError(
      409,
      'You have a reservation for another boat. Cancel it first, or go to the boat you reserved.',
    );
  }
  if (!ownReservation) {
    checkAccount(user);
    if (!isBookable(boat)) {
      throw new ApiError(
        409,
        `${boat.code} is not available right now. Pick another boat at this dock.`,
      );
    }
  }
  if (!canUnlockAt(new Date())) {
    throw new ApiError(
      409,
      `Boats can be unlocked between ${state.settings.opensAt} and ${state.settings.closesAt}.`,
    );
  }
  checkAge(user, boat.type);
  if (!fitsCapacity(boat.type, passengers)) {
    throw new ApiError(
      409,
      `${boat.type === 'XL' ? 'An XL' : 'A Standard'} boat carries up to ${RULES.capacity[boat.type]} people.`,
    );
  }

  const trip: Trip = {
    id: `TR-${Date.now()}`,
    boatId: boat.id,
    userId,
    startedAt: Date.now(),
    endedAt: null,
    passengers,
    holdSeconds: reservation && ownReservation ? (Date.now() - reservation.startedAt) / 1000 : 0,
    fromDockId: boat.dockId ?? '',
    toDockId: null,
    totalCents: null,
  };
  set({
    trips: [trip, ...state.trips],
    reservations: state.reservations.filter((r) => r !== reservation || !ownReservation),
    boats: patchBoat(boat.id, {
      state: 'IN_USE',
      reservedBy: null,
      dockId: null,
      berth: null,
      pluggedIn: false,
    }),
  });
  return trip;
}

/** US-13 + US-38: end the trip at a dock with a free berth, optionally reporting a problem. */
export function endTrip(
  userId: string,
  dockId: string,
  pluggedIn: boolean,
  problem?: { category: ProblemCategory; note: string },
): Trip {
  const trip = activeTripOf(userId);
  if (!trip) throw new ApiError(404, 'You have no trip in progress.');
  const dock = state.docks.find((d) => d.id === dockId);
  if (!dock) throw new ApiError(404, 'Unknown dock.');
  if (freeBerths(dock, state.boats) === 0) {
    throw new ApiError(409, `${dock.name} has no free berth. Go to another dock to end your trip.`);
  }
  const boat = findBoat(trip.boatId);
  const seconds = (Date.now() - trip.startedAt) / 1000;
  const charges = tripCharges(boat.type, seconds, trip.holdSeconds, state.settings.maxTripMinutes);
  const usedBerths = state.boats.filter((b) => b.dockId === dockId).map((b) => b.berth);
  const berth =
    Array.from({ length: dock.berthsInService }, (_, i) => i + 1).find(
      (n) => !usedBerths.includes(n),
    ) ?? 1;

  // BR20: a reported problem takes priority, then the charger, then the battery.
  let next: Boat['state'];
  if (problem) next = 'REPAIR_REQUIRED';
  else if (pluggedIn) next = boat.battery >= RULES.minBattery ? 'AVAILABLE' : 'CHARGING';
  else next = boat.battery >= RULES.minBattery ? 'AVAILABLE' : 'CHARGE_REQUIRED';

  const ended: Trip = { ...trip, endedAt: Date.now(), toDockId: dockId, totalCents: charges.total };
  let tickets = state.tickets;
  if (next === 'REPAIR_REQUIRED' || next === 'CHARGE_REQUIRED') {
    tickets = [
      ...tickets,
      openTicket(next === 'REPAIR_REQUIRED' ? 'REPAIR' : 'CHARGE', boat.id, dockId, problem?.note),
    ];
  }
  set({
    trips: state.trips.map((t) => (t.id === trip.id ? ended : t)),
    boats: patchBoat(boat.id, { state: next, dockId, berth, pluggedIn }),
    tickets,
  });
  return ended;
}

/** US-21 / US-39: assign to the technician who covers the dock, else the one with the fewest open tickets. */
function openTicket(
  type: TicketType,
  boatId: string,
  dockId: string | null,
  note?: string,
): Ticket {
  const covering = state.technicians.find((t) => dockId && t.dockIds.includes(dockId));
  const openCount = (id: string) =>
    state.tickets.filter((t) => t.technicianId === id && !t.closedAt).length;
  const fallback = [...state.technicians].sort((a, b) => openCount(a.id) - openCount(b.id))[0];
  const technician = covering ?? fallback;
  const openedAt = Date.now();
  return {
    id: `T-${Math.floor(openedAt / 1000) % 100000}`,
    type,
    boatId,
    technicianId: technician.id,
    openedAt,
    dueAt: openedAt + state.settings.slaHours[type] * 60 * 60 * 1000,
    closedAt: null,
    notes: note ? [`Reported by the user: ${note}`] : [],
  };
}

// ---------- Operator (US-03 to US-06, US-23, US-39) ----------

export function registerBoat(code: string, type: BoatType, dockId: string): Boat {
  const normalised = code.trim().toUpperCase();
  if (!/^B-\d{2,3}$/.test(normalised))
    throw new ApiError(400, 'Use the format B-00, for example B-16.');
  if (state.boats.some((b) => b.code === normalised))
    throw new ApiError(409, `${normalised} is already registered.`);
  if (!state.docks.some((d) => d.id === dockId))
    throw new ApiError(404, 'Pick an existing home dock.');
  const unlockCode = String(Math.floor(100000 + Math.random() * 900000));
  const boat: Boat = {
    id: normalised,
    code: normalised,
    type,
    state: 'INACTIVE',
    battery: 100,
    dockId,
    berth: null,
    pluggedIn: true,
    unlockCode,
    reservedBy: null,
  };
  set({ boats: [...state.boats, boat] });
  return boat;
}

export function registerDock(name: string, district: string, berths: number): Dock {
  if (!name.trim()) throw new ApiError(400, 'Give the dock a name.');
  if (berths < 1) throw new ApiError(400, 'A dock needs at least 1 berth.');
  const dock: Dock = {
    id: `d-${Date.now()}`,
    name: name.trim(),
    district: district.trim() || '—',
    x: 120 + Math.random() * 760,
    y: 80 + Math.random() * 440,
    berths,
    berthsInService: berths,
  };
  set({ docks: [...state.docks, dock] });
  return dock;
}

/** US-05 / BR19: activation needs the boat moored and plugged in. */
export function activateBoat(boatId: string) {
  const boat = findBoat(boatId);
  if (boat.state !== 'INACTIVE') throw new ApiError(409, 'Only an inactive boat can be activated.');
  if (!boat.dockId || !boat.pluggedIn) {
    throw new ApiError(
      409,
      `${boat.code} must be moored in a berth and plugged in before it is activated.`,
    );
  }
  set({
    boats: patchBoat(boatId, {
      state: boat.battery >= RULES.minBattery ? 'AVAILABLE' : 'CHARGING',
    }),
  });
}

export function deactivateBoat(boatId: string) {
  const boat = findBoat(boatId);
  if (boat.state === 'IN_USE' || boat.state === 'RESERVED') {
    throw new ApiError(
      409,
      `${boat.code} is ${boat.state === 'IN_USE' ? 'on a trip' : 'reserved'}. Deactivate it once it is back.`,
    );
  }
  if (boat.state !== 'AVAILABLE' && boat.state !== 'CHARGING') {
    throw new ApiError(409, 'Only an available or charging boat can be deactivated.');
  }
  set({ boats: patchBoat(boatId, { state: 'INACTIVE' }) });
}

const RETIRABLE: Boat['state'][] = [
  'INACTIVE',
  'REPAIR_REQUIRED',
  'TOW_REQUIRED',
  'RETURN_REQUIRED',
];

export function retireBoat(boatId: string) {
  const boat = findBoat(boatId);
  if (!RETIRABLE.includes(boat.state)) {
    throw new ApiError(
      409,
      `${boat.code} cannot be retired while ${boat.state.toLowerCase().replace('_', ' ')}.`,
    );
  }
  set({ boats: patchBoat(boatId, { state: 'RETIRED', dockId: null, berth: null }) });
}

export function reassignTicket(ticketId: string, technicianId: string) {
  set({ tickets: state.tickets.map((t) => (t.id === ticketId ? { ...t, technicianId } : t)) });
}

export function setTechnicianDocks(technicianId: string, dockIds: string[]) {
  set({
    technicians: state.technicians.map((t) => (t.id === technicianId ? { ...t, dockIds } : t)),
  });
}

// ---------- Technician (US-22) ----------

export function addTicketNote(ticketId: string, note: string) {
  if (!note.trim()) return;
  set({
    tickets: state.tickets.map((t) =>
      t.id === ticketId ? { ...t, notes: [...t.notes, note.trim()] } : t,
    ),
  });
}

export function closeTicket(ticketId: string) {
  const ticket = state.tickets.find((t) => t.id === ticketId);
  if (!ticket) throw new ApiError(404, 'Unknown ticket.');
  const boat = findBoat(ticket.boatId);
  // BR20 after a closed ticket: plugged in → CHARGING or AVAILABLE.
  const nextState: Boat['state'] = boat.battery >= RULES.minBattery ? 'AVAILABLE' : 'CHARGING';
  const dockId =
    boat.dockId ?? state.technicians.find((t) => t.id === ticket.technicianId)?.dockIds[0] ?? null;
  set({
    tickets: state.tickets.map((t) => (t.id === ticketId ? { ...t, closedAt: Date.now() } : t)),
    boats: patchBoat(boat.id, {
      state: nextState,
      pluggedIn: true,
      dockId,
      berth: boat.berth ?? 1,
    }),
  });
}

// ---------- Client manager (US-26 to US-28) ----------

export function updateSettings(next: Settings) {
  if (next.maxTripMinutes < 15)
    throw new ApiError(400, 'The maximum trip duration must be at least 15 minutes.');
  if (next.opensAt >= next.closesAt)
    throw new ApiError(400, 'Closing time must be after opening time.');
  set({ settings: next });
}

export function setBerthsInService(dockId: string, berthsInService: number) {
  const dock = state.docks.find((d) => d.id === dockId);
  if (!dock) throw new ApiError(404, 'Unknown dock.');
  if (berthsInService < 0 || berthsInService > dock.berths) {
    throw new ApiError(400, `${dock.name} has ${dock.berths} physical berths.`);
  }
  set({ docks: state.docks.map((d) => (d.id === dockId ? { ...d, berthsInService } : d)) });
}

// ---------- Telemetry simulator (stands in for MQTT + WebSocket push) ----------

const TICK_MS = 4000;
/** Battery drained per tick on a trip; accelerated so the BR7 alerts show up in a demo. */
const DRAIN_PER_TICK = 1;
const CHARGE_PER_TICK = 2;

function tick() {
  expireReservations();
  const notices = [...state.notices];
  const sentAlerts = { ...state.sentAlerts };
  const boats = state.boats.map((b) => {
    if (b.state === 'IN_USE') {
      const battery = Math.max(0, b.battery - DRAIN_PER_TICK);
      const trip = state.trips.find((t) => t.boatId === b.id && t.endedAt === null);
      const alert = batteryAlert(battery);
      if (trip && alert !== 'NONE') {
        const sent = sentAlerts[trip.id] ?? [];
        if (!sent.includes('USER')) {
          notices.push(
            notify(
              'USER',
              'warning',
              `Battery at ${battery} %. Head to the nearest dock with a free berth.`,
            ),
          );
          sentAlerts[trip.id] = [...sent, 'USER'];
        }
        if (alert === 'OPERATOR' && !(sentAlerts[trip.id] ?? []).includes('OPERATOR')) {
          notices.push(
            notify('OPERATOR', 'danger', `${b.code} is on a trip with ${battery} % battery.`),
          );
          sentAlerts[trip.id] = [...(sentAlerts[trip.id] ?? []), 'OPERATOR'];
        }
      }
      return { ...b, battery };
    }
    if (b.state === 'CHARGING' && b.pluggedIn) {
      const battery = Math.min(100, b.battery + CHARGE_PER_TICK);
      return {
        ...b,
        battery,
        state: battery >= RULES.minBattery ? 'AVAILABLE' : 'CHARGING',
      } as Boat;
    }
    if (b.state === 'AVAILABLE' && b.pluggedIn && b.battery < 100) {
      return { ...b, battery: Math.min(100, b.battery + 1) };
    }
    return b;
  });
  set({ boats, notices, sentAlerts });
}

if (typeof window !== 'undefined' && import.meta.env.MODE !== 'test') {
  window.setInterval(tick, TICK_MS);
}
