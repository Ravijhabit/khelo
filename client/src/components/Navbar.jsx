import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Activity, Upload, Clock, LogOut, Dumbbell } from 'lucide-react'

const SPORT_COLORS = {
  Cricket: 'bg-green-500/20 text-green-400',
  Football: 'bg-blue-500/20 text-blue-400',
  Basketball: 'bg-orange-500/20 text-orange-400',
  Badminton: 'bg-yellow-500/20 text-yellow-400',
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isActive = (path) => location.pathname === path

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
          {user && (
            <div className="hidden sm:flex items-center gap-2">
              <span className={`text-xs px-2 py-1 rounded-full font-medium ${SPORT_COLORS[user.sport] || 'bg-gray-700 text-gray-300'}`}>
                {user.sport}
              </span>
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
