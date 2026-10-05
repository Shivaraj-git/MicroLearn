import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import CourseForm from '../components/CourseForm'

export default function NewCourse() {
  const navigate = useNavigate()

  async function create(body) {
    const res = await api('/api/courses', { method: 'POST', body })
    navigate(`/teach/${res.courseId}`)   // next: add lessons and publish
  }

  return (
    <div className="auth">
      <p><Link to="/teach">Back to my courses</Link></p>
      <h1>New course</h1>
      <div className="panel"><CourseForm submitLabel="Create course" onSubmit={create} /></div>
    </div>
  )
}
