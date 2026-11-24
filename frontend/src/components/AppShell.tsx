import { AlertTriangle, Info, LogOut, Siren, X } from 'lucide-react';
import { Link, NavLink, Navigate, Outlet, useNavigate } from 'react-router-dom';
import { dismissNotice, useStore } from '../api/store';
import markOnDark from '../assets/barkito-mark-on-dark.png';
import mark from '../assets/barkito-mark.png';
import { ROLE_LABEL, useSession } from '../auth/session';
import { initials } from '../lib/format';
import { NAV_BY_ROLE } from './nav';

function Brand({ onDark = false }: { onDark?: boolean }) {
  return (
    <Link to="/" className="brand" translate="no">
      <img src={onDark ? markOnDark : mark} alt="" width={44} height={23} />
      BARKITO
    </Link>
  );
}

const TONE_ICON = { info: Info, warning: AlertTriangle, danger: Siren };

/** WebSocket notices for the signed-in role: BR7 low-battery warnings and alerts. */
function Notices() {
  const { user } = useSession();
  const notices = useStore((s) => s.notices);
  const audience =
    user?.role === 'END_USER' ? 'USER' : user?.role === 'OPERATOR' ? 'OPERATOR' : null;
  const visible = notices.filter((n) => n.audience === audience).slice(-3);
  return (
    <div className="toasts" aria-live="polite">
      {visible.map((n) => {
        const Icon = TONE_ICON[n.tone];
        return (
          <div key={n.id} className={`toast ${n.tone}`}>
            <Icon className="lead" aria-hidden="true" />
            <p>{n.text}</p>
            <button
              type="button"
              className="icon-btn"
              aria-label="Dismiss notification"
              onClick={() => dismissNotice(n.id)}
            >
              <X aria-hidden="true" />
            </button>
          </div>
        );
      })}
    </div>
  );
}

function AccountMenu() {
  const { user, signOut } = useSession();
  const navigate = useNavigate();
  if (!user) return null;
  return (
    <details className="account">
      <summary aria-label={`Account: ${user.name}`}>
        <span className="avatar" aria-hidden="true">
          {initials(user.name)}
        </span>
        <span className="name-full">{user.name.split(' ')[0]}</span>
      </summary>
      <div className="account-menu">
        <div className="who">
          <strong>{user.name}</strong>
          <span className="muted small">{ROLE_LABEL[user.role]}</span>
        </div>
        <button
          type="button"
          onClick={() => {
            signOut();
            navigate('/sign-in');
          }}
        >
          <LogOut aria-hidden="true" /> Sign Out
        </button>
      </div>
    </details>
  );
}

export function AppShell() {
  const { user } = useSession();
  if (!user) return <Navigate to="/sign-in" replace />;
  const items = NAV_BY_ROLE[user.role];

  return (
    <div className="shell">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <aside className="sidebar" aria-label="Main">
        <Brand onDark />
        <nav>
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to}>
              <Icon aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>
        <p className="sidebar-foot">{ROLE_LABEL[user.role]} · Ria Nova</p>
      </aside>

      <div>
        <header className="topbar">
          <Brand />
          <div className="spacer" />
          <AccountMenu />
        </header>
        <main id="main" className="main" tabIndex={-1}>
          <div className="main-inner">
            <Outlet />
          </div>
        </main>
      </div>

      <nav className="tabbar" aria-label="Main">
        {items.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to}>
            <Icon aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>
      <Notices />
    </div>
  );
}
