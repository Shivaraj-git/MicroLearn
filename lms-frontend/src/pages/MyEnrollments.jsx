import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import StatusBadge from '../components/StatusBadge'

const NOTE = {
  PENDING: 'Processing your payment…',
  CONFIRMED: 'You now have access to this course.',
  REJECTED: 'The payment was declined. No money was taken.',
  CANCELLED: 'We could not unlock the course, so your payment was refunded.',
}

export default function MyEnrollments() {
  const [state, setState] = useState({ data: null, error: '' })

  const load = useCallback(() => {
    api('/api/enrollments/my')
      .then((data) => setState({ data, error: '' }))
      .catch((e) => setState((s) => ({ ...s, error: e.message })))
  }, [])

  useEffect(() => { load() }, [load])

  // While any enrollment is still processing, check again every 2 seconds.
  const pending = state.data?.some((e) => e.status === 'PENDING')
  useEffect(() => {
    if (!pending) return
    const timer = setInterval(load, 2000)
    return () => clearInterval(timer)
  }, [pending, load])

  const { data, error } = state

  return (
    <>
      <h1>My enrollments</h1>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {!data && !error && <p className="muted" aria-live="polite">Loading…</p>}
      {data && data.length === 0 && (
        <p>You have not enrolled in anything yet. <Link to="/courses">Browse courses</Link></p>
      )}
      {data && data.length > 0 && (
        <ul className="course-list">
          {data.map((e) => (
            <li key={e.enrollmentId} className="enroll-row">
              <div>
                <h2><Link to={`/courses/${e.courseId}`}>{e.courseTitle || `Course #${e.courseId}`}</Link></h2>
                <p className="muted">{NOTE[e.status]}</p>
                {e.status === 'CONFIRMED' && <Link className="btn btn-line" to={`/learn/${e.courseId}`}>Continue learning</Link>}
              </div>
              <StatusBadge status={e.status} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
