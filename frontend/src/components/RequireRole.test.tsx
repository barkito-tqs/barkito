import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SessionProvider } from '../auth/session';
import { RequireRole } from './RequireRole';

const SESSION_KEY = 'barkito.session.userId';
const ADMIN_ID = 'u-admin';
const OPERATOR_ID = 'u-armando';

function openOperatorAreaAs(userId: string) {
  window.localStorage.setItem(SESSION_KEY, userId);
  render(
    <MemoryRouter>
      <SessionProvider>
        <RequireRole role="OPERATOR">
          <p>Fleet page</p>
        </RequireRole>
      </SessionProvider>
    </MemoryRouter>,
  );
}

/** US-02: the web UI shows only the pages of the user's role. */
describe('RequireRole (US-02)', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('refuses an operator area to another role and points to the own area', () => {
    openOperatorAreaAs(ADMIN_ID);

    expect(screen.getByText('This Area Belongs to Another Role')).toBeInTheDocument();
    expect(screen.queryByText('Fleet page')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to Your Area' })).toHaveAttribute('href', '/admin');
  });

  it('shows the page to a user with the matching role', () => {
    openOperatorAreaAs(OPERATOR_ID);

    expect(screen.getByText('Fleet page')).toBeInTheDocument();
    expect(screen.queryByText('This Area Belongs to Another Role')).not.toBeInTheDocument();
  });
});
