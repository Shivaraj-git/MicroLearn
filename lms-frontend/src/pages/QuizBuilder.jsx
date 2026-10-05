import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'

const blank = () => ({ text: '', options: [{ text: '', correct: true }, { text: '', correct: false }] })

export default function QuizBuilder() {
  const { id } = useParams()
  const [passMark, setPassMark] = useState(60)
  const [questions, setQuestions] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api(`/api/quizzes/courses/${id}/manage`)
      .then((q) => {
        setPassMark(q.passMark)
        setQuestions(q.questions.map((x) => ({ text: x.text, options: x.options.map((o) => ({ text: o.text, correct: o.correct })) })))
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404 && /no quiz/i.test(e.message)) setQuestions([blank()])
        else setError(e.message)
      })
  }, [id])

  if (!questions) {
    return error
      ? <><div className="notice notice-error" role="alert">{error}</div><Link to={`/teach/${id}`}>Back to the course</Link></>
      : <p className="muted" aria-live="polite">Loading…</p>
  }

  const setQ = (i, patch) => setQuestions((qs) => qs.map((q, j) => (j === i ? { ...q, ...patch } : q)))
  const setOpt = (i, k, patch) => setQ(i, { options: questions[i].options.map((o, j) => (j === k ? { ...o, ...patch } : o)) })
  const setCorrect = (i, k) => setQ(i, { options: questions[i].options.map((o, j) => ({ ...o, correct: j === k })) })
  const addOption = (i) => setQ(i, { options: [...questions[i].options, { text: '', correct: false }] })
  const removeOption = (i, k) => {
    const opts = questions[i].options.filter((_, j) => j !== k)
    if (!opts.some((o) => o.correct)) opts[0] = { ...opts[0], correct: true }
    setQ(i, { options: opts })
  }

  async function save(e) {
    e.preventDefault()
    setError('')
    setNotice('')
    if (questions.some((q) => !q.text.trim() || q.options.some((o) => !o.text.trim()))) {
      setError('Fill in every question and every option, or remove the empty ones.')
      return
    }
    setBusy(true)
    try {
      await api(`/api/quizzes/courses/${id}`, { method: 'POST', body: { passMark: Number(passMark), questions } })
      setNotice('Quiz saved.')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p><Link to={`/teach/${id}`}>Back to the course</Link></p>
      <h1>Quiz</h1>
      <p className="muted">One correct answer per question, 2 to 6 options each. Saving replaces the current quiz.</p>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-ok" role="status">{notice}</div>}
      <form onSubmit={save}>
        <label className="field" style={{ maxWidth: '12rem' }}>
          <span>Pass mark (%)</span>
          <input type="number" min="1" max="100" required value={passMark} onChange={(e) => setPassMark(e.target.value)} />
        </label>

        {questions.map((q, i) => (
          <fieldset className="q" key={i}>
            <legend>Question {i + 1}</legend>
            <label className="field">
              <span>Question</span>
              <input value={q.text} onChange={(e) => setQ(i, { text: e.target.value })} />
            </label>
            {q.options.map((o, k) => (
              <div className="opt" key={k}>
                <input type="radio" name={`correct-${i}`} checked={o.correct} onChange={() => setCorrect(i, k)} aria-label={`Option ${k + 1} is correct`} />
                <input value={o.text} placeholder={`Option ${k + 1}`} aria-label={`Option ${k + 1} text`} onChange={(e) => setOpt(i, k, { text: e.target.value })} />
                {q.options.length > 2 && <button type="button" className="btn btn-quiet" onClick={() => removeOption(i, k)}>Remove</button>}
              </div>
            ))}
            <div className="actions">
              {q.options.length < 6 && <button type="button" className="btn btn-line" onClick={() => addOption(i)}>Add option</button>}
              {questions.length > 1 && <button type="button" className="btn btn-quiet" onClick={() => setQuestions(questions.filter((_, j) => j !== i))}>Remove question</button>}
            </div>
          </fieldset>
        ))}

        <div className="actions">
          <button type="button" className="btn btn-line" onClick={() => setQuestions([...questions, blank()])}>Add question</button>
          <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save quiz'}</button>
        </div>
      </form>
    </>
  )
}
