import '@testing-library/jest-dom'
import React from 'react'
import { vi } from 'vitest'

// Vitest's jsdom environment doesn't apply Vite's JSX transform the same way the
// dev server does, so components using the automatic runtime can't find React.
// Exposing it globally mirrors what the automatic runtime injects at build time.
globalThis.React = React

// jsdom does not implement URL.createObjectURL (used by UploadZone for image previews)
globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock-url')
globalThis.URL.revokeObjectURL = vi.fn()
