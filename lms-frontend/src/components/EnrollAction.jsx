import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { useFetch } from '../api/useFetch'
import { formatPrice } from '../util'

export default function EnrollAction({ course }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Link className="btn" to="/login" state={{ from: location.pathname }}>Sign in to enroll</Link>
  }
  if (user.role !== 'STUDENT') return <p className="muted">Only student accounts can enroll.</p>
  return <StudentEnroll course={course} />
}

function StudentEnroll({ course }) {
  const navigate = useNavigate()
  const { data } = useFetch('/api/enrollments/my')
  const [method, setMethod] = useState('TEST_SUCCESS')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const mine = data?.find(
    (e) => e.courseId === course.courseId && (e.status === 'PENDING' || e.status === 'CONFIRMED'),
  )
  if (mine) {
    if (mine.status === 'CONFIRMED') {
      return (
        <>
          <p>You are enrolled in this course.</p>
          <Link className="btn" to={`/learn/${course.courseId}`}>Go to course</Link>
        </>
      )
    }
    return <p>Your enrollment is being processed. <Link to="/enrollments">View enrollments</Link></p>
  }

  async function enroll() {
    setBusy(true)
    setError('')
    try {
      await api('/api/enrollments', { method: 'POST', body: { courseId: course.courseId, paymentMethod: method } })
      navigate('/enrollments')
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      <label className="field">
        <span>Test payment</span>
        <select value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="TEST_SUCCESS">Payment succeeds</option>
          <option value="TEST_FAIL">Payment is declined</option>
        </select>
        <small>Payments are simulated. Pick what the fake gateway should do.</small>
      </label>
      <button className="btn" disabled={busy} onClick={enroll}>
        {busy ? 'Enrolling…' : Number(course.price) === 0 ? 'Enroll for free' : `Enroll for ${formatPrice(course.price)}`}
      </button>
    </>
  )
}
