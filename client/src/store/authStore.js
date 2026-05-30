import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import httpClient from '../services/httpClient'

/**
 * Central auth store — single source of truth for the authenticated player's
 * identity, JWT token, and loading state.
 *
 * Persistence strategy:
 *   - `persist` middleware syncs `token` and `user` to localStorage under the
 *     key "ghost-coach-auth". Zustand handles all reads/writes — no manual
 *     localStorage.getItem/setItem calls exist anywhere in the app.
 *   - `loading` is intentionally excluded from persistence (via `partialize`)
 *     because it always starts as true on page load and is resolved by
 *     validateToken(). Persisting it would leave stale false values.
 *   - localStorage hydration is synchronous, so `token` and `user` are available
 *     on first render without a flash of unauthenticated UI.
 */
export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      /** True while validateToken() is in flight — blocks ProtectedRoute rendering. */
      loading: true,

      /**
       * Authenticates with email + password and stores the returned token + user.
       * Uses httpClient (no interceptors) because login is a public endpoint.
       *
       * @returns {Object} the authenticated user object
       */
      login: async (email, password) => {
        const res = await httpClient.post('/auth/login', { email, password })
        set({ token: res.data.token, user: res.data.user })
        return res.data.user
      },

      /**
       * Creates a new account and immediately populates auth state.
       * Registration auto-logs the player in — no separate login step.
       *
       * @returns {Object} the newly created user object
       */
      register: async (data) => {
        const res = await httpClient.post('/auth/register', data)
        set({ token: res.data.token, user: res.data.user })
        return res.data.user
      },

      /**
       * Clears all auth state from both the store and localStorage.
       * Zustand's persist middleware propagates the null values to localStorage
       * automatically — no manual removeItem calls needed.
       */
      logout: () => set({ token: null, user: null }),

      /**
       * Verifies the stored token with the backend on every app load.
       * Guards against tokens that expired or were invalidated server-side since
       * the last visit. The Bearer header is added manually here because httpClient
       * has no interceptors (avoids the circular dependency with api.js).
       *
       * Always sets loading: false regardless of outcome so ProtectedRoute
       * can make a routing decision.
       */
      validateToken: async () => {
        const { token } = get()
        if (!token) {
          set({ loading: false })
          return
        }
        try {
          const res = await httpClient.get('/auth/me', {
            headers: { Authorization: `Bearer ${token}` },
          })
          set({ user: res.data, loading: false })
        } catch {
          // Token is invalid or expired — wipe state so the user is redirected to login
          set({ token: null, user: null, loading: false })
        }
      },
    }),
    {
      name: 'ghost-coach-auth',
      // Only persist identity data — loading is always recomputed on mount
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
)
