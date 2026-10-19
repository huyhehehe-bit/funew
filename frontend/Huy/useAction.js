import { useRef, useState } from 'react'

export default function useAction() {
  const lock = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  const run = async (action) => {
    if (lock.current) return false
    lock.current = true
    setPending(true)
    setError('')
    try {
      await action()
      return true
    } catch (failure) {
      setError(failure.message)
      return false
    } finally {
      lock.current = false
      setPending(false)
    }
  }

  return { pending, error, run, clearError: () => setError('') }
}
