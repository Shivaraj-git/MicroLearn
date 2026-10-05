import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function Learn() {
  const { courseId } = useParams()
  const [course, setCourse] = useState(null)
  const [progress, setProgress] = useState(null)
  const [hasQuiz, setHasQuiz] = useState(false)
  const [current, setCurrent] = useState(1)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    Promise.all([api(`/api/learn/courses/${courseId}`), api(`/api/progress/courses/${courseId}`)])
      .then(([c, p]) => {
        setCourse(c)
        setProgress(p)
        setCurrent(c.lessons.find((l) => !p.completedLessons.includes(l.position))?.position ?? 1)
      })
      .catch((e) => setError(e.message))
    api(`/api/quizzes/courses/${courseId}`).then(() => setHasQuiz(true)).catch(() => {})
  }, [courseId])

  if (error) {
    return (
      <>
        <div className="notice notice-error" role="alert">{error}</div>
        <Link to="/enrollments">Back to my enrollments</Link>
      </>
    )
  }
  if (!course || !progress) return <p className="muted" aria-live="polite">Loading…</p>

  const done = progress.completedLessons
  const total = course.lessons.length
  const lesson = course.lessons.find((l) => l.position === current) || course.lessons[0]
  const lessonDone = done.includes(lesson.position)

  async function complete() {
    setBusy(true)
    setActionError('')
    try {
      setProgress(await api(`/api/progress/courses/${courseId}/lessons/${lesson.position}/complete`, { method: 'POST' }))
      if (lesson.position < total) setCurrent(lesson.position + 1)
    } catch (e) {
      setActionError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p><Link to="/enrollments">Back to my enrollments</Link></p>
      <h1>{course.title}</h1>
      <p className="muted">{done.length} of {total} lessons done</p>
      <div className="bar-track"><div className="bar-fill" style={{ width: `${(done.length / total) * 100}%` }} /></div>

      {progress.courseCompleted && (
        <div className="notice notice-ok">
          Course completed. Your certificate will appear under <Link to="/certificates">Certificates</Link> in a few seconds.
        </div>
      )}
      {done.length >= total && !progress.courseCompleted && hasQuiz && (
        <div className="notice notice-ok">All lessons are done. Pass the quiz to finish the course.</div>
      )}

      <div className="learn">
        <ul className="lessons">
          {course.lessons.map((l) => (
            <li key={l.position}>
              <button aria-current={l.position === lesson.position ? 'true' : undefined} onClick={() => setCurrent(l.position)}>
                <span>{l.position}. {l.title}</span>
                {done.includes(l.position) && <span className="done">Done</span>}
              </button>
            </li>
          ))}
        </ul>
        <section>
          <h2>{lesson.title}</h2>
          <div className="content">{lesson.content || 'This lesson has no text yet.'}</div>
          {actionError && <div className="notice notice-error" role="alert">{actionError}</div>}
          <div className="actions">
            <button className="btn" disabled={busy || lessonDone} onClick={complete}>
              {lessonDone ? 'Completed' : busy ? 'Saving…' : 'Mark as complete'}
            </button>
            {hasQuiz && <Link className="btn btn-line" to={`/learn/${courseId}/quiz`}>Take the quiz</Link>}
          </div>
        </section>
      </div>
    </>
  )
}
