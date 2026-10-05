import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useFetch } from '../api/useFetch'
import StatusBadge from '../components/StatusBadge'
import UserIcon from '../components/UserIcon'
import { roleLabel } from '../util'

export default function Profile() {
  const { user } = useAuth()
  const { data: me } = useFetch('/api/users/me')

  return (
    <>
      <div className="profile-head">
        <div className="avatar avatar-lg"><UserIcon size={30} /></div>
        <div>
          <h1>{user.name}</h1>
          <p className="muted">{me ? `${me.email} · ` : ''}{roleLabel(user.role)}</p>
        </div>
        <Link className="btn btn-line" to="/profile/edit">Edit profile</Link>
      </div>
      {user.role === 'STUDENT'
        ? <MyLearning />
        : <p className="muted">Learning progress is shown for student accounts.</p>}
    </>
  )
}

function MyLearning() {
  const [state, setState] = useState({ loading: true, error: '', courses: [] })

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const enrollments = await api('/api/enrollments/my')
        const enrolled = enrollments.filter((e) => e.status === 'CONFIRMED')
        const courses = (await Promise.all(enrolled.map(async (e) => {
          try {
            let title = e.courseTitle
            if (!title) {
              try { title = (await api(`/api/catalog/${e.courseId}`)).title } catch { /* unavailable */ }
            }
            const p = await api(`/api/progress/courses/${e.courseId}`)
            return { id: e.courseId, title: title || `Course #${e.courseId}`,
                     done: p.completedLessons.length, total: p.totalLessons, completed: p.courseCompleted }
          } catch {
            return null   // no access, or the course is not available right now: skip it
          }
        }))).filter(Boolean)
        if (!cancelled) setState({ loading: false, error: '', courses })
      } catch (e) {
        if (!cancelled) setState({ loading: false, error: e.message, courses: [] })
      }
    })()
    return () => { cancelled = true }
  }, [])

  if (state.loading) return <p className="muted" aria-live="polite">Loading your courses…</p>
  if (state.error) return <div className="notice notice-error" role="alert">{state.error}</div>

  const active = state.courses.filter((c) => !c.completed)
  const finished = state.courses.filter((c) => c.completed)

  return (
    <>
      <h2>In progress</h2>
      {active.length === 0 ? (
        <p className="muted">Nothing in progress. <Link to="/courses">Browse courses</Link></p>
      ) : (
        <ul className="course-list" style={{ marginBottom: '2rem' }}>
          {active.map((c) => (
            <li key={c.id} className="enroll-row">
              <div style={{ flex: 1 }}>
                <h2><Link to={`/learn/${c.id}`}>{c.title}</Link></h2>
                <p className="muted">{c.done} of {c.total} lessons done</p>
                <div className="bar-track slim"><div className="bar-fill" style={{ width: `${c.total ? (c.done / c.total) * 100 : 0}%` }} /></div>
              </div>
              <Link className="btn btn-line" to={`/learn/${c.id}`}>Continue</Link>
            </li>
          ))}
        </ul>
      )}

      <h2>Completed</h2>
      {finished.length === 0 ? (
        <p className="muted">No completed courses yet.</p>
      ) : (
        <ul className="course-list">
          {finished.map((c) => (
            <li key={c.id} className="enroll-row">
              <div>
                <h2><Link to={`/learn/${c.id}`}>{c.title}</Link></h2>
                <p className="muted">{c.total} of {c.total} lessons done</p>
              </div>
              <div className="actions">
                <StatusBadge status="COMPLETED" />
                <Link className="btn btn-line" to="/certificates">Certificate</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
