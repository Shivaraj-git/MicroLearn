import { useState } from 'react'
import Field from './Field'
import { LIMITS } from '../limits'

/** Shared by "New course" and the details section of the course editor. */
export default function CourseForm({ initial, submitLabel, onSubmit }) {
  const [form, setForm] = useState({
    title: initial?.title ?? '',
    description: initial?.description ?? '',
    price: initial?.price ?? '',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await onSubmit({ title: form.title.trim(), description: form.description.trim(), price: Number(form.price) })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit}>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      <Field label="Title" required maxLength={LIMITS.title} value={form.title} onChange={set('title')} />
      <label className="field">
        <span>Description</span>
        <textarea rows={4} maxLength={LIMITS.description} value={form.description} onChange={set('description')} />
        <small>{form.description.length} / {LIMITS.description}</small>
      </label>
      <Field label="Price (₹)" type="number" min="0" step="any" required hint="Use 0 for a free course." value={form.price} onChange={set('price')} />
      <button className="btn" disabled={busy}>{busy ? 'Saving…' : submitLabel}</button>
    </form>
  )
}
