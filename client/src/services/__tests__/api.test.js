import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock axios before importing api.js so the module picks up the mock
vi.mock('axios', () => {
  const instance = {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  }
  return { default: { create: () => instance, ...instance } }
})

// Mock the auth store — api.js reads token from it in the request interceptor
vi.mock('../../store/authStore', () => ({
  useAuthStore: { getState: () => ({ token: 'test-jwt', logout: vi.fn() }) },
}))

import { sessionsApi, sportsApi } from '../api'
import axios from 'axios'

const axiosInstance = axios.create()

describe('sportsApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls GET /sports', () => {
    axiosInstance.get.mockResolvedValue({ data: ['BADMINTON', 'CRICKET', 'BASKETBALL'] })
    sportsApi.list()
    expect(axiosInstance.get).toHaveBeenCalledWith('/sports')
  })
})

describe('sessionsApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls POST /sessions with multipart/form-data header', () => {
    const fd = new FormData()
    fd.append('image', new File(['x'], 'img.jpg', { type: 'image/jpeg' }))
    fd.append('sport', 'CRICKET')
    axiosInstance.post.mockResolvedValue({ data: {} })
    sessionsApi.upload(fd)
    expect(axiosInstance.post).toHaveBeenCalledWith(
      '/sessions',
      fd,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  })

  it('calls GET /sessions for list', () => {
    axiosInstance.get.mockResolvedValue({ data: [] })
    sessionsApi.list()
    expect(axiosInstance.get).toHaveBeenCalledWith('/sessions')
  })

  it('calls GET /sessions/:id for get', () => {
    axiosInstance.get.mockResolvedValue({ data: {} })
    sessionsApi.get(7)
    expect(axiosInstance.get).toHaveBeenCalledWith('/sessions/7')
  })
})
