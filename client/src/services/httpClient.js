import axios from 'axios'

/**
 * Plain axios instance with no interceptors.
 * Used exclusively by the Zustand auth store for login, register, and token
 * validation calls — keeping it interceptor-free breaks the circular dependency
 * that would arise if the store imported from api.js (which imports the store).
 *
 * Auth header is added manually in validateToken since it's the only store call
 * that needs it. Login and register endpoints are public (no Bearer token required).
 */
const httpClient = axios.create({
  baseURL: '/api',
})

export default httpClient
