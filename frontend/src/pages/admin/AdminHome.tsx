import { Users } from 'lucide-react';
import { Empty, PageHead } from '../../components/ui';

/** US-02: the platform admin's area. Account management arrives with a later story. */
export function AdminHome() {
  return (
    <>
      <PageHead
        eyebrow="Platform admin"
        title="Accounts"
        lead="Manage user accounts and roles; follow platform health."
      />
      <div className="card">
        <Empty icon={<Users aria-hidden="true" />} title="Account Management Is Coming">
          <p>Creating and managing accounts and roles arrives with a later story.</p>
        </Empty>
      </div>
    </>
  );
}
