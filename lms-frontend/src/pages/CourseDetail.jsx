import { Link, useParams } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import EnrollAction from '../components/EnrollAction'
import { formatPrice, lessonsLabel } from '../util'

export default function CourseDetail() {
  const { id } = useParams()
  const { data: course, error, loading } = useFetch(`/api/catalog/${id}`)

  if (loading) return <p className="muted" aria-live="polite">Loading…</p>
  if (error) {
    return (
      <>
        <div className="notice notice-error" role="alert">{error}</div>
        <Link to="/courses">Back to courses</Link>
      </>
    )
  }

  return (
    <>
      <p><Link to="/courses">Back to courses</Link></p>
      <article className="detail">
        <div>
          <h1>{course.title}</h1>
          <p className="lead">{course.description || 'No description yet.'}</p>
          <dl className="facts">
            <dt>Instructor</dt><dd>{course.instructorName || course.instructorEmail}</dd>
            <dt>Length</dt><dd>{lessonsLabel(course.lessonCount)}</dd>
          </dl>
        </div>
        <aside className="panel buy">
          <strong className="price">{formatPrice(course.price)}</strong>
          <EnrollAction course={course} />
        </aside>
      </article>
    </>
  )
}
