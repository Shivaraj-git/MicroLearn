import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { roleLabel } from '../util'

const SHORTCUTS = {
  STUDENT: [['/courses', 'Browse courses'], ['/enrollments', 'My enrollments'], ['/certificates', 'Certificates']],
  INSTRUCTOR: [['/teach', 'My courses'], ['/teach/new', 'New course'], ['/courses', 'Browse courses']],
  ADMIN: [['/admin/users', 'Users'], ['/admin/courses', 'Manage courses'], ['/admin/enrollments', 'Enrollments']],
}

export default function Home() {
  const { user } = useAuth()
  return user ? <Dashboard user={user} /> : <Welcome />
}

function Welcome() {
  return (
    <section className="hero">
      <h1>Courses you finish. Certificates you can verify.</h1>
      <p>Browse courses, enroll in a few clicks, and earn a certificate that anyone can check with a code.</p>
      <div className="actions">
        <Link className="btn" to="/courses">Browse courses</Link>
        <Link className="btn btn-line" to="/register">Create account</Link>
      </div>
    </section>
  )
}

function Dashboard({ user }) {
  const [me, setMe] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/api/users/me').then(setMe).catch((e) => setError(e.message))
  }, [])

  return (
    <>
      <h1>Welcome back, {user.name}</h1>
      <div className="actions" style={{ marginBottom: '1.5rem' }}>
        {(SHORTCUTS[user.role] || []).map(([to, label], i) => (
          <Link key={to} className={i === 0 ? 'btn' : 'btn btn-line'} to={to}>{label}</Link>
        ))}
      </div>
      <div className="panel">
        <h2>Your account</h2>
        {error && <div className="notice notice-error">{error}</div>}
        {me && (
          <dl className="facts">
            <dt>Email</dt><dd>{me.email}</dd>
            <dt>Role</dt><dd>{roleLabel(me.role)}</dd>
          </dl>
        )}
        {!me && !error && <p className="muted">Loading…</p>}
        <p style={{ margin: '1rem 0 0' }}><Link to="/profile">View your profile</Link></p>
      </div>
    </>
  )
}
