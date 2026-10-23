import { describe, it, expect, vi, beforeEach } from 'vitest';

const storage: Record<string, string> = {};
const mockLocalStorage = {
  getItem: (key: string) => storage[key] || null,
  setItem: (key: string, value: string) => { storage[key] = value; },
  removeItem: (key: string) => { delete storage[key]; },
  clear: () => { Object.keys(storage).forEach(k => delete storage[k]); }
};

const dispatchedEvents: any[] = [];
if (typeof window === 'undefined') {
  (global as any).window = {
    dispatchEvent: (evt: any) => {
      dispatchedEvents.push(evt);
      return true;
    }
  };
}
(global as any).localStorage = mockLocalStorage;

import api, { getSafeErrorMessage } from '../api';

describe('API Service and Safe Error Handling', () => {
  beforeEach(() => {
    mockLocalStorage.clear();
    dispatchedEvents.length = 0;
    vi.restoreAllMocks();
  });

  describe('getSafeErrorMessage', () => {
    it('returns custom fallback when error is null or undefined', () => {
      expect(getSafeErrorMessage(null)).toBe('An unexpected error occurred. Please try again.');
      expect(getSafeErrorMessage(undefined, 'Custom fallback')).toBe('Custom fallback');
    });

    it('returns user-friendly error from backend message when safe', () => {
      const error = {
        response: {
          data: { message: 'Invalid credentials provided' }
        }
      };
      expect(getSafeErrorMessage(error)).toBe('Invalid credentials provided');
    });

    it('suppresses internal exceptions and stack traces', () => {
      const errorWithException = {
        response: {
          data: {
            message: 'org.hibernate.exception.ConstraintViolationException: could not execute statement; at com.edupulse.service.AuthService.login'
          }
        }
      };
      expect(getSafeErrorMessage(errorWithException, 'Login failed')).toBe('Login failed');
    });

    it('suppresses raw PostgreSQL SQL errors', () => {
      const errorWithSql = {
        response: {
          data: {
            message: 'org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint'
          }
        }
      };
      expect(getSafeErrorMessage(errorWithSql)).toBe('An unexpected error occurred. Please try again.');
    });

    it('maps 401 Unauthorized to session expired message', () => {
      const error = { response: { status: 401 } };
      expect(getSafeErrorMessage(error)).toBe('Your session has expired. Please log in again.');
    });

    it('maps 403 Forbidden to permission message', () => {
      const error = { response: { status: 403 } };
      expect(getSafeErrorMessage(error)).toBe('You do not have permission to access this resource.');
    });

    it('maps 404 Not Found to record not found message', () => {
      const error = { response: { status: 404 } };
      expect(getSafeErrorMessage(error)).toBe('Requested academic record was not found.');
    });

    it('maps 429 Too Many Requests to rate limiting message', () => {
      const error = { response: { status: 429 } };
      expect(getSafeErrorMessage(error)).toBe('Too many requests. Please wait a moment before trying again.');
    });

    it('maps 500 Internal Server Error to server unavailable message', () => {
      const error = { response: { status: 500 } };
      expect(getSafeErrorMessage(error)).toBe('The server encountered an error. Please try again later.');
    });
  });

  describe('api interceptor auth invalidation', () => {
    it('dispatches auth-unauthorized event and clears storage on 401', async () => {
      mockLocalStorage.setItem('token', 'sample-jwt-token');
      mockLocalStorage.setItem('user', JSON.stringify({ id: 1, name: 'Student' }));

      const interceptor = (api.interceptors.response as any).handlers[0];
      const error401 = { response: { status: 401 } };

      try {
        await interceptor.rejected(error401);
      } catch (e) {
        // expected rejection
      }

      expect(mockLocalStorage.getItem('token')).toBeNull();
      expect(mockLocalStorage.getItem('user')).toBeNull();
      expect(dispatchedEvents.length).toBeGreaterThan(0);
      expect(dispatchedEvents[0].type).toBe('auth-unauthorized');
    });

    it('dispatches network-error event on 429 rate limit', async () => {
      const interceptor = (api.interceptors.response as any).handlers[0];
      const error429 = { response: { status: 429 } };

      try {
        await interceptor.rejected(error429);
      } catch (e) {
        // expected rejection
      }

      expect(dispatchedEvents.length).toBeGreaterThan(0);
      expect(dispatchedEvents[0].type).toBe('network-error');
      expect(dispatchedEvents[0].detail).toContain('Too many requests');
    });
  });
});
