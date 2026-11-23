export type BoatType = 'STANDARD' | 'XL';

/** Boat lifecycle states (Product Specification 3.3). */
export type BoatState =
  | 'INACTIVE'
  | 'CHARGING'
  | 'AVAILABLE'
  | 'RESERVED'
  | 'IN_USE'
  | 'CHARGE_REQUIRED'
  | 'REPAIR_REQUIRED'
  | 'TOW_REQUIRED'
  | 'RETURN_REQUIRED'
  | 'RETIRED';

/** The five roles of the specification (US-02). */
export const ROLES = ['END_USER', 'OPERATOR', 'TECHNICIAN', 'CLIENT_MANAGER', 'ADMIN'] as const;

export type Role = (typeof ROLES)[number];

export interface Dock {
  id: string;
  name: string;
  district: string;
  latitude: number;
  longitude: number;
  /** Position on the canal map, in map units (0–1000 × 0–600), from the latitude and longitude. */
  x: number;
  y: number;
  berths: number;
  berthsInService: number;
}

export interface Boat {
  id: string;
  code: string;
  type: BoatType;
  state: BoatState;
  battery: number;
  dockId: string | null;
  berth: number | null;
  pluggedIn: boolean;
  /** Code printed below the boat's QR code (US-12). */
  unlockCode: string;
  reservedBy: string | null;
}

export interface Reservation {
  id: string;
  boatId: string;
  userId: string;
  startedAt: number;
}

export interface Trip {
  id: string;
  boatId: string;
  userId: string;
  startedAt: number;
  endedAt: number | null;
  passengers: number;
  holdSeconds: number;
  fromDockId: string;
  toDockId: string | null;
  /** Charges in cents, set when the trip ends. */
  totalCents: number | null;
}

export type TicketType = 'CHARGE' | 'REPAIR' | 'TOW' | 'RETURN';

export interface Ticket {
  id: string;
  type: TicketType;
  boatId: string;
  technicianId: string;
  openedAt: number;
  dueAt: number;
  closedAt: number | null;
  notes: string[];
}

export interface Technician {
  id: string;
  name: string;
  dockIds: string[];
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  birthDate: string;
  hasPaymentMethod: boolean;
  debtCents: number;
}
