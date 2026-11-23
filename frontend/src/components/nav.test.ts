import { HOME_BY_ROLE } from '../auth/session';
import { ROLES } from '../domain/types';
import { NAV_BY_ROLE } from './nav';

/** US-02: every role has an area of its own, and signing in lands inside it. */
describe('role-based navigation (US-02)', () => {
  it('knows the five roles of the specification', () => {
    expect(ROLES).toEqual(['END_USER', 'OPERATOR', 'TECHNICIAN', 'CLIENT_MANAGER', 'ADMIN']);
  });

  it('gives every role at least one menu item', () => {
    for (const role of ROLES) {
      expect(NAV_BY_ROLE[role].length).toBeGreaterThan(0);
    }
  });

  it('sends every role home to a page of its own menu', () => {
    for (const role of ROLES) {
      expect(NAV_BY_ROLE[role].some((item) => item.to === HOME_BY_ROLE[role])).toBe(true);
    }
  });
});
