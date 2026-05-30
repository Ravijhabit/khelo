import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Dumbbell } from 'lucide-react'

const SPORTS = {
  Cricket: ['Batsman', 'Bowler', 'All-Rounder', 'Wicket Keeper'],
  Football: ['Goalkeeper', 'Defender', 'Midfielder', 'Forward'],
  Basketball: ['Point Guard', 'Shooting Guard', 'Small Forward', 'Power Forward', 'Center'],
  Badminton: ['Singles Player', 'Doubles Player', 'Mixed Doubles Player'],
}

const LEVELS = ['Beginner', 'Intermediate', 'Advanced']

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    sport: '',
    position: '',
    experienceLevel: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const set = (key, value) => {
    setForm((prev) => ({
      ...prev,
      [key]: value,
      ...(key === 'sport' ? { position: '' } : {}),
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.sport || !form.position || !form.experienceLevel) {
      setError('Please complete all fields.')
      return
    }
    setLoading(true)
    try {
      await register(form)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="flex flex-col items-center mb-8">
          <div className="flex items-center gap-2 mb-2">
            <Dumbbell className="w-8 h-8 text-indigo-400" />
            <span className="text-2xl font-bold text-white">
              Ghost<span className="text-indigo-400">Coach</span>
            </span>
          </div>
          <p className="text-gray-400 text-sm">Set up your player profile</p>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8">
          <h1 className="text-xl font-semibold text-white mb-6">Create account</h1>

          {error && (
            <div className="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Full Name">
              <input
                type="text"
                required
                value={form.name}
                onChange={(e) => set('name', e.target.value)}
                className="input"
                placeholder="Your name"
              />
            </Field>

            <Field label="Email">
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                className="input"
                placeholder="you@example.com"
              />
            </Field>

            <Field label="Password">
              <input
                type="password"
                required
                minLength={6}
                value={form.password}
                onChange={(e) => set('password', e.target.value)}
                className="input"
                placeholder="Min 6 characters"
              />
            </Field>

            <Field label="Sport">
              <div className="grid grid-cols-2 gap-2">
                {Object.keys(SPORTS).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => set('sport', s)}
                    className={`py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
                      form.sport === s
                        ? 'bg-indigo-600 border-indigo-500 text-white'
                        : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-gray-500'
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </Field>

            {form.sport && (
              <Field label="Position / Role">
                <select
                  required
                  value={form.position}
                  onChange={(e) => set('position', e.target.value)}
                  className="input"
                >
                  <option value="">Select position</option>
                  {SPORTS[form.sport].map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </Field>
            )}

            <Field label="Experience Level">
              <div className="grid grid-cols-3 gap-2">
                {LEVELS.map((l) => (
                  <button
                    key={l}
                    type="button"
                    onClick={() => set('experienceLevel', l)}
                    className={`py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
                      form.experienceLevel === l
                        ? 'bg-indigo-600 border-indigo-500 text-white'
                        : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-gray-500'
                    }`}
                  >
                    {l}
                  </button>
                ))}
              </div>
            </Field>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2 mt-2"
            >
              {loading && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
              {loading ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-400">
            Already have an account?{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>

      <style>{`
        .input {
          width: 100%;
          padding: 0.625rem 0.875rem;
          background: #111827;
          border: 1px solid #374151;
          border-radius: 0.5rem;
          color: #f9fafb;
          font-size: 0.875rem;
          outline: none;
          transition: border-color 0.15s;
          box-sizing: border-box;
        }
        .input:focus { border-color: #6366f1; }
        .input::placeholder { color: #6b7280; }
        select.input option { background: #1f2937; }
      `}</style>
    </div>
  )
}

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-300 mb-1.5">{label}</label>
      {children}
    </div>
  )
}
