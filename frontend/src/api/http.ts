/**
 * Calls the Barkito REST API under /api/v1 (proxied to the backend by Vite and nginx). Errors come
 * back as RFC 9457 Problem Details and are thrown as ApiError with their status and detail.
 */

import { authorizationHeader } from './auth';

export type ApiStatus = 400 | 401 | 403 | 404 | 409 | 422 | 500 | 503;

export class ApiError extends Error {
  constructor(
    readonly status: ApiStatus,
    message: string,
  ) {
    super(message);
  }
}

const BASE = '/api/v1';
const UNREACHABLE = 503;
const MESSAGES: Partial<Record<number, string>> = {
  400: 'Check the fields and try again.',
  401: 'Sign in to continue.',
  403: 'Your role cannot do this.',
  404: 'Not found.',
  422: 'The request breaks a business rule.',
};

async function problemOf(response: Response): Promise<ApiError> {
  let detail: string | undefined;
  try {
    const body = (await response.json()) as { detail?: string };
    detail = body.detail;
  } catch {
    // Not a Problem Details body; fall back to a generic message.
  }
  const message = detail ?? MESSAGES[response.status] ?? 'Something went wrong.';
  return new ApiError(response.status as ApiStatus, message);
}

async function request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers: {
        ...authorizationHeader(),
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(UNREACHABLE, 'The server cannot be reached.');
  }
  if (!response.ok) {
    throw await problemOf(response);
  }
  return (await response.json()) as T;
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body: unknown) => request<T>('POST', path, body),
};
