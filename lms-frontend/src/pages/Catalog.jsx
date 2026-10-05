import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import CourseRow from '../components/CourseRow'

const PAGE_SIZE = 8

export default function Catalog() {
  // The search word and page live in the address bar, so Back and shared links work.
  const [params, setParams] = useSearchParams()
  const q = params.get('q') || ''
  const page = Math.max(1, Number(params.get('page')) || 1)
  const [text, setText] = useState(q)
  useEffect(() => setText(q), [q])

  const { data, error, loading } = useFetch(
    `/api/catalog?q=${encodeURIComponent(q)}&page=${page - 1}&size=${PAGE_SIZE}`,
  )

  function search(e) {
    e.preventDefault()
    setParams(text.trim() ? { q: text.trim() } : {})
  }

  function goTo(p) {
    const next = {}
    if (q) next.q = q
    if (p > 1) next.page = p
    setParams(next)
    window.scrollTo(0, 0)
  }

  const items = data?.items ?? []

  return (
    <>
      <h1>Courses</h1>
      <form className="search" role="search" onSubmit={search}>
        <input
          type="search"
          aria-label="Search courses"
          placeholder="Search by title, topic, or instructor"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button className="btn">Search</button>
        {q && <button type="button" className="btn btn-line" onClick={() => setParams({})}>Clear</button>}
      </form>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading && <p className="muted" aria-live="polite">Loading courses…</p>}

      {data && (
        <>
          <p className="muted count">
            {data.totalItems} {data.totalItems === 1 ? 'course' : 'courses'}
            {q && <> matching “{q}”</>}
          </p>
          {items.length === 0 ? (
            <p>{q ? 'No courses match that search. Try a different word.' : 'No courses are published yet. Check back soon.'}</p>
          ) : (
            <ul className="course-list">
              {items.map((c) => <CourseRow key={c.courseId} course={c} />)}
            </ul>
          )}
          {data.totalPages > 1 && (
            <nav className="pager" aria-label="Pages">
              <button className="btn btn-line" disabled={page <= 1} onClick={() => goTo(page - 1)}>Previous</button>
              <span className="muted">Page {page} of {data.totalPages}</span>
              <button className="btn btn-line" disabled={page >= data.totalPages} onClick={() => goTo(page + 1)}>Next</button>
            </nav>
          )}
        </>
      )}
    </>
  )
}
