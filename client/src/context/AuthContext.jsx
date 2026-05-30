import { createContext, useContext, useState, useEffect } from 'react'
import { authApi } from '../services/api'

const AuthContext = createContext(null)

/**
 * Provides authentication state (current user, loading flag) and auth actions
 * (login, register, logout) to the entire component tree.
 *
 * On mount, validates any stored JWT with the /auth/me endpoint to restore session
 * across page refreshes without re-prompting the user to log in.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    // Initialise from localStorage synchronously to avoid a flash of unauthenticated UI.
    // This is treated as optimistic — the useEffect below validates the token.
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })
  const [loading, setLoading] = useState(true)

  /**
   * On mount: if a token exists, verify it with the backend and refresh the user object.
   * This catches tokens that expired or were revoked since the last page load.
   * If validation fails, the api.js 401 interceptor clears storage and redirects.
   */
  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      setLoading(false)
      return
    }
    authApi
      .me()
      .then((res) => {
        setUser(res.data)
        localStorage.setItem('user', JSON.stringify(res.data))
      })
      .catch(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  /**
   * Authenticates with email + password, stores the JWT and user profile,
   * and updates React state. Returns the user object so callers can redirect.
   */
  const login = async (email, password) => {
    const res = await authApi.login({ email, password })
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    setUser(res.data.user)
    return res.data.user
  }

  /**
   * Registers a new account and immediately logs the player in.
   * The API returns a token on successful registration, so no separate
   * login call is needed.
   */
  const register = async (data) => {
    const res = await authApi.register(data)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    setUser(res.data.user)
    return res.data.user
  }

  /**
   * Clears all auth state — both in-memory and in localStorage.
   * The caller is responsible for navigating to /login after calling this.
   */
  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

/** Convenience hook — throws a descriptive error if used outside AuthProvider. */
export const useAuth = () => useContext(AuthContext)
