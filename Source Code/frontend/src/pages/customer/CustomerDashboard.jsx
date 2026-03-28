import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import api from '../../api/client'

export default function CustomerDashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/api/customer/dashboard').then(r => setStats(r.data))
  }, [])

  return (
    <>
      <h2 className="mb-1">Welcome back, {user?.name}</h2>
      <p className="text-muted mb-4">What would you like to do today?</p>

      {stats && (
        <div className="row g-3 mb-4">
          <div className="col-sm-4">
            <div className="card border-0 bg-primary-subtle text-center p-3">
              <div className="fs-1 fw-bold text-primary">{stats.confirmedAppointments}</div>
              <div className="small text-muted">Upcoming Appointments</div>
            </div>
          </div>
          <div className="col-sm-4">
            <div className="card border-0 bg-success-subtle text-center p-3">
              <div className="fs-1 fw-bold text-success">{stats.activeQueues}</div>
              <div className="small text-muted">Active Queue Entries</div>
            </div>
          </div>
          <div className="col-sm-4">
            <div className="card border-0 bg-warning-subtle text-center p-3">
              <div className="fs-1 fw-bold text-warning">{stats.totalAppointments}</div>
              <div className="small text-muted">Total Appointments</div>
            </div>
          </div>
        </div>
      )}

      <div className="row g-3">
        <div className="col-md-4">
          <Link to="/customer/services" className="card border-0 shadow-sm p-4 text-center text-decoration-none text-dark d-block hover-card">
            <i className="bi bi-calendar-plus fs-1 text-primary"></i>
            <h5 className="mt-3">Book Appointment</h5>
            <p className="text-muted small mb-0">Schedule a service appointment</p>
          </Link>
        </div>
        <div className="col-md-4">
          <Link to="/customer/queue" className="card border-0 shadow-sm p-4 text-center text-decoration-none text-dark d-block hover-card">
            <i className="bi bi-list-ol fs-1 text-success"></i>
            <h5 className="mt-3">Join Queue</h5>
            <p className="text-muted small mb-0">Join a virtual queue now</p>
          </Link>
        </div>
        <div className="col-md-4">
          <Link to="/customer/appointments" className="card border-0 shadow-sm p-4 text-center text-decoration-none text-dark d-block hover-card">
            <i className="bi bi-calendar-check fs-1 text-info"></i>
            <h5 className="mt-3">My Appointments</h5>
            <p className="text-muted small mb-0">View and manage your bookings</p>
          </Link>
        </div>
      </div>
    </>
  )
}
