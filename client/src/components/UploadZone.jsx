import { useRef, useState } from 'react'
import { Upload, Image, X } from 'lucide-react'

const MAX_SIZE_MB = 5
const ALLOWED_TYPES = ['image/jpeg', 'image/png']

export default function UploadZone({ onFile, disabled }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [preview, setPreview] = useState(null)
  const [error, setError] = useState('')

  const validate = (file) => {
    if (!ALLOWED_TYPES.includes(file.type)) return 'Only JPEG and PNG files are allowed.'
    if (file.size > MAX_SIZE_MB * 1024 * 1024) return `File must be under ${MAX_SIZE_MB}MB.`
    return null
  }

  const handleFile = (file) => {
    const err = validate(file)
    if (err) { setError(err); return }
    setError('')
    setPreview(URL.createObjectURL(file))
    onFile(file)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  const clear = () => {
    setPreview(null)
    setError('')
    onFile(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className="space-y-2">
      {preview ? (
        <div className="relative rounded-xl overflow-hidden border border-gray-700 bg-gray-900">
          <img src={preview} alt="Uploaded stance" className="w-full max-h-72 object-contain" />
          {!disabled && (
            <button
              onClick={clear}
              className="absolute top-2 right-2 p-1.5 bg-gray-900/80 hover:bg-gray-800 rounded-full text-gray-300 hover:text-white transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      ) : (
        <div
          onClick={() => !disabled && inputRef.current?.click()}
          onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center gap-3 cursor-pointer transition-colors ${
            dragging
              ? 'border-indigo-400 bg-indigo-500/10'
              : 'border-gray-700 hover:border-gray-500 bg-gray-900/50'
          } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
        >
          <div className="w-12 h-12 rounded-full bg-gray-800 flex items-center justify-center">
            <Upload className="w-6 h-6 text-indigo-400" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-gray-200">Drop your photo here or click to browse</p>
            <p className="text-xs text-gray-500 mt-1">JPEG / PNG · Max 5MB</p>
          </div>
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <Image className="w-3.5 h-3.5" />
            Clear, well-lit photos give the best coaching feedback
          </div>
        </div>
      )}

      {error && <p className="text-sm text-red-400">{error}</p>}

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png"
        className="hidden"
        onChange={(e) => e.target.files[0] && handleFile(e.target.files[0])}
        disabled={disabled}
      />
    </div>
  )
}
