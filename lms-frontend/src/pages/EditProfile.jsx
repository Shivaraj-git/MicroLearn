import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useFetch } from '../api/useFetch'
import Field from '../components/Field'
import { roleLabel } from '../util'

export default function EditProfile() {
  const { data: me } = useFetch('/api/users/me')
  return (
    <div className="auth">
      <p><Link to="/profile">Back to my profile</Link></p>
      <h1>Edit profile</h1>
      {me && (
        <dl className="facts" style={{ marginBottom: '1.5rem' }}>
          <dt>Email</dt><dd>{me.email}</dd>
          <dt>Role</dt><dd>{roleLabel(me.role)}</dd>
        </dl>
      )}
      <NameForm />
      <PasswordForm />
    </div>
  )
}

function Messages({ s }) {
  return (
    <>
      {s.error && <div className="notice notice-error" role="alert">{s.error}</div>}
      {s.ok && <div className="notice notice-ok" role="status">{s.ok}</div>}
    </>
  )
}

function NameForm() {
  const { user, applyAuth } = useAuth()
  const [name, setName] = useState(user.name)
  const [s, setS] = useState({ busy: false, error: '', ok: '' })

  async function submit(e) {
    e.preventDefault()
    setS({ busy: true, error: '', ok: '' })
    try {
      // the reply holds a fresh token carrying the new name
      applyAuth(await api('/api/users/me', { method: 'PUT', body: { name: name.trim() } }))
      setS({ busy: false, error: '', ok: 'Name updated. Your courses show it within a few seconds.' })
    } catch (err) {
      setS({ busy: false, error: err.message, ok: '' })
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Your name</h2>
      <Messages s={s} />
      <Field label="Name" required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
      <button className="btn" disabled={s.busy || !name.trim() || name.trim() === user.name}>
        {s.busy ? 'Saving…' : 'Save name'}
      </button>
    </form>
  )
}

function PasswordForm() {
  const [f, setF] = useState({ current: '', next: '', again: '' })
  const [s, setS] = useState({ busy: false, error: '', ok: '' })
  const set = (key) => (e) => setF({ ...f, [key]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    if (f.next.length < 8) return setS({ busy: false, error: 'The new password needs at least 8 characters.', ok: '' })
    if (f.next !== f.again) return setS({ busy: false, error: 'The two new passwords do not match.', ok: '' })
    setS({ busy: true, error: '', ok: '' })
    try {
      await api('/api/users/me/password', { method: 'PUT', body: { currentPassword: f.current, newPassword: f.next } })
      setF({ current: '', next: '', again: '' })
      setS({ busy: false, error: '', ok: 'Password changed.' })
    } catch (err) {
      setS({ busy: false, error: err.message, ok: '' })
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Change password</h2>
      <Messages s={s} />
      <Field label="Current password" type="password" autoComplete="current-password" required value={f.current} onChange={set('current')} />
      <Field label="New password" type="password" autoComplete="new-password" required hint="At least 8 characters." value={f.next} onChange={set('next')} />
      <Field label="New password again" type="password" autoComplete="new-password" required value={f.again} onChange={set('again')} />
      <button className="btn" disabled={s.busy}>{s.busy ? 'Saving…' : 'Change password'}</button>
    </form>
  )
}
