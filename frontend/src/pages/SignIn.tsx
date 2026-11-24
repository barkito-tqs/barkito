import { ArrowRight } from 'lucide-react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useStore } from '../api/store';
import logo from '../assets/barkito-logo-dark.png';
import { HOME_BY_ROLE, ROLE_LABEL, useSession } from '../auth/session';
import type { Role } from '../domain/types';
import { initials } from '../lib/format';

const ROLE_BLURB: Record<Role, string> = {
  END_USER: 'Find, reserve and unlock boats; follow your trips.',
  OPERATOR: 'Run the fleet, docks, tickets and technicians.',
  TECHNICIAN: 'Work through your tickets by SLA deadline.',
  CLIENT_MANAGER: 'Set the rules of use and follow the KPIs.',
  ADMIN: 'Manage user accounts and roles; follow platform health.',
};

/**
 * Sign-in through the identity provider is US-01 (I3). Until then, pick one of the persona accounts
 * from the Product Specification to enter its area.
 */
export function SignIn() {
  const { user, signIn } = useSession();
  const users = useStore((s) => s.users);
  const navigate = useNavigate();
  if (user) return <Navigate to={HOME_BY_ROLE[user.role]} replace />;

  return (
    <div className="signin">
      <section className="signin-hero">
        <img src={logo} alt="Barkito" width={240} height={240} />
        <h1>Shared Electric Boats on the Canals of Ria Nova</h1>
        <p>
          Reserve a boat while you walk to the dock, unlock it with its QR code and leave it at any
          dock with a free berth.
        </p>
      </section>
      <section className="signin-panel" aria-labelledby="signin-title">
        <div className="signin-inner">
          <h2 id="signin-title">Sign In</h2>
          <p className="muted small">
            Sign-in with your Barkito account arrives in iteration 3. For now, choose an account to
            open its area.
          </p>
          <ul className="persona-list">
            {users.map((u) => (
              <li key={u.id}>
                <button
                  type="button"
                  className="persona"
                  onClick={() => {
                    signIn(u.id);
                    navigate(HOME_BY_ROLE[u.role]);
                  }}
                >
                  <span className="avatar" aria-hidden="true">
                    {initials(u.name)}
                  </span>
                  <span className="persona-text">
                    <strong>{u.name}</strong>
                    <span className="small muted">
                      {ROLE_LABEL[u.role]} · {ROLE_BLURB[u.role]}
                    </span>
                  </span>
                  <ArrowRight aria-hidden="true" />
                </button>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </div>
  );
}
