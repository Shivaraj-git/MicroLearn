import { useEffect } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import UserIcon from './UserIcon'
import { roleLabel } from '../util'

// Each later step adds its links here. `roles` limits who sees a link (omit = everyone).
const NAV = [
  { to: '/', label: 'Home', end: true },
  { to: '/courses', label: 'Courses' },
  { to: '/teach', label: 'My courses', roles: ['INSTRUCTOR'] },
  { to: '/enrollments', label: 'My enrollments', roles: ['STUDENT'] },
  { to: '/notifications', label: 'Notifications', roles: ['STUDENT'] },
  { to: '/certificates', label: 'Certificates', roles: ['STUDENT'] },
  { to: '/admin/users', label: 'Users', roles: ['ADMIN'] },
  { to: '/admin/courses', label: 'Manage courses', roles: ['ADMIN'] },
  { to: '/admin/enrollments', label: 'Enrollments', roles: ['ADMIN'] },
  { to: '/verify', label: 'Verify' },
]

export default function Layout() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  useEffect(() => {
    const hit = NAV.find((l) => l.to !== '/' && pathname.startsWith(l.to))
    document.title = hit ? `${hit.label} · MicroLearn` : 'MicroLearn'
    window.scrollTo(0, 0)
  }, [pathname])
  const links = NAV.filter((l) => !l.roles || (user && l.roles.includes(user.role)))

  return (
    <>
      <header className="bar">
        <div className="bar-inner">
          <Link to="/" className="brand">
            <span className="brand-mark" aria-hidden="true" />
            MicroLearn
          </Link>
          <nav aria-label="Main">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end}>{l.label}</NavLink>
            ))}
          </nav>
          <div className="bar-user">
            {user ? (
              <>
                <NavLink className="avatar" to="/profile" aria-label="My profile" title="My profile"><UserIcon /></NavLink>
                <span className="who">{user.name}<small>{roleLabel(user.role)}</small></span>
                <button className="btn btn-quiet" onClick={() => { signOut(); navigate('/') }}>
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link className="btn btn-quiet" to="/login">Sign in</Link>
                <Link className="btn" to="/register">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="page"><Outlet /></main>
    </>
  )
}
