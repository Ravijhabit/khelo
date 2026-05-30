import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sessionsApi } from '../services/api'
import FeedbackCard from '../components/FeedbackCard'
import ChatBox from '../components/ChatBox'
import { ArrowLeft, MessageSquare, ClipboardList } from 'lucide-react'

export default function SessionPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [session, setSession] = useState(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('report')

  useEffect(() => {
    sessionsApi
      .get(id)
      .then((res) => setSession(res.data))
      .catch(() => navigate('/history'))
      .finally(() => setLoading(false))
  }, [id, navigate])

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="h-8 bg-gray-800 rounded w-32 mb-6 animate-pulse" />
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 animate-pulse space-y-4">
          {[1, 2, 3, 4].map((i) => <div key={i} className="h-16 bg-gray-800 rounded-xl" />)}
        </div>
      </div>
    )
  }

  if (!session) return null

  const feedback = {
    overallScore: session.overallScore,
    strengths: JSON.parse(session.strengths || '[]'),
    areasToImprove: JSON.parse(session.areasToImprove || '[]'),
    priorityFix: session.priorityFix,
    drillSuggestion: session.drillSuggestion,
    confidenceLevel: session.confidenceLevel,
  }

  const date = new Date(session.createdAt).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })

  return (
    <div className="max-w-3xl mx-auto">
      <button
        onClick={() => navigate('/history')}
        className="flex items-center gap-1.5 text-sm text-gray-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to History
      </button>

      <div className="mb-6">
        <p className="text-xs text-gray-500 mb-1">{date}</p>
        <h1 className="text-xl font-bold text-white">Coaching Session</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-900 border border-gray-800 rounded-xl p-1 mb-4">
        <TabButton active={activeTab === 'report'} onClick={() => setActiveTab('report')} icon={<ClipboardList className="w-4 h-4" />}>
          Report
        </TabButton>
        <TabButton active={activeTab === 'chat'} onClick={() => setActiveTab('chat')} icon={<MessageSquare className="w-4 h-4" />}>
          Ask Coach
        </TabButton>
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
        {activeTab === 'report' && (
          <div>
            {session.imagePath && (
              <div className="mb-6 rounded-xl overflow-hidden border border-gray-800">
                <img
                  src={`/api/sessions/${session.id}/image`}
                  alt="Stance"
                  className="w-full max-h-72 object-contain bg-gray-950"
                />
              </div>
            )}
            <FeedbackCard feedback={feedback} />
          </div>
        )}

        {activeTab === 'chat' && (
          <div>
            <p className="text-xs text-gray-500 mb-4">
              Your coach has read this session's full report and knows your profile.
            </p>
            <ChatBox sessionId={id} />
          </div>
        )}
      </div>
    </div>
  )
}

function TabButton({ active, onClick, icon, children }) {
  return (
    <button
      onClick={onClick}
      className={`flex-1 flex items-center justify-center gap-2 py-2 px-4 rounded-lg text-sm font-medium transition-colors ${
        active ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
      }`}
    >
      {icon}
      {children}
    </button>
  )
}
