import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronDown, ChevronUp, MessageSquare, Calendar, Zap } from 'lucide-react'
import FeedbackCard from './FeedbackCard'

const SCORE_BG = (s) => {
  if (s >= 8) return 'bg-green-500/20 text-green-400'
  if (s >= 5) return 'bg-yellow-500/20 text-yellow-400'
  return 'bg-red-500/20 text-red-400'
}

export default function SessionCard({ session }) {
  const navigate = useNavigate()
  const [expanded, setExpanded] = useState(false)

  const imageUrl = session.imagePath
    ? `/api/sessions/${session.id}/image`
    : null

  const date = new Date(session.createdAt)
  const dateStr = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  const timeStr = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })

  const feedback = {
    overallScore: session.overallScore,
    strengths: JSON.parse(session.strengths || '[]'),
    areasToImprove: JSON.parse(session.areasToImprove || '[]'),
    priorityFix: session.priorityFix,
    drillSuggestion: session.drillSuggestion,
    confidenceLevel: session.confidenceLevel,
  }

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
      {/* Card header */}
      <div className="flex items-center gap-4 p-4">
        {/* Thumbnail */}
        <div className="w-16 h-16 shrink-0 rounded-lg overflow-hidden bg-gray-800 border border-gray-700">
          {imageUrl ? (
            <img src={imageUrl} alt="Stance" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-600 text-xs">No img</div>
          )}
        </div>

        {/* Meta */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className={`text-sm font-bold px-2 py-0.5 rounded-full ${SCORE_BG(session.overallScore)}`}>
              {session.overallScore}/10
            </span>
          </div>
          <div className="flex items-center gap-1 text-xs text-gray-500 mb-1">
            <Calendar className="w-3 h-3" />
            {dateStr} · {timeStr}
          </div>
          <div className="flex items-start gap-1 text-xs text-gray-400 line-clamp-2">
            <Zap className="w-3 h-3 text-indigo-400 shrink-0 mt-0.5" />
            {session.priorityFix}
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col gap-2 shrink-0">
          <button
            onClick={() => navigate(`/sessions/${session.id}`)}
            className="p-2 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-400 transition-colors"
            title="Ask the coach"
          >
            <MessageSquare className="w-4 h-4" />
          </button>
          <button
            onClick={() => setExpanded(!expanded)}
            className="p-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-400 transition-colors"
            title={expanded ? 'Collapse' : 'View full report'}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Expanded report */}
      {expanded && (
        <div className="border-t border-gray-800 p-4">
          <FeedbackCard feedback={feedback} />
        </div>
      )}
    </div>
  )
}
