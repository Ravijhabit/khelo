import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import SessionCard from '../SessionCard'

const baseSession = {
  id: 1,
  imagePath: null,
  overallScore: 7,
  strengths: '["Good posture"]',
  areasToImprove: '["Footwork"]',
  priorityFix: 'Keep your elbow up',
  drillSuggestion: 'Shadow drill x10',
  confidenceLevel: 'High',
  createdAt: '2026-05-30T10:00:00',
  sport: null,
}

const renderCard = (session) =>
  render(<MemoryRouter><SessionCard session={session} /></MemoryRouter>)

describe('SessionCard', () => {
  it('renders score badge', () => {
    renderCard(baseSession)
    expect(screen.getByText('7/10')).toBeInTheDocument()
  })

  it('renders sport badge when sport is present', () => {
    renderCard({ ...baseSession, sport: 'CRICKET' })
    expect(screen.getByText('Cricket')).toBeInTheDocument()
    expect(screen.getByText('🏏')).toBeInTheDocument()
  })

  it('renders badminton badge with correct emoji', () => {
    renderCard({ ...baseSession, sport: 'BADMINTON' })
    expect(screen.getByText('Badminton')).toBeInTheDocument()
    expect(screen.getByText('🏸')).toBeInTheDocument()
  })

  it('renders basketball badge with correct emoji', () => {
    renderCard({ ...baseSession, sport: 'BASKETBALL' })
    expect(screen.getByText('Basketball')).toBeInTheDocument()
    expect(screen.getByText('🏀')).toBeInTheDocument()
  })

  it('renders no sport badge when sport is null', () => {
    renderCard(baseSession)
    expect(screen.queryByText('Cricket')).not.toBeInTheDocument()
    expect(screen.queryByText('Badminton')).not.toBeInTheDocument()
    expect(screen.queryByText('Basketball')).not.toBeInTheDocument()
  })

  it('expands to show full report when expand button is clicked', async () => {
    renderCard(baseSession)
    const expandBtn = screen.getByTitle('View full report')
    await userEvent.click(expandBtn)
    // Strengths and drill suggestion only appear inside the expanded FeedbackCard
    expect(screen.getByText('Good posture')).toBeInTheDocument()
    expect(screen.getByText('Shadow drill x10')).toBeInTheDocument()
  })

  it('collapses report when expanded and clicked again', async () => {
    renderCard(baseSession)
    const expandBtn = screen.getByTitle('View full report')
    await userEvent.click(expandBtn)
    await userEvent.click(screen.getByTitle('Collapse'))
    expect(screen.queryByText('Good posture')).not.toBeInTheDocument()
  })

  it('applies green score color for score >= 8', () => {
    renderCard({ ...baseSession, overallScore: 9 })
    const badge = screen.getByText('9/10')
    expect(badge.className).toContain('green')
  })

  it('applies red score color for score < 5', () => {
    renderCard({ ...baseSession, overallScore: 3 })
    const badge = screen.getByText('3/10')
    expect(badge.className).toContain('red')
  })
})
