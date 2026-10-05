import { useState } from 'react'
import { useFetch } from '../api/useFetch'
import Pager from '../components/Pager'
import StatusBadge from '../components/StatusBadge'
import { formatPrice } from '../util'

export default function AdminEnrollments() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const { data, error } = useFetch(`/api/admin/enrollments?status=${status}&page=${page}&size=15`)

  return (
    <>
      <h1>Enrollments</h1>
      <label className="field" style={{ maxWidth: '14rem' }}>
        <span>Status</span>
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">All</option>
          <option value="PENDING">Processing</option>
          <option value="CONFIRMED">Enrolled</option>
          <option value="REJECTED">Payment declined</option>
          <option value="CANCELLED">Refunded</option>
        </select>
      </label>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {data && (
        <>
          <p className="muted count">{data.totalItems} {data.totalItems === 1 ? 'enrollment' : 'enrollments'}</p>
          <ul className="course-list">
            {data.items.map((e) => (
              <li key={e.id} className="enroll-row">
                <div>
                  <h2>{e.courseTitle || `Course #${e.courseId}`}</h2>
                  <p className="muted">Student #{e.studentId} · {formatPrice(e.price)}</p>
                </div>
                <StatusBadge status={e.status} />
              </li>
            ))}
          </ul>
          <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />
        </>
      )}
    </>
  )
}
