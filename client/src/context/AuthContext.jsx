import { useEffect } from 'react'
import { useAuthStore } from '../store/authStore'

/**
 * Triggers token validation once on app mount.
 * All state lives in the Zustand store — this component's only job is to
 * call validateToken() at the right point in the React lifecycle (after the
 * store has hydrated from localStorage but before any protected route renders).
 *
 * Kept as a named provider so App.jsx requires no structural changes.
 */
export function AuthProvider({ children }) {
  const validateToken = useAuthStore((s) => s.validateToken)

  useEffect(() => {
    validateToken()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return children
}

/**
 * Drop-in replacement for the old useAuth() hook.
 * Re-exports the Zustand store selector so every page and component that
 * imports { useAuth } from this file continues to work without modification.
 *
 * Returns the full store slice: { user, token, loading, login, register, logout, validateToken }
 */
export const useAuth = () => useAuthStore()
