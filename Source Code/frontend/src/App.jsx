import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'

import LoginPage        from './pages/LoginPage'
import RegisterPage     from './pages/RegisterPage'

function RootRedirect() {
  const { user, loading } = useAuth()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace />
  if (user.role === 'ADMIN') return <Navigate to="/admin/dashboard" replace />
  if (user.role === 'STAFF') return <Navigate to="/staff/dashboard" replace />
  return <Navigate to="/customer/dashboard" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Navbar />
        <div className="container mt-4">
          <Routes>
            <Route path="/"         element={<RootRedirect />} />
            <Route path="/login"    element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}
