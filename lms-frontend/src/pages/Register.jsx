import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import Field from '../components/Field'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'STUDENT' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    if (form.password.length < 8) {
      setError('Choose a password with at least 8 characters.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await register({ ...form, name: form.name.trim(), email: form.email.trim() })
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <h1>Create your account</h1>
      <form className="panel" onSubmit={submit}>
        {error && <div className="notice notice-error" role="alert">{error}</div>}
        <Field label="Name" autoComplete="name" required value={form.name} onChange={set('name')} />
        <Field label="Email" type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        <Field label="Password" type="password" autoComplete="new-password" required hint="At least 8 characters." value={form.password} onChange={set('password')} />
        <label className="field">
          <span>I want to</span>
          <select value={form.role} onChange={set('role')}>
            <option value="STUDENT">Learn (student)</option>
            <option value="INSTRUCTOR">Teach (instructor)</option>
          </select>
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="muted">Already registered? <Link to="/login">Sign in</Link></p>
    </div>
  )
}
