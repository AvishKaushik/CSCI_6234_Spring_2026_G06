import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function StaffDashboard() {
  const [services, setServices] = useState([])

  useEffect(() => {
    const load = () => api.get('/api/staff/services').then(r => setServices(r.data))
    load()
    const interval = setInterval(load, 10000)
    return () => clearInterval(interval)
  }, [])

  return (
    <>
      <h2 className="mb-1"><i className="bi bi-person-badge me-2 text-primary"></i>Staff Dashboard</h2>
      <p className="text-muted mb-4">Select a service queue to manage</p>

      <div className="row g-3">
        {services.map(svc => (
          <div className="col-md-4" key={svc.id}>
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body text-center py-4">
                <i className="bi bi-people fs-1 text-primary mb-2"></i>
                <h5 className="mb-1">{svc.serviceName}</h5>
                <div className="display-5 fw-bold text-warning">{svc.waitingCount}</div>
                <div className="text-muted small">customers waiting</div>
              </div>
              <div className="card-footer bg-transparent border-0 pb-3">
                <Link to={`/staff/queue/${svc.id}`}
                      className="btn btn-primary w-100">
                  <i className="bi bi-play-circle me-1"></i>Manage Queue
                </Link>
              </div>
            </div>
          </div>
        ))}
        {services.length === 0 && (
          <div className="col-12 text-center text-muted py-5">
            <i className="bi bi-inbox fs-1"></i>
            <p className="mt-2">No services available</p>
          </div>
        )}
      </div>
    </>
  )
}
