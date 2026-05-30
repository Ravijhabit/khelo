import axios from 'axios'
import { useAuthStore } from '../store/authStore'

/**
 * Interceptor-equipped axios instance for all authenticated API calls.
 * No circular dependency: api.js imports the store; the store imports httpClient
 * (not api.js), so the dependency graph is a DAG.
 */
const api = axios.create({
  baseURL: '/api',
})

/**
 * Request interceptor: reads the JWT from the Zustand store via getState()
 * (the non-reactive, outside-component accessor) and attaches it as a Bearer token.
 * Every call to sessionsApi or chatApi gets the header automatically.
 */
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * Response interceptor: on 401, wipes the Zustand store (which also clears
 * localStorage via persist middleware) and redirects to /login.
 * window.location.href is used instead of React Router because this interceptor
 * runs outside of the React component tree.
 */
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

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
