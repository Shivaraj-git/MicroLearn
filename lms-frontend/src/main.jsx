import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import App from './App'
import ErrorBoundary from './components/ErrorBoundary'
import './styles.css'

createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <AuthProvider>
      <ErrorBoundary><App /></ErrorBoundary>
    </AuthProvider>
  </BrowserRouter>,
)
