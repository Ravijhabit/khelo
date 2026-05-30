import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import httpClient from '../services/httpClient'

/**
 * Central auth store — single source of truth for the authenticated player's
 * identity, JWT token, active sport selection, and loading state.
 *
 * Persistence strategy:
 *   - `persist` middleware syncs `token`, `user`, and `activeSport` to localStorage.
 *   - `loading` is intentionally excluded — it always starts true on page load.
 *   - `activeSport` is persisted so the player's sport context survives a refresh.
 */
export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      /** The sport the player is currently training in. Null until explicitly set. */
      activeSport: null,
      /** True while validateToken() is in flight — blocks ProtectedRoute rendering. */
      loading: true,

      /**
       * Authenticates with email + password and stores the returned token + user.
       * Initialises activeSport to the player's first registered sport.
       */
      login: async (email, password) => {
        const res = await httpClient.post('/auth/login', { email, password })
        const user = res.data.user
        set({
          token: res.data.token,
          user,
          activeSport: user.sportProfiles?.[0]?.sport ?? null,
        })
        return user
      },

      /**
       * Creates a new account and immediately populates auth state.
       * Registration auto-logs the player in — no separate login step.
       */
      register: async (data) => {
        const res = await httpClient.post('/auth/register', data)
        const user = res.data.user
        set({
          token: res.data.token,
          user,
          activeSport: user.sportProfiles?.[0]?.sport ?? null,
        })
        return user
      },

      /**
       * Clears all auth state including the active sport selection.
       */
      logout: () => set({ token: null, user: null, activeSport: null }),

      /**
       * Switches the player's active sport context.
       * Only accepts values from the player's registered sports.
       *
       * @param {string} sport - display-name sport string (e.g. "Cricket")
       */
      setActiveSport: (sport) => {
        const { user } = get()
        if (user?.sportProfiles?.some((p) => p.sport === sport)) {
          set({ activeSport: sport })
        }
      },

      /**
       * Verifies the stored token with the backend on every app load.
       * Guards against tokens that expired or were invalidated server-side.
       */
      validateToken: async () => {
        const { token, user, activeSport } = get()
        if (!token) {
          set({ loading: false })
          return
        }
        try {
          const res = await httpClient.get('/auth/me', {
            headers: { Authorization: `Bearer ${token}` },
          })
          const freshUser = res.data
          // Re-validate activeSport in case the user's sport list changed
          const validActiveSport =
            activeSport && freshUser.sportProfiles?.some((p) => p.sport === activeSport)
              ? activeSport
              : freshUser.sportProfiles?.[0]?.sport ?? null
          set({ user: freshUser, activeSport: validActiveSport, loading: false })
        } catch {
          set({ token: null, user: null, activeSport: null, loading: false })
        }
      },
    }),
    {
      name: 'ghost-coach-auth',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        activeSport: state.activeSport,
      }),
    }
  )
)
