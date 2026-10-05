import { Link } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import { formatDate } from '../util'

export default function Certificates() {
  const { data, error, loading } = useFetch('/api/certificates/my')

  return (
    <>
      <h1>Certificates</h1>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading && <p className="muted" aria-live="polite">Loading…</p>}
      {data && data.length === 0 && <p>No certificates yet. Finish a course to earn one.</p>}
      {data && data.length > 0 && (
        <ul className="course-list">
          {data.map((c) => (
            <li key={c.code} className="enroll-row">
              <div>
                <h2>{c.courseTitle}</h2>
                <p className="muted">Issued {formatDate(c.issuedAt)}</p>
                <p className="code">{c.code}</p>
              </div>
              <Link className="btn btn-line" to={`/verify/${c.code}`}>Open</Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
