import { useFetch } from '../api/useFetch'
import StatusBadge from '../components/StatusBadge'
import { formatDate } from '../util'

export default function Notifications() {
  const { data, error, loading } = useFetch('/api/notifications/my')

  return (
    <>
      <h1>Notifications</h1>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading && <p className="muted" aria-live="polite">Loading…</p>}
      {data && data.length === 0 && <p>Nothing here yet. Updates about your enrollments will show up here.</p>}
      {data && data.length > 0 && (
        <ul className="course-list">
          {data.map((n) => (
            <li key={n.id} className="enroll-row">
              <div>
                <p>{n.message}</p>
                <small className="muted">{formatDate(n.createdAt)}</small>
              </div>
              <StatusBadge status={n.type} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
