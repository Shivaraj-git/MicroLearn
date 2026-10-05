import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, tokenStore, setUnauthorizedHandler } from '../api/client'

const AuthContext = createContext(null)
const USER_KEY = 'lms.user'

function tokenExpired(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)).exp * 1000 < Date.now()
  } catch {
    return true
  }
}

function loadUser() {
  const token = tokenStore.get()
  if (!token || tokenExpired(token)) return null
  try { return JSON.parse(localStorage.getItem(USER_KEY)) } catch { return null }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadUser)

  const signOut = useCallback(() => {
    tokenStore.clear()
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  useEffect(() => { setUnauthorizedHandler(signOut) }, [signOut])

  function accept(res) {
    const u = { id: res.userId, name: res.name, role: res.role }
    tokenStore.set(res.token)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
    return u
  }

  const signIn = async (email, password) =>
    accept(await api('/api/auth/login', { method: 'POST', body: { email, password }, auth: false }))

  const register = async (form) =>
    accept(await api('/api/auth/register', { method: 'POST', body: form, auth: false }))

  return (
    <AuthContext.Provider value={{ user, signIn, register, signOut, applyAuth: accept }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
