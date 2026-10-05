import { Link } from 'react-router-dom'
import { formatPrice, lessonsLabel } from '../util'

export default function CourseRow({ course }) {
  return (
    <li>
      <Link className="course-row" to={`/courses/${course.courseId}`}>
        <div>
          <h2>{course.title}</h2>
          <p className="clamp">{course.description || 'No description yet.'}</p>
          <span className="muted">by {course.instructorName || course.instructorEmail}</span>
        </div>
        <div className="course-meta">
          <strong>{formatPrice(course.price)}</strong>
          <span className="muted">{lessonsLabel(course.lessonCount)}</span>
        </div>
      </Link>
    </li>
  )
}
