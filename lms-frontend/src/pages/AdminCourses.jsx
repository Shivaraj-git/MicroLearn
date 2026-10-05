import { useState } from 'react'
import { api } from '../api/client'
import { useFetch } from '../api/useFetch'
import StatusBadge from '../components/StatusBadge'
import { formatPrice, lessonsLabel } from '../util'

export default function AdminCourses() {
  const { data, error, reload } = useFetch('/api/admin/courses')
  const [actionError, setActionError] = useState('')

  async function act(c, action) {
    if (action === 'suspend' && !window.confirm(`Suspend "${c.title}"? It will leave the catalog and the instructor cannot re-publish it.`)) return
    setActionError('')
    try {
      await api(`/api/admin/courses/${c.id}/${action}`, { method: 'POST' })
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  return (
    <>
      <h1>Manage courses</h1>
      {(error || actionError) && <div className="notice notice-error" role="alert">{error || actionError}</div>}
      {data && data.length === 0 && <p>There are no courses yet.</p>}
      {data && data.length > 0 && (
        <ul className="course-list">
          {data.map((c) => (
            <li key={c.id} className="enroll-row">
              <div>
                <h2>{c.title}</h2>
                <p className="muted">{c.instructorEmail} · {lessonsLabel(c.lessonCount)} · {formatPrice(c.price)}</p>
              </div>
              <div className="actions">
                <StatusBadge status={c.status} />
                {c.status === 'PUBLISHED' && <button className="btn btn-line" onClick={() => act(c, 'suspend')}>Suspend</button>}
                {c.status === 'SUSPENDED' && <button className="btn btn-line" onClick={() => act(c, 'reinstate')}>Reinstate</button>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
