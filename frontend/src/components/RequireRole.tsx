import { Ban } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { HOME_BY_ROLE, ROLE_LABEL, useUser } from '../auth/session';
import type { Role } from '../domain/types';
import { Empty } from './ui';

/** US-02: a page of another role is refused, as the API refuses it with 403. */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const user = useUser();
  if (user.role === role) return <>{children}</>;
  return (
    <div className="card">
      <Empty icon={<Ban aria-hidden="true" />} title="This Area Belongs to Another Role">
        <p>
          The {ROLE_LABEL[role].toLowerCase()} area is not available to your account ({ROLE_LABEL[user.role].toLowerCase()}).
        </p>
        <Link className="btn btn-primary" to={HOME_BY_ROLE[user.role]}>
          Go to Your Area
        </Link>
      </Empty>
    </div>
  );
}
