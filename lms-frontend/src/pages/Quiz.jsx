import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useFetch } from '../api/useFetch'

export default function Quiz() {
  const { courseId } = useParams()
  const { data: quiz, error, loading } = useFetch(`/api/quizzes/courses/${courseId}`)
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [submitError, setSubmitError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setSubmitError('')
    try {
      const list = Object.entries(answers).map(([q, o]) => ({ questionId: Number(q), optionId: Number(o) }))
      setResult(await api(`/api/quizzes/courses/${courseId}/attempts`, { method: 'POST', body: { answers: list } }))
    } catch (err) {
      setSubmitError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <p className="muted" aria-live="polite">Loading…</p>
  if (error) return (<><div className="notice notice-error" role="alert">{error}</div><Link to={`/learn/${courseId}`}>Back to the course</Link></>)

  if (result) {
    return (
      <>
        <h1>{result.passed ? 'You passed' : 'Not quite'}</h1>
        <div className="panel">
          <p>You got {result.correctAnswers} of {result.totalQuestions} right ({result.scorePercent}%). The pass mark is {result.passMark}%.</p>
          <div className="actions">
            {!result.passed && <button className="btn" onClick={() => { setResult(null); setAnswers({}) }}>Try again</button>}
            <Link className="btn btn-line" to={`/learn/${courseId}`}>Back to the course</Link>
            {result.passed && <Link className="btn btn-line" to="/certificates">Certificates</Link>}
          </div>
        </div>
      </>
    )
  }

  return (
    <>
      <p><Link to={`/learn/${courseId}`}>Back to the course</Link></p>
      <h1>Quiz</h1>
      <p className="muted">Pass mark: {quiz.passMark}%. Choose one answer for each question.</p>
      <form onSubmit={submit}>
        {quiz.questions.map((q, i) => (
          <fieldset className="q" key={q.id}>
            <legend>{i + 1}. {q.text}</legend>
            {q.options.map((o) => (
              <label key={o.id}>
                <input type="radio" name={`q${q.id}`} checked={answers[q.id] === o.id} onChange={() => setAnswers({ ...answers, [q.id]: o.id })} /> {o.text}
              </label>
            ))}
          </fieldset>
        ))}
        {submitError && <div className="notice notice-error" role="alert">{submitError}</div>}
        <button className="btn" disabled={busy || Object.keys(answers).length < quiz.questions.length}>
          {busy ? 'Grading…' : 'Submit answers'}
        </button>
      </form>
    </>
  )
}
