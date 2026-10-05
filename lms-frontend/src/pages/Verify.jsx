import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import { formatDate } from '../util'

// Public page: anyone can check a certificate code, no sign-in needed.
export default function Verify() {
  const { code } = useParams()
  const navigate = useNavigate()
  const [text, setText] = useState(code || '')
  const { data, error, loading } = useFetch(code ? `/api/certificates/verify/${encodeURIComponent(code)}` : null)

  return (
    <>
      <h1>Verify a certificate</h1>
      <form className="search" role="search" onSubmit={(e) => { e.preventDefault(); if (text.trim()) navigate(`/verify/${text.trim()}`) }}>
        <input aria-label="Certificate code" placeholder="Paste the certificate code" value={text} onChange={(e) => setText(e.target.value)} />
        <button className="btn">Verify</button>
      </form>
      {loading && <p className="muted" aria-live="polite">Checking…</p>}
      {error && <div className="notice notice-error" role="alert">No certificate was found for that code.</div>}
      {data && (
        <article className="cert">
          <p className="muted">Certificate of completion</p>
          <h2>{data.courseTitle}</h2>
          <p>Awarded to <strong>{data.holder}</strong></p>
          <p className="muted">Issued {formatDate(data.issuedAt)}</p>
          <p className="code">{data.code}</p>
        </article>
      )}
    </>
  )
}
