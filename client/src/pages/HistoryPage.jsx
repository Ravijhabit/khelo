import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionsApi } from '../services/api'
import SessionCard from '../components/SessionCard'
import { Clock, Upload } from 'lucide-react'

export default function HistoryPage() {
  const navigate = useNavigate()
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    sessionsApi
      .list()
      .then((res) => setSessions(res.data))
      .catch(() => setError('Could not load session history.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Clock className="w-6 h-6 text-indigo-400" />
            Session History
          </h1>
          <p className="text-gray-400 mt-1 text-sm">All your past coaching sessions</p>
        </div>
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors"
        >
          <Upload className="w-4 h-4" />
          New Analysis
        </button>
      </div>

      {loading && (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-gray-900 border border-gray-800 rounded-2xl p-4 animate-pulse">
              <div className="flex gap-4">
                <div className="w-16 h-16 bg-gray-800 rounded-lg shrink-0" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-800 rounded w-20" />
                  <div className="h-3 bg-gray-800 rounded w-32" />
                  <div className="h-3 bg-gray-800 rounded w-full" />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {error && (
        <div className="px-4 py-3 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {!loading && !error && sessions.length === 0 && (
        <div className="text-center py-20 bg-gray-900 border border-gray-800 rounded-2xl">
          <Clock className="w-10 h-10 text-gray-700 mx-auto mb-3" />
          <p className="text-gray-400 font-medium">No sessions yet</p>
          <p className="text-gray-600 text-sm mt-1">Upload your first stance photo to get started.</p>
          <button
            onClick={() => navigate('/dashboard')}
            className="mt-4 px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors"
          >
            Upload a Photo
          </button>
        </div>
      )}

      {!loading && sessions.length > 0 && (
        <div className="space-y-4">
          <p className="text-xs text-gray-500">{sessions.length} session{sessions.length !== 1 ? 's' : ''}</p>
          {sessions.map((s) => (
            <SessionCard key={s.id} session={s} />
          ))}
        </div>
      )}
    </div>
  )
}
