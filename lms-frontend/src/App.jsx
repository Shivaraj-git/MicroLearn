import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Catalog from './pages/Catalog'
import CourseDetail from './pages/CourseDetail'
import MyEnrollments from './pages/MyEnrollments'
import Notifications from './pages/Notifications'
import ProtectedRoute from './auth/ProtectedRoute'
import Learn from './pages/Learn'
import Quiz from './pages/Quiz'
import Certificates from './pages/Certificates'
import Verify from './pages/Verify'
import MyCourses from './pages/MyCourses'
import NewCourse from './pages/NewCourse'
import ManageCourse from './pages/ManageCourse'
import QuizBuilder from './pages/QuizBuilder'
import AdminUsers from './pages/AdminUsers'
import AdminCourses from './pages/AdminCourses'
import AdminEnrollments from './pages/AdminEnrollments'
import Profile from './pages/Profile'
import EditProfile from './pages/EditProfile'
import NotFound from './pages/NotFound'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="courses" element={<Catalog />} />
        <Route path="courses/:id" element={<CourseDetail />} />
        <Route path="enrollments" element={<ProtectedRoute roles={['STUDENT']}><MyEnrollments /></ProtectedRoute>} />
        <Route path="notifications" element={<ProtectedRoute roles={['STUDENT']}><Notifications /></ProtectedRoute>} />
        <Route path="learn/:courseId" element={<ProtectedRoute roles={['STUDENT']}><Learn /></ProtectedRoute>} />
        <Route path="learn/:courseId/quiz" element={<ProtectedRoute roles={['STUDENT']}><Quiz /></ProtectedRoute>} />
        <Route path="certificates" element={<ProtectedRoute roles={['STUDENT']}><Certificates /></ProtectedRoute>} />
        <Route path="verify" element={<Verify />} />
        <Route path="verify/:code" element={<Verify />} />
        <Route path="teach" element={<ProtectedRoute roles={['INSTRUCTOR']}><MyCourses /></ProtectedRoute>} />
        <Route path="teach/new" element={<ProtectedRoute roles={['INSTRUCTOR']}><NewCourse /></ProtectedRoute>} />
        <Route path="teach/:id" element={<ProtectedRoute roles={['INSTRUCTOR']}><ManageCourse /></ProtectedRoute>} />
        <Route path="teach/:id/quiz" element={<ProtectedRoute roles={['INSTRUCTOR']}><QuizBuilder /></ProtectedRoute>} />
        <Route path="admin/users" element={<ProtectedRoute roles={['ADMIN']}><AdminUsers /></ProtectedRoute>} />
        <Route path="admin/courses" element={<ProtectedRoute roles={['ADMIN']}><AdminCourses /></ProtectedRoute>} />
        <Route path="admin/enrollments" element={<ProtectedRoute roles={['ADMIN']}><AdminEnrollments /></ProtectedRoute>} />
        <Route path="profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
        <Route path="profile/edit" element={<ProtectedRoute><EditProfile /></ProtectedRoute>} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        {/* later steps add their routes here */}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
