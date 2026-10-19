import { useCallback, useEffect, useState } from 'react'
import { api } from '../QA/api'

export default function useList(path, enabled = true) {
  const [state, setState] = useState({ items: [], loading: enabled, error: '' })

  useEffect(() => {
    if (!enabled) return
    const controller = new AbortController()
    api(path, { signal: controller.signal })
      .then((items) => setState({ items, loading: false, error: '' }))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setState((current) => ({ ...current, loading: false, error: error.message }))
        }
      })
    return () => controller.abort()
  }, [path, enabled])

  const reload = useCallback(async () => {
    if (!enabled) return
    setState((current) => ({ ...current, loading: true, error: '' }))
    try {
      const items = await api(path)
      setState({ items, loading: false, error: '' })
    } catch (error) {
      setState((current) => ({ ...current, loading: false, error: error.message }))
    }
  }, [path, enabled])

  const save = async (item, method) => {
    const body = Object.fromEntries(
      Object.entries(item).filter(([key]) => !['id', 'createdById', 'createdDate'].includes(key)),
    )
    const saved = await api(method === 'POST' ? path : `${path}/${item.id}`, { method, body })
    setState((current) => ({
      ...current,
      items: method === 'POST'
        ? [...current.items, saved]
        : current.items.map((entry) => entry.id === saved.id ? saved : entry),
    }))
    return saved
  }

  const remove = async (id) => {
    await api(`${path}/${id}`, { method: 'DELETE' })
    setState((current) => ({ ...current, items: current.items.filter((item) => item.id !== id) }))
  }

  return { ...state, reload, add: (item) => save(item, 'POST'), update: (item) => save(item, 'PUT'), remove }
}
