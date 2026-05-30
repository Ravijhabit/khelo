import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { sessionsApi } from '../services/api'
import UploadZone from '../components/UploadZone'
import FeedbackCard from '../components/FeedbackCard'
import { Sparkles, ArrowRight, Brain, Target, ChevronRight } from 'lucide-react'

/**
 * Primary feature page — upload a stance photo and receive AI coaching feedback.
 * The sport context is driven by the Active Sport selection in the Navbar, not
 * a per-page picker, so the player's sport focus persists across all pages.
 */
export default function DashboardPage() {
  const { user, activeSport, setActiveSport } = useAuthStore()
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [sessionId, setSessionId] = useState(null)
  const [error, setError] = useState('')

  const canAnalyze = !!file && !!activeSport && !loading

  const handleAnalyze = async () => {
    if (!canAnalyze) return
    setError('')
    setLoading(true)
    setResult(null)
    try {
      const fd = new FormData()
      fd.append('image', file)
      fd.append('sport', activeSport.toUpperCase())
      const res = await sessionsApi.upload(fd)
      setResult(res.data.feedback)
      setSessionId(res.data.id)
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleReset = () => {
    setResult(null)
    setFile(null)
    setSessionId(null)
    setError('')
  }

  const sports = (user?.sportProfiles ?? []).map((p) => p.sport)
  const activeProfile = user?.sportProfiles?.find((p) => p.sport === activeSport)

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-white">
          Hey, {user?.name?.split(' ')[0]} 👋
        </h1>
        <p className="text-gray-400 mt-1">
          Upload a stance photo for personalised AI coaching feedback.
        </p>
      </div>

      {/* Player context badge */}
      <div className="flex items-center gap-3 mb-6 p-3 bg-gray-900 border border-gray-800 rounded-xl">
        <div className="w-10 h-10 rounded-full bg-indigo-600/20 flex items-center justify-center text-indigo-400 font-bold text-sm">
          {user?.name?.[0]?.toUpperCase()}
        </div>
        <div className="flex-1">
          <p className="text-sm font-medium text-white">{user?.name}</p>
          <p className="text-xs text-gray-400">
            {activeProfile ? `${activeProfile.position} · ${activeProfile.experienceLevel}` : `${sports.length} sport${sports.length !== 1 ? 's' : ''} registered`}
          </p>
        </div>
        {activeSport && (
          <span className="text-xs px-2.5 py-1 rounded-full bg-indigo-600/20 border border-indigo-500/30 text-indigo-300 font-medium">
            {activeSport}
          </span>
        )}
      </div>

      {/* No active sport selected — prompt the player to pick one from the navbar */}
      {!activeSport && sports.length > 0 && (
        <div className="mb-4 p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl flex items-start gap-3">
          <Target className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm font-medium text-amber-300">Choose your Active Sport</p>
            <p className="text-xs text-amber-400/70 mt-0.5">
              Select a sport from the navbar to start your analysis session.
            </p>
            {/* Inline quick-pick for mobile / convenience */}
            <div className="flex flex-wrap gap-2 mt-3">
              {sports.map((sport) => (
                <button
                  key={sport}
                  onClick={() => setActiveSport(sport)}
                  className="flex items-center gap-1 text-xs px-3 py-1.5 rounded-full border border-amber-500/40 text-amber-300 hover:bg-amber-500/20 transition-colors"
                >
                  {sport}
                  <ChevronRight className="w-3 h-3" />
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Image upload — always visible */}
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mb-4">
        <h2 className="text-base font-semibold text-white mb-4 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-indigo-400" />
          Upload Stance Photo
          {activeSport && (
            <span className="ml-auto text-xs font-normal text-gray-400">
              {activeSport} · {user?.experienceLevel}
            </span>
          )}
        </h2>
        <UploadZone onFile={setFile} disabled={loading} />
      </div>

      {error && (
        <div className="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {!result && (
        <button
          onClick={handleAnalyze}
          disabled={!canAnalyze}
          className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 mb-6"
        >
          {loading ? (
            <>
              <Brain className="w-5 h-5 animate-pulse" />
              Analyzing your {activeSport} technique...
            </>
          ) : (
            <>
              <Sparkles className="w-5 h-5" />
              {!activeSport
                ? 'Select an Active Sport to continue'
                : !file
                  ? 'Upload a photo to continue'
                  : `Get ${activeSport} Coaching Feedback`}
            </>
          )}
        </button>
      )}

      {/* Skeleton loader */}
      {loading && (
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 space-y-4 animate-pulse">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-full bg-gray-800" />
            <div className="space-y-2 flex-1">
              <div className="h-4 bg-gray-800 rounded w-32" />
              <div className="h-3 bg-gray-800 rounded w-24" />
            </div>
          </div>
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 bg-gray-800 rounded-xl" />
          ))}
        </div>
      )}

      {result && !loading && (
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
          <h2 className="text-base font-semibold text-white mb-5 flex items-center gap-2">
            <Brain className="w-4 h-4 text-indigo-400" />
            {activeSport} Coaching Report
          </h2>
          <FeedbackCard feedback={result} />

          <div className="mt-6 flex gap-3">
            <button
              onClick={() => navigate(`/sessions/${sessionId}`)}
              className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2 text-sm"
            >
              Ask the Coach
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={handleReset}
              className="px-4 py-2.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-medium rounded-lg transition-colors text-sm"
            >
              New Analysis
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
