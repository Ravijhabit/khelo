import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import SportSelector, { SPORTS } from '../SportSelector'

describe('SportSelector', () => {
  it('renders all sport options', () => {
    render(<SportSelector selected={null} onSelect={() => {}} />)
    SPORTS.forEach(({ label }) => {
      expect(screen.getByRole('radio', { name: label })).toBeInTheDocument()
    })
  })

  it('calls onSelect with the sport id when a sport is clicked', async () => {
    const onSelect = vi.fn()
    render(<SportSelector selected={null} onSelect={onSelect} />)
    await userEvent.click(screen.getByRole('radio', { name: 'Cricket' }))
    expect(onSelect).toHaveBeenCalledOnce()
    expect(onSelect).toHaveBeenCalledWith('CRICKET')
  })

  it('marks the selected sport as checked', () => {
    render(<SportSelector selected="BADMINTON" onSelect={() => {}} />)
    expect(screen.getByRole('radio', { name: 'Badminton' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: 'Cricket' })).toHaveAttribute('aria-checked', 'false')
    expect(screen.getByRole('radio', { name: 'Basketball' })).toHaveAttribute('aria-checked', 'false')
  })

  it('does not call onSelect when disabled', async () => {
    const onSelect = vi.fn()
    render(<SportSelector selected={null} onSelect={onSelect} disabled />)
    const btn = screen.getByRole('radio', { name: 'Cricket' })
    expect(btn).toBeDisabled()
    await userEvent.click(btn)
    expect(onSelect).not.toHaveBeenCalled()
  })

  it('allows switching between sports', async () => {
    const onSelect = vi.fn()
    const { rerender } = render(<SportSelector selected={null} onSelect={onSelect} />)

    await userEvent.click(screen.getByRole('radio', { name: 'Basketball' }))
    expect(onSelect).toHaveBeenCalledWith('BASKETBALL')

    rerender(<SportSelector selected="BASKETBALL" onSelect={onSelect} />)
    expect(screen.getByRole('radio', { name: 'Basketball' })).toHaveAttribute('aria-checked', 'true')
  })
})
