import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/api/admin/dashboard').then(r => setStats(r.data))
  }, [])

  return (
    <>
      <h2 className="mb-1"><i className="bi bi-shield-check me-2 text-danger"></i>Admin Dashboard</h2>
      <p className="text-muted mb-4">System overview and management</p>

      {stats && (
        <div className="row g-3 mb-4">
          <div className="col-sm-6">
            <div className="card border-0 bg-primary-subtle text-center p-4">
              <div className="display-4 fw-bold text-primary">{stats.serviceCount}</div>
              <div className="text-muted">Services</div>
            </div>
          </div>
          <div className="col-sm-6">
            <div className="card border-0 bg-success-subtle text-center p-4">
              <div className="display-4 fw-bold text-success">{stats.staffCount}</div>
              <div className="text-muted">Staff Members</div>
            </div>
          </div>
        </div>
      )}

      <div className="row g-3">
        <div className="col-md-6">
          <Link to="/admin/services" className="card border-0 shadow-sm p-4 text-center text-decoration-none text-dark d-block">
            <i className="bi bi-gear fs-1 text-primary"></i>
            <h5 className="mt-3">Manage Services</h5>
            <p className="text-muted small mb-0">Add, edit or remove services</p>
          </Link>
        </div>
        <div className="col-md-6">
          <Link to="/admin/staff" className="card border-0 shadow-sm p-4 text-center text-decoration-none text-dark d-block">
            <i className="bi bi-people fs-1 text-success"></i>
            <h5 className="mt-3">Manage Staff</h5>
            <p className="text-muted small mb-0">Create and manage staff accounts</p>
          </Link>
        </div>
      </div>
    </>
  )
}
