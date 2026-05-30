import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { sessionsApi } from '../services/api'
import UploadZone from '../components/UploadZone'
import FeedbackCard from '../components/FeedbackCard'
import { Sparkles, ArrowRight, Brain } from 'lucide-react'

/**
 * The primary feature page — stance upload and AI coaching feedback.
 * Flow: select image → click Analyze → display structured coaching report → optionally navigate to chat.
 * Session state is local; the feedback is also persisted in the backend so it appears in History.
 */
export default function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [sessionId, setSessionId] = useState(null)
  const [error, setError] = useState('')

  /**
   * Submits the selected image to the backend for Gemini analysis.
   * Uses FormData to send the file as multipart/form-data — required by Spring's
   * @RequestParam("image") MultipartFile handler.
   *
   * Extracts feedback and session ID from the response so:
   * - feedback drives the immediate FeedbackCard display
   * - sessionId enables the "Ask the Coach" navigation without a separate fetch
   */
  const handleAnalyze = async () => {
    if (!file) return
    setError('')
    setLoading(true)
    setResult(null)
    try {
      const fd = new FormData()
      fd.append('image', file)
      const res = await sessionsApi.upload(fd)
      setResult(res.data.feedback)
      setSessionId(res.data.id)
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-white">
          Hey, {user?.name?.split(' ')[0]} 👋
        </h1>
        <p className="text-gray-400 mt-1">
          Upload a photo of your {user?.sport?.toLowerCase()} technique for AI coaching feedback.
        </p>
      </div>

      {/* Player context badge — reminds the player what profile the AI will use */}
      <div className="flex items-center gap-3 mb-6 p-3 bg-gray-900 border border-gray-800 rounded-xl">
        <div className="w-10 h-10 rounded-full bg-indigo-600/20 flex items-center justify-center text-indigo-400 font-bold">
          {user?.name?.[0]?.toUpperCase()}
        </div>
        <div>
          <p className="text-sm font-medium text-white">{user?.name}</p>
          <p className="text-xs text-gray-400">{user?.sport} · {user?.position} · {user?.experienceLevel}</p>
        </div>
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mb-4">
        <h2 className="text-base font-semibold text-white mb-4 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-indigo-400" />
          Upload Stance Photo
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
          disabled={!file || loading}
          className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 mb-6"
        >
          {loading ? (
            <>
              <Brain className="w-5 h-5 animate-pulse" />
              Analyzing your technique...
            </>
          ) : (
            <>
              <Sparkles className="w-5 h-5" />
              Get AI Coaching Feedback
            </>
          )}
        </button>
      )}

      {/* Skeleton loader — mirrors the FeedbackCard shape to prevent layout shift */}
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
            Coaching Report
          </h2>
          <FeedbackCard feedback={result} />

          <div className="mt-6 flex gap-3">
            {/* Navigate to SessionPage where the player can chat with the AI about this report */}
            <button
              onClick={() => navigate(`/sessions/${sessionId}`)}
              className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2 text-sm"
            >
              Ask the Coach
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={() => { setResult(null); setFile(null); setSessionId(null) }}
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
