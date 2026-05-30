import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from '../DashboardPage'

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    user: { name: 'Ravi Jha', sport: 'Cricket', position: 'Batsman', experienceLevel: 'Intermediate' },
  }),
}))

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useNavigate: () => vi.fn() }
})

const mockUpload = vi.fn()
vi.mock('../../services/api', () => ({
  sessionsApi: { upload: (...args) => mockUpload(...args) },
}))

// ── Helpers ───────────────────────────────────────────────────────────────────

const renderPage = () => render(<MemoryRouter><DashboardPage /></MemoryRouter>)

const fakeFile = () => new File(['pixels'], 'stance.jpg', { type: 'image/jpeg' })

const selectSport = async (label) => {
  await userEvent.click(screen.getByRole('radio', { name: label }))
}

const uploadFile = async (file) => {
  const input = document.querySelector('input[type="file"]')
  await userEvent.upload(input, file)
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('DashboardPage', () => {
  beforeEach(() => {
    mockUpload.mockReset()
  })

  it('renders the sport selector', () => {
    renderPage()
    expect(screen.getByRole('radio', { name: 'Cricket' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Badminton' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Basketball' })).toBeInTheDocument()
  })

  it('analyze button is disabled with neither sport nor file', () => {
    renderPage()
    expect(screen.getByRole('button', { name: /select a sport/i })).toBeDisabled()
  })

  it('analyze button stays disabled with only sport selected', async () => {
    renderPage()
    await selectSport('Cricket')
    expect(screen.getByRole('button', { name: /upload a photo/i })).toBeDisabled()
  })

  it('analyze button stays disabled with only file selected', async () => {
    renderPage()
    await uploadFile(fakeFile())
    // button label changes to 'Select a sport' when there's a file but no sport
    expect(screen.getByRole('button', { name: /select a sport/i })).toBeDisabled()
  })

  it('analyze button is enabled when both sport and file are selected', async () => {
    renderPage()
    await selectSport('Cricket')
    await uploadFile(fakeFile())
    expect(screen.getByRole('button', { name: /get ai coaching feedback/i })).not.toBeDisabled()
  })

  it('appends sport to FormData on upload', async () => {
    mockUpload.mockResolvedValue({
      data: { id: 42, feedback: { overallScore: 8, strengths: [], areasToImprove: [], priorityFix: 'x', drillSuggestion: 'y', confidenceLevel: 'High' } },
    })
    renderPage()
    await selectSport('Badminton')
    await uploadFile(fakeFile())
    await userEvent.click(screen.getByRole('button', { name: /get ai coaching feedback/i }))

    await waitFor(() => expect(mockUpload).toHaveBeenCalledOnce())
    const [formData] = mockUpload.mock.calls[0]
    expect(formData.get('sport')).toBe('BADMINTON')
    expect(formData.get('image')).toBeTruthy()
  })

  it('shows error message when upload fails', async () => {
    mockUpload.mockRejectedValue({ response: { data: { message: 'Gemini unavailable' } } })
    renderPage()
    await selectSport('Cricket')
    await uploadFile(fakeFile())
    await userEvent.click(screen.getByRole('button', { name: /get ai coaching feedback/i }))
    await waitFor(() => expect(screen.getByText('Gemini unavailable')).toBeInTheDocument())
  })

  it('resets all state on New Analysis', async () => {
    mockUpload.mockResolvedValue({
      data: { id: 1, feedback: { overallScore: 7, strengths: [], areasToImprove: [], priorityFix: 'p', drillSuggestion: 'd', confidenceLevel: 'Medium' } },
    })
    renderPage()
    await selectSport('Basketball')
    await uploadFile(fakeFile())
    await userEvent.click(screen.getByRole('button', { name: /get ai coaching feedback/i }))
    await waitFor(() => screen.getByText('New Analysis'))
    await userEvent.click(screen.getByText('New Analysis'))

    // Sport selector should be back, no result shown
    expect(screen.getByRole('radio', { name: 'Cricket' })).toHaveAttribute('aria-checked', 'false')
    expect(screen.queryByText('Coaching Report')).not.toBeInTheDocument()
  })
})
