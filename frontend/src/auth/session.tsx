import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { getState } from '../api/store';
import type { Role, User } from '../domain/types';

/**
 * Development session. Sign-in through the identity provider (OpenID Connect) is US-01, planned for I3;
 * until then the sign-in page lets you pick one of the persona accounts, and the role decides which
 * area is shown (US-02).
 */

const STORAGE_KEY = 'barkito.session.userId';

interface Session {
  user: User | null;
  signIn: (userId: string) => void;
  signOut: () => void;
}

const SessionContext = createContext<Session | null>(null);

function readStoredUserId(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function writeStoredUserId(userId: string | null) {
  try {
    if (userId) window.localStorage.setItem(STORAGE_KEY, userId);
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be blocked (private mode); the session then lasts until reload.
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(readStoredUserId);

  const signIn = useCallback((id: string) => {
    writeStoredUserId(id);
    setUserId(id);
  }, []);

  const signOut = useCallback(() => {
    writeStoredUserId(null);
    setUserId(null);
  }, []);

  const value = useMemo<Session>(() => {
    const user = getState().users.find((u) => u.id === userId) ?? null;
    return { user, signIn, signOut };
  }, [userId, signIn, signOut]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): Session {
  const session = useContext(SessionContext);
  if (!session) throw new Error('useSession must be used inside SessionProvider');
  return session;
}

/** The signed-in user; only call it inside a route that requires a session. */
export function useUser(): User {
  const { user } = useSession();
  if (!user) throw new Error('No signed-in user');
  return user;
}

export const ROLE_LABEL: Record<Role, string> = {
  END_USER: 'End user',
  OPERATOR: 'Operator',
  TECHNICIAN: 'Field Technician',
  CLIENT_MANAGER: 'Client manager',
  ADMIN: 'Platform admin',
};

export const HOME_BY_ROLE: Record<Role, string> = {
  END_USER: '/boats',
  OPERATOR: '/fleet',
  TECHNICIAN: '/my-tickets',
  CLIENT_MANAGER: '/kpis',
  ADMIN: '/admin',
};
