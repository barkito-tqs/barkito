import {
  Anchor,
  BarChart3,
  History,
  ListChecks,
  MapPin,
  QrCode,
  Ship,
  ShieldCheck,
  SlidersHorizontal,
  Ticket,
  UserCog,
  Wallet,
  Wrench,
  type LucideIcon,
} from 'lucide-react';
import type { Role } from '../domain/types';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

/** Each role sees only its own area (US-02). */
export const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  END_USER: [
    { to: '/boats', label: 'Boats', icon: MapPin },
    { to: '/unlock', label: 'Unlock', icon: QrCode },
    { to: '/trip', label: 'My trip', icon: Ship },
    { to: '/trips', label: 'History', icon: History },
    { to: '/wallet', label: 'Wallet', icon: Wallet },
  ],
  OPERATOR: [
    { to: '/fleet', label: 'Fleet', icon: Ship },
    { to: '/docks', label: 'Docks', icon: Anchor },
    { to: '/tickets', label: 'Tickets', icon: Ticket },
    { to: '/technicians', label: 'Technicians', icon: UserCog },
  ],
  TECHNICIAN: [{ to: '/my-tickets', label: 'My tickets', icon: Wrench }],
  CLIENT_MANAGER: [
    { to: '/kpis', label: 'KPIs', icon: BarChart3 },
    { to: '/rules', label: 'Rules of use', icon: SlidersHorizontal },
    { to: '/berths', label: 'Berths', icon: ListChecks },
  ],
  ADMIN: [{ to: '/admin', label: 'Accounts', icon: ShieldCheck }],
};
