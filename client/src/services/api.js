import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

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

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
}

export const sessionsApi = {
  upload: (formData) =>
    api.post('/sessions', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  list: () => api.get('/sessions'),
  get: (id) => api.get(`/sessions/${id}`),
}

export const chatApi = {
  send: (sessionId, message) =>
    api.post(`/sessions/${sessionId}/chat`, { message }),
  history: (sessionId) => api.get(`/sessions/${sessionId}/chat`),
}

export default api
