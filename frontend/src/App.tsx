import { Compass } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { HOME_BY_ROLE, useSession } from './auth/session';
import { AppShell } from './components/AppShell';
import { RequireRole } from './components/RequireRole';
import { Empty } from './components/ui';
import type { Role } from './domain/types';
import { SignIn } from './pages/SignIn';
import { AdminHome } from './pages/admin/AdminHome';
import { Berths } from './pages/client/Berths';
import { Kpis } from './pages/client/Kpis';
import { Rules } from './pages/client/Rules';
import { BoatDetail } from './pages/operator/BoatDetail';
import { DockDetail } from './pages/operator/DockDetail';
import { Docks } from './pages/operator/Docks';
import { Fleet } from './pages/operator/Fleet';
import { Technicians } from './pages/operator/Technicians';
import { Tickets } from './pages/operator/Tickets';
import { MyTickets, TicketDetail } from './pages/technician/MyTickets';
import { FindBoats } from './pages/user/FindBoats';
import { MyTrip } from './pages/user/MyTrip';
import { Trips } from './pages/user/Trips';
import { Unlock } from './pages/user/Unlock';
import { Wallet } from './pages/user/Wallet';

function Home() {
  const { user } = useSession();
  return <Navigate to={user ? HOME_BY_ROLE[user.role] : '/sign-in'} replace />;
}

function NotFound() {
  return (
    <div className="card">
      <Empty icon={<Compass aria-hidden="true" />} title="This Page Does Not Exist">
        <Link className="btn btn-primary" to="/">
          Go Home
        </Link>
      </Empty>
    </div>
  );
}

const AREAS: { path: string; role: Role; element: ReactNode }[] = [
  { path: 'boats', role: 'END_USER', element: <FindBoats /> },
  { path: 'unlock', role: 'END_USER', element: <Unlock /> },
  { path: 'trip', role: 'END_USER', element: <MyTrip /> },
  { path: 'trips', role: 'END_USER', element: <Trips /> },
  { path: 'wallet', role: 'END_USER', element: <Wallet /> },
  { path: 'fleet', role: 'OPERATOR', element: <Fleet /> },
  { path: 'fleet/:boatId', role: 'OPERATOR', element: <BoatDetail /> },
  { path: 'docks', role: 'OPERATOR', element: <Docks /> },
  { path: 'docks/:dockId', role: 'OPERATOR', element: <DockDetail /> },
  { path: 'tickets', role: 'OPERATOR', element: <Tickets /> },
  { path: 'technicians', role: 'OPERATOR', element: <Technicians /> },
  { path: 'my-tickets', role: 'TECHNICIAN', element: <MyTickets /> },
  { path: 'my-tickets/:ticketId', role: 'TECHNICIAN', element: <TicketDetail /> },
  { path: 'kpis', role: 'CLIENT_MANAGER', element: <Kpis /> },
  { path: 'rules', role: 'CLIENT_MANAGER', element: <Rules /> },
  { path: 'berths', role: 'CLIENT_MANAGER', element: <Berths /> },
  { path: 'admin', role: 'ADMIN', element: <AdminHome /> },
];

export function App() {
  return (
    <Routes>
      <Route path="/sign-in" element={<SignIn />} />
      <Route path="/" element={<Home />} />
      <Route element={<AppShell />}>
        {AREAS.map(({ path, role, element }) => (
          <Route
            key={path}
            path={path}
            element={<RequireRole role={role}>{element}</RequireRole>}
          />
        ))}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
