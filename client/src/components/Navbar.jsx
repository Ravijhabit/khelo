import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { Activity, Upload, Clock, LogOut, Dumbbell, ChevronDown } from 'lucide-react'
import { useState, useRef, useEffect } from 'react'

const SPORT_STYLES = {
  Cricket:    { dot: 'bg-green-400',  pill: 'border-green-500 bg-green-500/10 text-green-300',  idle: 'border-gray-700 text-gray-400 hover:border-green-500/50 hover:text-green-300' },
  Basketball: { dot: 'bg-orange-400', pill: 'border-orange-500 bg-orange-500/10 text-orange-300', idle: 'border-gray-700 text-gray-400 hover:border-orange-500/50 hover:text-orange-300' },
  Badminton:  { dot: 'bg-yellow-400', pill: 'border-yellow-500 bg-yellow-500/10 text-yellow-300',  idle: 'border-gray-700 text-gray-400 hover:border-yellow-500/50 hover:text-yellow-300' },
}

export default function Navbar() {
  const { user, logout, activeSport, setActiveSport } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false)
  const dropdownRef = useRef(null)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isActive = (path) => location.pathname === path

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const sports = (user?.sportProfiles ?? []).map((p) => p.sport)
  const styles = SPORT_STYLES[activeSport] ?? { dot: 'bg-gray-400', pill: 'border-gray-600 bg-gray-700/50 text-gray-300', idle: '' }

  return (
    <nav className="border-b border-gray-800 bg-gray-900/80 backdrop-blur sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link to="/dashboard" className="flex items-center gap-2 font-bold text-lg text-white">
          <Dumbbell className="w-5 h-5 text-indigo-400" />
          <span>Ghost<span className="text-indigo-400">Coach</span></span>
        </Link>

        <div className="flex items-center gap-1">
          <NavLink to="/dashboard" active={isActive('/dashboard')}>
            <Upload className="w-4 h-4" />
            <span className="hidden sm:inline">Analyze</span>
          </NavLink>
          <NavLink to="/history" active={isActive('/history')}>
            <Clock className="w-4 h-4" />
            <span className="hidden sm:inline">History</span>
          </NavLink>
          <NavLink to="/progress" active={isActive('/progress')}>
            <Activity className="w-4 h-4" />
            <span className="hidden sm:inline">Progress</span>
          </NavLink>
        </div>

        <div className="flex items-center gap-3">
          {user && sports.length > 0 && (
            <div className="hidden sm:flex items-center gap-2">
              <span className="text-xs text-gray-500 font-medium">Active Sport</span>

              {sports.length === 1 ? (
                /* Single sport — just a static pill */
                <span className={`flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full font-medium border ${styles.pill}`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${styles.dot}`} />
                  {activeSport}
                </span>
              ) : (
                /* Multi-sport — dropdown switcher */
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => setOpen((o) => !o)}
                    className={`flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full font-medium border transition-colors ${styles.pill}`}
                  >
                    <span className={`w-1.5 h-1.5 rounded-full ${styles.dot}`} />
                    {activeSport ?? 'Select sport'}
                    <ChevronDown className={`w-3 h-3 transition-transform ${open ? 'rotate-180' : ''}`} />
                  </button>

                  {open && (
                    <div className="absolute right-0 mt-2 w-40 bg-gray-900 border border-gray-700 rounded-xl shadow-xl overflow-hidden">
                      {sports.map((sport) => {
                        const s = SPORT_STYLES[sport] ?? { dot: 'bg-gray-400', idle: 'border-gray-700 text-gray-400' }
                        const isActiveItem = sport === activeSport
                        return (
                          <button
                            key={sport}
                            onClick={() => { setActiveSport(sport); setOpen(false) }}
                            className={`w-full flex items-center gap-2 px-3 py-2.5 text-sm transition-colors ${
                              isActiveItem
                                ? 'bg-gray-800 text-white font-medium'
                                : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                            }`}
                          >
                            <span className={`w-2 h-2 rounded-full flex-shrink-0 ${s.dot}`} />
                            {sport}
                            {isActiveItem && (
                              <span className="ml-auto text-indigo-400 text-xs">✓</span>
                            )}
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>
              )}

              <span className="text-sm text-gray-400">{user.name}</span>
            </div>
          )}

          <button
            onClick={handleLogout}
            className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
            title="Logout"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </nav>
  )
}

function NavLink({ to, active, children }) {
  return (
    <Link
      to={to}
      className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
        active
          ? 'bg-indigo-600 text-white'
          : 'text-gray-400 hover:text-white hover:bg-gray-800'
      }`}
    >
      {children}
    </Link>
  )
}
