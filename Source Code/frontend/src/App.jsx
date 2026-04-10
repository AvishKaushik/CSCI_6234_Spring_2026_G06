import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'

import LoginPage        from './pages/LoginPage'
import RegisterPage     from './pages/RegisterPage'

import CustomerDashboard  from './pages/customer/CustomerDashboard'
import ServicesPage       from './pages/customer/ServicesPage'
import AppointmentsPage   from './pages/customer/AppointmentsPage'
import QueuePage          from './pages/customer/QueuePage'

import StaffDashboard   from './pages/staff/StaffDashboard'
import StaffQueuePage   from './pages/staff/StaffQueuePage'

import AdminDashboard   from './pages/admin/AdminDashboard'
import AdminServicesPage from './pages/admin/AdminServicesPage'
import AdminStaffPage   from './pages/admin/AdminStaffPage'

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

            <Route path="/customer/dashboard" element={
              <ProtectedRoute role="CUSTOMER"><CustomerDashboard /></ProtectedRoute>} />
            <Route path="/customer/services" element={
              <ProtectedRoute role="CUSTOMER"><ServicesPage /></ProtectedRoute>} />
            <Route path="/customer/appointments" element={
              <ProtectedRoute role="CUSTOMER"><AppointmentsPage /></ProtectedRoute>} />
            <Route path="/customer/queue" element={
              <ProtectedRoute role="CUSTOMER"><QueuePage /></ProtectedRoute>} />

            <Route path="/staff/dashboard" element={
              <ProtectedRoute role="STAFF"><StaffDashboard /></ProtectedRoute>} />
            <Route path="/staff/queue/:serviceId" element={
              <ProtectedRoute role="STAFF"><StaffQueuePage /></ProtectedRoute>} />

            <Route path="/admin/dashboard" element={
              <ProtectedRoute role="ADMIN"><AdminDashboard /></ProtectedRoute>} />
            <Route path="/admin/services" element={
              <ProtectedRoute role="ADMIN"><AdminServicesPage /></ProtectedRoute>} />
            <Route path="/admin/staff" element={
              <ProtectedRoute role="ADMIN"><AdminStaffPage /></ProtectedRoute>} />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}
