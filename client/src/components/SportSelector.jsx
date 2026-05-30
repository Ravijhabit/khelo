const SPORTS = [
  { id: 'BADMINTON', label: 'Badminton', emoji: '🏸',
    idle: 'border-gray-700 bg-gray-800 text-gray-300 hover:border-yellow-500/50',
    active: 'border-yellow-500 bg-yellow-500/10 text-yellow-300' },
  { id: 'CRICKET',   label: 'Cricket',   emoji: '🏏',
    idle: 'border-gray-700 bg-gray-800 text-gray-300 hover:border-green-500/50',
    active: 'border-green-500 bg-green-500/10 text-green-300' },
  { id: 'BASKETBALL', label: 'Basketball', emoji: '🏀',
    idle: 'border-gray-700 bg-gray-800 text-gray-300 hover:border-orange-500/50',
    active: 'border-orange-500 bg-orange-500/10 text-orange-300' },
]

/**
 * Horizontal sport picker used on the Dashboard before stance upload.
 * Emits the sport ID string (e.g. "CRICKET") via onSelect.
 */
export default function SportSelector({ selected, onSelect, disabled = false }) {
  return (
    <div className="grid grid-cols-3 gap-3" role="radiogroup" aria-label="Select sport">
      {SPORTS.map((sport) => {
        const isSelected = selected === sport.id
        return (
          <button
            key={sport.id}
            type="button"
            role="radio"
            aria-checked={isSelected}
            aria-label={sport.label}
            disabled={disabled}
            onClick={() => !disabled && onSelect(sport.id)}
            className={`flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all
              ${isSelected ? sport.active : sport.idle}
              ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
          >
            <span className="text-2xl" role="img" aria-hidden="true">{sport.emoji}</span>
            <span className="text-sm font-medium">{sport.label}</span>
          </button>
        )
      })}
    </div>
  )
}

export { SPORTS }
