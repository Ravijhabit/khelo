import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Dumbbell, X } from 'lucide-react'

// Only sports supported by the AI analysis backend
const SPORTS = {
  Cricket:    ['Batsman', 'Bowler', 'All-Rounder', 'Wicket Keeper'],
  Basketball: ['Point Guard', 'Shooting Guard', 'Small Forward', 'Power Forward', 'Center'],
  Badminton:  ['Singles Player', 'Doubles Player', 'Mixed Doubles Player'],
}

const SPORT_COLORS = {
  Cricket:    { active: 'bg-green-600 border-green-500 text-white', dot: 'bg-green-300', card: 'border-green-500/30 bg-green-500/5' },
  Basketball: { active: 'bg-orange-600 border-orange-500 text-white', dot: 'bg-orange-300', card: 'border-orange-500/30 bg-orange-500/5' },
  Badminton:  { active: 'bg-yellow-600 border-yellow-500 text-white', dot: 'bg-yellow-300', card: 'border-yellow-500/30 bg-yellow-500/5' },
}

const LEVELS = ['Beginner', 'Intermediate', 'Advanced']

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    sportProfiles: [],   // [{ sport, position, experienceLevel }]
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const addSport = (sport) => {
    if (form.sportProfiles.some((p) => p.sport === sport)) return
    setForm((prev) => ({
      ...prev,
      sportProfiles: [...prev.sportProfiles, { sport, position: '', experienceLevel: '' }],
    }))
  }

  const removeSport = (sport) => {
    setForm((prev) => ({
      ...prev,
      sportProfiles: prev.sportProfiles.filter((p) => p.sport !== sport),
    }))
  }

  const updateProfile = (sport, key, value) => {
    setForm((prev) => ({
      ...prev,
      sportProfiles: prev.sportProfiles.map((p) =>
        p.sport === sport ? { ...p, [key]: value } : p
      ),
    }))
  }

  const set = (key, value) => setForm((prev) => ({ ...prev, [key]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.sportProfiles.length) {
      setError('Select at least one sport.')
      return
    }
    const incomplete = form.sportProfiles.find((p) => !p.position || !p.experienceLevel)
    if (incomplete) {
      setError(`Complete the position and level for ${incomplete.sport}.`)
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

  const selectedSports = form.sportProfiles.map((p) => p.sport)

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-lg">
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

          <form onSubmit={handleSubmit} className="space-y-5">
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

            {/* Sport picker */}
            <Field label="Sports" hint="Select all you train in">
              <div className="grid grid-cols-3 gap-2">
                {Object.keys(SPORTS).map((sport) => {
                  const on = selectedSports.includes(sport)
                  const colors = SPORT_COLORS[sport]
                  return (
                    <button
                      key={sport}
                      type="button"
                      onClick={() => (on ? removeSport(sport) : addSport(sport))}
                      className={`py-2 px-3 rounded-lg text-sm font-medium border transition-colors ${
                        on
                          ? colors.active
                          : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-gray-500'
                      }`}
                    >
                      {sport}
                    </button>
                  )
                })}
              </div>
            </Field>

            {/* Per-sport profile cards */}
            {form.sportProfiles.map((profile) => {
              const colors = SPORT_COLORS[profile.sport]
              return (
                <div
                  key={profile.sport}
                  className={`rounded-xl border p-4 space-y-3 ${colors.card}`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className={`w-2 h-2 rounded-full ${colors.dot}`} />
                      <span className="text-sm font-semibold text-white">{profile.sport}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeSport(profile.sport)}
                      className="p-1 rounded text-gray-500 hover:text-red-400 transition-colors"
                      aria-label={`Remove ${profile.sport}`}
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Position */}
                  <div>
                    <label className="block text-xs font-medium text-gray-400 mb-1">
                      Position / Role
                    </label>
                    <select
                      value={profile.position}
                      onChange={(e) => updateProfile(profile.sport, 'position', e.target.value)}
                      className="input text-xs"
                    >
                      <option value="">Select your role</option>
                      {SPORTS[profile.sport].map((p) => (
                        <option key={p} value={p}>{p}</option>
                      ))}
                    </select>
                  </div>

                  {/* Experience level */}
                  <div>
                    <label className="block text-xs font-medium text-gray-400 mb-1">
                      Experience Level
                    </label>
                    <div className="grid grid-cols-3 gap-1.5">
                      {LEVELS.map((level) => (
                        <button
                          key={level}
                          type="button"
                          onClick={() => updateProfile(profile.sport, 'experienceLevel', level)}
                          className={`py-1.5 rounded-md text-xs font-medium border transition-colors ${
                            profile.experienceLevel === level
                              ? 'bg-indigo-600 border-indigo-500 text-white'
                              : 'bg-gray-800 border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'
                          }`}
                        >
                          {level}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )
            })}

            {form.sportProfiles.length === 0 && (
              <p className="text-xs text-gray-600 text-center py-2">
                Select a sport above to set up your profile
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2 mt-2"
            >
              {loading && (
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              )}
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
          padding: 0.5rem 0.75rem;
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

function Field({ label, hint, children }) {
  return (
    <div>
      <div className="flex items-baseline justify-between mb-1.5">
        <label className="block text-sm font-medium text-gray-300">{label}</label>
        {hint && <span className="text-xs text-gray-500">{hint}</span>}
      </div>
      {children}
    </div>
  )
}
