import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionsApi } from '../services/api'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine,
} from 'recharts'
import { Activity, TrendingUp, TrendingDown, Minus, Upload } from 'lucide-react'

/**
 * Visualises a player's coaching score over time using a Recharts line chart.
 * Requires at least 2 sessions to render — a single data point is not a trend.
 *
 * Derives three summary stats inline (avg, best, last-vs-previous trend) from
 * the same sessions array used for the chart, avoiding redundant API calls.
 */
export default function ProgressPage() {
  const navigate = useNavigate()
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    sessionsApi
      .list()
      .then((res) => setSessions(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  /**
   * Transforms sessions into Recharts-compatible data points.
   * Sessions from the API come newest-first; reversed here so the chart
   * reads chronologically left-to-right (oldest → newest).
   */
  const chartData = [...sessions]
    .reverse()
    .map((s, i) => ({
      session: `#${i + 1}`,
      score: s.overallScore,
      date: new Date(s.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    }))

  const avg = sessions.length
    ? (sessions.reduce((sum, s) => sum + s.overallScore, 0) / sessions.length).toFixed(1)
    : 0
  const best = sessions.length ? Math.max(...sessions.map((s) => s.overallScore)) : 0

  /**
   * Last vs. previous session delta — positive means improvement.
   * chartData is in chronological order, so the last two elements are the
   * most recent pair. Index -1 and -2 via slice would work but this is clearer.
   */
  const trend = chartData.length >= 2
    ? chartData[chartData.length - 1].score - chartData[chartData.length - 2].score
    : 0

  const TrendIcon = trend > 0 ? TrendingUp : trend < 0 ? TrendingDown : Minus
  const trendColor = trend > 0 ? 'text-green-400' : trend < 0 ? 'text-red-400' : 'text-gray-400'

  /** Custom Recharts tooltip — shows the human-readable date alongside the score. */
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload?.length) {
      return (
        <div className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm">
          <p className="text-gray-400">{payload[0].payload.date}</p>
          <p className="text-white font-semibold">Score: {payload[0].value}/10</p>
        </div>
      )
    }
    return null
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Activity className="w-6 h-6 text-indigo-400" />
            Progress
          </h1>
          <p className="text-gray-400 mt-1 text-sm">Your technique score over time</p>
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
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 animate-pulse">
          <div className="h-48 bg-gray-800 rounded-xl" />
        </div>
      )}

      {/* Guard against single-session players — a single point is not a progress trend */}
      {!loading && sessions.length < 2 && (
        <div className="text-center py-20 bg-gray-900 border border-gray-800 rounded-2xl">
          <Activity className="w-10 h-10 text-gray-700 mx-auto mb-3" />
          <p className="text-gray-400 font-medium">Not enough data yet</p>
          <p className="text-gray-600 text-sm mt-1">Complete at least 2 sessions to see your progress chart.</p>
          <button
            onClick={() => navigate('/dashboard')}
            className="mt-4 px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors"
          >
            Upload a Photo
          </button>
        </div>
      )}

      {!loading && sessions.length >= 2 && (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <StatCard label="Avg Score" value={`${avg}/10`} />
            <StatCard label="Best Score" value={`${best}/10`} color="text-green-400" />
            <StatCard
              label="Last vs Prev"
              value={`${trend > 0 ? '+' : ''}${trend}`}
              color={trendColor}
              icon={<TrendIcon className={`w-4 h-4 ${trendColor}`} />}
            />
          </div>

          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
            <h2 className="text-sm font-semibold text-gray-300 mb-6">Score Over Sessions</h2>
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={chartData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                <XAxis dataKey="session" tick={{ fill: '#6b7280', fontSize: 12 }} axisLine={false} tickLine={false} />
                {/* Fixed domain [0,10] matches the coaching score scale */}
                <YAxis domain={[0, 10]} tick={{ fill: '#6b7280', fontSize: 12 }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                {/* Reference line at 5 marks the mid-point — below this is "needs work" territory */}
                <ReferenceLine y={5} stroke="#374151" strokeDasharray="4 4" />
                <Line
                  type="monotone"
                  dataKey="score"
                  stroke="#6366f1"
                  strokeWidth={2.5}
                  dot={{ fill: '#6366f1', r: 4, strokeWidth: 0 }}
                  activeDot={{ r: 6, fill: '#818cf8' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  )
}

/** Reusable stat display card used in the three-column summary row. */
function StatCard({ label, value, color = 'text-white', icon }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 text-center">
      <p className="text-xs text-gray-500 mb-1">{label}</p>
      <div className="flex items-center justify-center gap-1">
        {icon}
        <p className={`text-xl font-bold ${color}`}>{value}</p>
      </div>
    </div>
  )
}
