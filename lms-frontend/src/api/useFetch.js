import { useEffect, useState } from 'react'
import { api } from './client'

/** Loads `path` whenever it changes (null = skip). Returns { data, error, loading, reload }. */
export function useFetch(path) {
  const [state, setState] = useState({ data: null, error: '', loading: !!path })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!path) { setState({ data: null, error: '', loading: false }); return }
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: '' }))
    api(path)
      .then((data) => !cancelled && setState({ data, error: '', loading: false }))
      .catch((e) => !cancelled && setState({ data: null, error: e.message, loading: false }))
    return () => { cancelled = true }
  }, [path, version])

  return { ...state, reload: () => setVersion((v) => v + 1) }
}
