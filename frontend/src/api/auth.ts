import type { Role } from '../domain/types';

/**
 * The role of the signed-in persona, so API calls carry its dev token (US-02). Real sign-in with
 * the identity provider replaces this with the user's own token (US-01).
 */
let role: Role | null = null;

export function setSessionRole(next: Role | null) {
  role = next;
}

/** The Authorization header for the signed-in persona, or none without a session or a token. */
export function authorizationHeader(): Record<string, string> {
  const token = role ? __DEV_TOKENS__[role] : undefined;
  return token ? { Authorization: `Bearer ${token}` } : {};
}
