import { Link } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import StatusBadge from '../components/StatusBadge'
import { formatPrice, lessonsLabel } from '../util'

export default function MyCourses() {
  const { data, error, loading } = useFetch('/api/courses/mine')

  return (
    <>
      <h1>My courses</h1>
      <p><Link className="btn" to="/teach/new">New course</Link></p>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading && <p className="muted" aria-live="polite">Loading…</p>}
      {data && data.length === 0 && <p>You have not created any courses yet.</p>}
      {data && data.length > 0 && (
        <ul className="course-list">
          {data.map((c) => (
            <li key={c.id} className="enroll-row">
              <div>
                <h2><Link to={`/teach/${c.id}`}>{c.title}</Link></h2>
                <p className="muted">{lessonsLabel(c.lessonCount)} · {formatPrice(c.price)}</p>
              </div>
              <StatusBadge status={c.status} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
