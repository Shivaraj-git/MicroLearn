export const roleLabel = (role) =>
  ({ STUDENT: 'Student', INSTRUCTOR: 'Instructor', ADMIN: 'Admin' })[role] || role

// Prices come from the backend as plain numbers. Change 'INR' here to switch currency.
export const formatPrice = (price) =>
  Number(price) === 0
    ? 'Free'
    : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(price)

export const lessonsLabel = (n) => `${n} ${n === 1 ? 'lesson' : 'lessons'}`

export const formatDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : ''
