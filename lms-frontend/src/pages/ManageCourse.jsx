import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import CourseForm from '../components/CourseForm'
import Field from '../components/Field'
import StatusBadge from '../components/StatusBadge'
import { LIMITS } from '../limits'

export default function ManageCourse() {
  const { id } = useParams()
  const base = `/api/courses/${id}`
  const [course, setCourse] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [lesson, setLesson] = useState({ title: '', content: '' })

  const load = useCallback(() => api(base).then(setCourse), [base])
  useEffect(() => { load().catch((e) => setError(e.message)) }, [load])

  // Runs an action, reloads the course, and shows a message. Returns true if it worked.
  async function run(action, message) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await action()
      await load()
      setNotice(message)
      return true
    } catch (e) {
      setError(e.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  if (!course) {
    return error
      ? <><div className="notice notice-error" role="alert">{error}</div><Link to="/teach">Back to my courses</Link></>
      : <p className="muted" aria-live="polite">Loading…</p>
  }

  const draft = course.status === 'DRAFT'
  const post = (path, body) => api(`${base}${path}`, { method: 'POST', body })

  return (
    <>
      <p><Link to="/teach">Back to my courses</Link></p>
      <h1>{course.title} <StatusBadge status={course.status} /></h1>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-ok" role="status">{notice}</div>}

      <section className="panel">
        <h2>Visibility</h2>
        {draft && (
          <>
            <p className="muted">Students cannot see this course yet. {course.lessons.length === 0 && 'Add at least one lesson to publish it.'}</p>
            <button className="btn" disabled={busy || course.lessons.length === 0}
              onClick={() => run(() => post('/publish'), 'Published. It can take a few seconds to appear in the catalog.')}>Publish</button>
          </>
        )}
        {course.status === 'PUBLISHED' && (
          <>
            <p className="muted">Students can find and enroll in this course.</p>
            <button className="btn btn-line" disabled={busy}
              onClick={() => run(() => post('/unpublish'), 'Unpublished. It is hidden from the catalog.')}>Unpublish</button>
          </>
        )}
        {course.status === 'SUSPENDED' && (
          <p className="muted">An admin suspended this course. Ask an admin to reinstate it before you can publish again.</p>
        )}
      </section>

      <section className="panel">
        <h2>Details</h2>
        <CourseForm key={course.title + course.price} initial={course} submitLabel="Save details"
          onSubmit={async (body) => { await api(base, { method: 'PUT', body }); await load(); setNotice('Details saved.') }} />
      </section>

      <section className="panel">
        <h2>Lessons</h2>
        {course.lessons.length === 0 && <p className="muted">No lessons yet.</p>}
        <ul className="course-list">
          {course.lessons.map((l) => (
            <LessonItem key={l.id + l.title} lesson={l} busy={busy} canDelete={draft}
              onSave={(body) => run(() => api(`${base}/lessons/${l.id}`, { method: 'PUT', body }), 'Lesson saved.')}
              onDelete={() => window.confirm('Delete this lesson?') && run(() => api(`${base}/lessons/${l.id}`, { method: 'DELETE' }), 'Lesson deleted.')} />
          ))}
        </ul>
        {!draft && <p className="muted">To delete a lesson, unpublish the course first.</p>}
        <h3>Add a lesson</h3>
        <Field label="Title" maxLength={LIMITS.title} value={lesson.title} onChange={(e) => setLesson({ ...lesson, title: e.target.value })} />
        <label className="field">
          <span>Content</span>
          <textarea rows={5} maxLength={LIMITS.lesson} value={lesson.content} onChange={(e) => setLesson({ ...lesson, content: e.target.value })} />
          <small>{lesson.content.length} / {LIMITS.lesson}</small>
        </label>
        <button className="btn" disabled={busy || !lesson.title.trim()}
          onClick={async () => { if (await run(() => post('/lessons', lesson), 'Lesson added.')) setLesson({ title: '', content: '' }) }}>
          Add lesson
        </button>
      </section>

      <section className="panel">
        <h2>Quiz</h2>
        <p className="muted">Students must pass the quiz (if there is one) to finish the course and earn the certificate.</p>
        <Link className="btn btn-line" to={`/teach/${id}/quiz`}>Edit quiz</Link>
      </section>
    </>
  )
}

function LessonItem({ lesson, canDelete, busy, onSave, onDelete }) {
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({ title: lesson.title, content: lesson.content || '' })

  if (editing) {
    return (
      <li className="lesson-edit">
        <Field label="Title" maxLength={LIMITS.title} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        <label className="field">
          <span>Content</span>
          <textarea rows={5} maxLength={LIMITS.lesson} value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
          <small>{form.content.length} / {LIMITS.lesson}</small>
        </label>
        <div className="actions">
          <button className="btn" disabled={busy || !form.title.trim()} onClick={async () => { if (await onSave(form)) setEditing(false) }}>Save</button>
          <button className="btn btn-line" onClick={() => setEditing(false)}>Cancel</button>
        </div>
      </li>
    )
  }
  return (
    <li className="enroll-row">
      <div>
        <h2>{lesson.position}. {lesson.title}</h2>
        <p className="muted clamp">{lesson.content || 'No text yet.'}</p>
      </div>
      <div className="actions">
        <button className="btn btn-line" onClick={() => setEditing(true)}>Edit</button>
        {canDelete && <button className="btn btn-line" disabled={busy} onClick={onDelete}>Delete</button>}
      </div>
    </li>
  )
}
