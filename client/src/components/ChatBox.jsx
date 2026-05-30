import { useState, useEffect, useRef } from 'react'
import { chatApi } from '../services/api'
import { Send, Bot, User, Loader2 } from 'lucide-react'

/**
 * Renders a coaching chat thread for a specific session.
 * Loads prior message history on mount and appends new exchanges in real time.
 * The AI has full session context on the backend — this component is purely UI.
 *
 * @param {string|number} sessionId - the coaching session to chat about
 */
export default function ChatBox({ sessionId }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(true)
  const bottomRef = useRef(null)

  /**
   * Fetches the full conversation history for this session on mount.
   * Errors are silently swallowed — the chat just starts empty rather than
   * blocking the player with an error they can't act on.
   */
  useEffect(() => {
    chatApi
      .history(sessionId)
      .then((res) => setMessages(res.data))
      .catch(() => {})
      .finally(() => setFetching(false))
  }, [sessionId])

  /**
   * Auto-scrolls to the latest message whenever the messages array grows.
   * The ref sits on a zero-height div at the bottom of the message list.
   */
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  /**
   * Sends the player's message and appends the AI reply.
   * Optimistically adds the user bubble before the API call completes so
   * the UI feels instant. Falls back to an inline error bubble rather than
   * a full-page error state — the player can simply try again.
   */
  const send = async () => {
    const text = input.trim()
    if (!text || loading) return
    setInput('')
    // Optimistic update — show the user's message immediately
    const userMsg = { role: 'user', content: text, createdAt: new Date().toISOString() }
    setMessages((prev) => [...prev, userMsg])
    setLoading(true)
    try {
      const res = await chatApi.send(sessionId, text)
      setMessages((prev) => [...prev, res.data])
    } catch {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'Sorry, I could not process that. Please try again.', createdAt: new Date().toISOString() },
      ])
    } finally {
      setLoading(false)
    }
  }

  /**
   * Submits on Enter (without Shift) to match chat app conventions.
   * Shift+Enter inserts a newline for multi-line questions.
   */
  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  if (fetching) {
    return (
      <div className="flex items-center justify-center h-40 text-gray-500">
        <Loader2 className="w-5 h-5 animate-spin mr-2" /> Loading chat...
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 overflow-y-auto space-y-4 mb-4 max-h-96 pr-1">
        {/* Empty state with suggested questions to lower the barrier to first engagement */}
        {messages.length === 0 && (
          <div className="text-center py-8">
            <Bot className="w-8 h-8 text-gray-700 mx-auto mb-2" />
            <p className="text-gray-500 text-sm">Ask your coach anything about this session.</p>
            <div className="mt-4 flex flex-col gap-2 items-center">
              {[
                'How do I fix my priority issue?',
                'Give me a warm-up drill',
                'What should I focus on tomorrow?',
              ].map((q) => (
                <button
                  key={q}
                  onClick={() => setInput(q)}
                  className="text-xs text-indigo-400 hover:text-indigo-300 bg-indigo-500/10 hover:bg-indigo-500/20 px-3 py-1.5 rounded-full transition-colors"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i} className={`flex gap-3 ${m.role === 'user' ? 'flex-row-reverse' : ''}`}>
            <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${
              m.role === 'user' ? 'bg-indigo-600' : 'bg-gray-800'
            }`}>
              {m.role === 'user'
                ? <User className="w-3.5 h-3.5 text-white" />
                : <Bot className="w-3.5 h-3.5 text-indigo-400" />
              }
            </div>
            <div className={`max-w-[80%] px-4 py-2.5 rounded-2xl text-sm leading-relaxed ${
              m.role === 'user'
                ? 'bg-indigo-600 text-white rounded-tr-sm'
                : 'bg-gray-800 text-gray-200 rounded-tl-sm'
            }`}>
              {m.content}
            </div>
          </div>
        ))}

        {/* Typing indicator while waiting for AI reply */}
        {loading && (
          <div className="flex gap-3">
            <div className="w-7 h-7 rounded-full bg-gray-800 flex items-center justify-center shrink-0">
              <Bot className="w-3.5 h-3.5 text-indigo-400" />
            </div>
            <div className="bg-gray-800 px-4 py-3 rounded-2xl rounded-tl-sm flex items-center gap-1">
              <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
            </div>
          </div>
        )}
        {/* Scroll anchor — always kept at the bottom of the message list */}
        <div ref={bottomRef} />
      </div>

      <div className="flex gap-2">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKey}
          rows={1}
          placeholder="Ask your coach a question..."
          disabled={loading}
          className="flex-1 px-4 py-2.5 bg-gray-800 border border-gray-700 focus:border-indigo-500 rounded-xl text-sm text-gray-100 placeholder-gray-500 outline-none resize-none disabled:opacity-50 transition-colors"
        />
        <button
          onClick={send}
          disabled={!input.trim() || loading}
          className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white rounded-xl transition-colors shrink-0"
        >
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}
