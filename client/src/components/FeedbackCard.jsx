import { CheckCircle, AlertTriangle, Zap, Dumbbell, ShieldAlert } from 'lucide-react'

const CONFIDENCE_STYLES = {
  High: 'bg-green-500/20 text-green-400 border-green-500/30',
  Medium: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  Low: 'bg-red-500/20 text-red-400 border-red-500/30',
}

const SCORE_COLOR = (score) => {
  if (score >= 8) return 'text-green-400'
  if (score >= 5) return 'text-yellow-400'
  return 'text-red-400'
}

const SCORE_RING = (score) => {
  if (score >= 8) return 'border-green-500'
  if (score >= 5) return 'border-yellow-500'
  return 'border-red-500'
}

export default function FeedbackCard({ feedback }) {
  const {
    overallScore,
    strengths = [],
    areasToImprove = [],
    priorityFix,
    drillSuggestion,
    confidenceLevel,
  } = feedback

  return (
    <div className="space-y-4">
      {/* Score + Confidence */}
      <div className="flex items-center gap-4">
        <div className={`w-20 h-20 rounded-full border-4 ${SCORE_RING(overallScore)} flex flex-col items-center justify-center shrink-0`}>
          <span className={`text-2xl font-bold ${SCORE_COLOR(overallScore)}`}>{overallScore}</span>
          <span className="text-xs text-gray-500">/10</span>
        </div>
        <div>
          <p className="text-white font-semibold text-lg">Technique Score</p>
          <span className={`inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full border font-medium mt-1 ${CONFIDENCE_STYLES[confidenceLevel] || CONFIDENCE_STYLES.Medium}`}>
            <ShieldAlert className="w-3 h-3" />
            {confidenceLevel} confidence
          </span>
        </div>
      </div>

      {/* Strengths */}
      <Section icon={<CheckCircle className="w-4 h-4 text-green-400" />} title="Strengths" color="green">
        <ul className="space-y-2">
          {strengths.map((s, i) => (
            <li key={i} className="flex items-start gap-2 text-sm text-gray-300">
              <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-green-400 shrink-0" />
              {s}
            </li>
          ))}
        </ul>
      </Section>

      {/* Areas to Improve */}
      <Section icon={<AlertTriangle className="w-4 h-4 text-yellow-400" />} title="Areas to Improve" color="yellow">
        <ul className="space-y-2">
          {areasToImprove.map((a, i) => (
            <li key={i} className="flex items-start gap-2 text-sm text-gray-300">
              <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-yellow-400 shrink-0" />
              {a}
            </li>
          ))}
        </ul>
      </Section>

      {/* Priority Fix */}
      <div className="bg-indigo-500/10 border border-indigo-500/30 rounded-xl p-4">
        <div className="flex items-center gap-2 mb-2">
          <Zap className="w-4 h-4 text-indigo-400" />
          <span className="text-sm font-semibold text-indigo-300">Priority Fix</span>
        </div>
        <p className="text-sm text-gray-200">{priorityFix}</p>
      </div>

      {/* Drill Suggestion */}
      <div className="bg-purple-500/10 border border-purple-500/30 rounded-xl p-4">
        <div className="flex items-center gap-2 mb-2">
          <Dumbbell className="w-4 h-4 text-purple-400" />
          <span className="text-sm font-semibold text-purple-300">Drill Suggestion</span>
        </div>
        <p className="text-sm text-gray-200">{drillSuggestion}</p>
      </div>
    </div>
  )
}

function Section({ icon, title, color, children }) {
  const border = { green: 'border-green-500/20 bg-green-500/5', yellow: 'border-yellow-500/20 bg-yellow-500/5' }
  return (
    <div className={`border rounded-xl p-4 ${border[color]}`}>
      <div className="flex items-center gap-2 mb-3">
        {icon}
        <span className="text-sm font-semibold text-gray-200">{title}</span>
      </div>
      {children}
    </div>
  )
}
