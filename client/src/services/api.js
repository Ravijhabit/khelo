import axios from 'axios'

/**
 * Shared Axios instance scoped to /api.
 * Vite's dev server proxy (vite.config.js) forwards /api/* to localhost:8080,
 * so this works identically in development without CORS setup on the backend.
 */
const api = axios.create({
  baseURL: '/api',
})

/**
 * Request interceptor: automatically attaches the JWT from localStorage to every
 * outgoing request as a Bearer token. This means individual API calls never
 * need to manually pass the Authorization header.
 */
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * Response interceptor: handles expired or invalid JWT globally.
 * On 401, clears stored credentials and hard-redirects to /login.
 * Using window.location.href instead of React Router's navigate because
 * this interceptor runs outside of React's component tree.
 */
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

/** Authentication endpoints — no JWT required (permitted in SecurityConfig). */
export const authApi = {
  /** Creates a new player account. Returns { token, user }. */
  register: (data) => api.post('/auth/register', data),

  /** Authenticates with email + password. Returns { token, user }. */
  login: (data) => api.post('/auth/login', data),

  /** Fetches the current player's profile by validating the stored JWT. */
  me: () => api.get('/auth/me'),
}

/** Coaching session endpoints — all require a valid JWT. */
export const sessionsApi = {
  /**
   * Uploads an image and triggers Gemini analysis.
   * Must be sent as multipart/form-data with the file under the "image" field.
   * Returns { id, feedback: { overallScore, strengths, ... } }.
   */
  upload: (formData) =>
    api.post('/sessions', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  /** Returns all sessions for the current player, newest first. */
  list: () => api.get('/sessions'),

  /** Returns a single session's full report by ID. */
  get: (id) => api.get(`/sessions/${id}`),
}

/** Coaching chat endpoints — scoped to a specific session. */
export const chatApi = {
  /**
   * Sends a message to the AI coach and returns the assistant's reply.
   * The backend injects the session report + full conversation history into the prompt,
   * so the AI has context even though each HTTP call is stateless.
   */
  send: (sessionId, message) =>
    api.post(`/sessions/${sessionId}/chat`, { message }),

  /** Fetches the ordered message history for a session's chat thread. */
  history: (sessionId) => api.get(`/sessions/${sessionId}/chat`),
}

export default api
