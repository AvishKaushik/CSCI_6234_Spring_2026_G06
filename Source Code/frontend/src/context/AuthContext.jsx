import { createContext, useContext, useState, useEffect } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser]       = useState(null)
  const [loading, setLoading] = useState(true)

  // Restore auth state on page load
  useEffect(() => {
    api.get('/api/auth/me')
      .then(r => setUser(r.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  /**
   * JSON login — sends credentials as JSON to /api/auth/login.
   * This avoids Spring Security's form-login endpoint at /login which would
   * return raw JSON that the browser displays as a page.
   */
  const login = async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password })
    if (!res.data.authenticated) throw new Error('Login failed')
    // Fetch full user details (id, name, role)
    const me = await api.get('/api/auth/me')
    setUser(me.data)
    return me.data
  }

  const logout = async () => {
    try { await api.post('/api/auth/logout') } catch { /* ignore */ }
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
