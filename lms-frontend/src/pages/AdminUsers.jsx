import { useState } from 'react'
import { api } from '../api/client'
import { useFetch } from '../api/useFetch'
import { useAuth } from '../auth/AuthContext'
import Pager from '../components/Pager'
import StatusBadge from '../components/StatusBadge'
import { roleLabel } from '../util'

export default function AdminUsers() {
  const { user: me } = useAuth()
  const [q, setQ] = useState('')
  const [text, setText] = useState('')
  const [page, setPage] = useState(0)
  const [actionError, setActionError] = useState('')
  const { data, error, reload } = useFetch(`/api/admin/users?q=${encodeURIComponent(q)}&page=${page}&size=15`)

  async function toggle(u) {
    if (u.enabled && !window.confirm(`Disable ${u.name}? They will not be able to sign in.`)) return
    setActionError('')
    try {
      await api(`/api/admin/users/${u.id}/${u.enabled ? 'disable' : 'enable'}`, { method: 'POST' })
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  return (
    <>
      <h1>Users</h1>
      <p className="muted">Disabling blocks new sign-ins. Someone already signed in keeps access until their session ends (up to 60 minutes).</p>
      <form className="search" role="search" onSubmit={(e) => { e.preventDefault(); setPage(0); setQ(text.trim()) }}>
        <input aria-label="Search users" placeholder="Search by name or email" value={text} onChange={(e) => setText(e.target.value)} />
        <button className="btn">Search</button>
      </form>
      {(error || actionError) && <div className="notice notice-error" role="alert">{error || actionError}</div>}
      {data && (
        <>
          <p className="muted count">{data.totalItems} {data.totalItems === 1 ? 'user' : 'users'}</p>
          <ul className="course-list">
            {data.items.map((u) => (
              <li key={u.id} className="enroll-row">
                <div>
                  <h2>{u.name}</h2>
                  <p className="muted">{u.email} · {roleLabel(u.role)}</p>
                </div>
                <div className="actions">
                  <StatusBadge status={u.enabled ? 'ACTIVE' : 'DISABLED'} />
                  {u.id !== me.id && (
                    <button className="btn btn-line" onClick={() => toggle(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>
                  )}
                </div>
              </li>
            ))}
          </ul>
          <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />
        </>
      )}
    </>
  )
}
