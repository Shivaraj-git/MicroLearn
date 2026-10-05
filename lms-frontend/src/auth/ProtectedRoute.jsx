import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Wrap a page: <ProtectedRoute roles={['STUDENT']}><Page /></ProtectedRoute> */
export default function ProtectedRoute({ roles, children }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="notice notice-error">
        This page is only available to {roles.join(' / ').toLowerCase()} accounts.
      </div>
    )
  }
  return children
}
