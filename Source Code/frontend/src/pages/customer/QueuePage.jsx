import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import api from '../../api/client'

const STATUS_COLORS = {
  WAITING: 'warning', CALLED: 'primary', IN_SERVICE: 'success',
  COMPLETED: 'info', NO_SHOW: 'danger', LEFT: 'secondary'
}

/** Fix 6: alphanumeric token like SQ-001, distinct from numeric position */
const formatToken = (n) => `SQ-${String(n).padStart(3, '0')}`

export default function QueuePage() {
  const [entries, setEntries]   = useState([])
  const [services, setServices] = useState([])
  const [message, setMessage]   = useState(null)
  const location = useLocation()

  useEffect(() => {
    if (location.state?.success) {
      setMessage({ type: 'success', text: location.state.success })
      window.history.replaceState({}, '')
    }
    load()
    const interval = setInterval(load, 15000)
    return () => clearInterval(interval)
  }, [])

  const load = () => {
    api.get('/api/customer/queue').then(r => setEntries(r.data))
    api.get('/api/customer/services').then(r => setServices(r.data))
  }

  const join = async (serviceId) => {
    try {
      const r = await api.post('/api/customer/queue/join', { serviceId })
      setMessage({
        type: 'success',
        text: `Joined queue! Token: ${formatToken(r.data.queueNumber)} — Position: ${r.data.position}`
      })
      load()
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Could not join queue' })
    }
  }

  const active = entries.filter(e => ['WAITING', 'CALLED', 'IN_SERVICE'].includes(e.status))
  const past   = entries.filter(e => ['COMPLETED', 'NO_SHOW', 'LEFT'].includes(e.status))

  return (
    <>
      <h2 className="mb-4"><i className="bi bi-list-ol me-2 text-success"></i>Virtual Queue</h2>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      {/* Active entries */}
      {active.length > 0 && (
        <div className="mb-4">
          <h5 className="text-muted mb-3">Your Active Queue Positions</h5>
          <div className="row g-3">
            {active.map(e => (
              <div className="col-md-6" key={e.id}>
                <div className="card border-0 shadow-sm">
                  <div className="card-body text-center py-4">
                    <h6 className="text-muted mb-3">{e.serviceName}</h6>

                    {/* Fix 6: position shown large + bold */}
                    <div className="mb-1 text-muted small text-uppercase fw-semibold">Position in Queue</div>
                    <div style={{ fontSize: '4rem', fontWeight: 700, lineHeight: 1, color: '#0d6efd' }}>
                      {e.position}
                    </div>

                    {/* Alphanumeric token */}
                    <div className="mt-2 mb-3">
                      <span className="badge bg-secondary fs-6 px-3 py-2">
                        Token: {formatToken(e.queueNumber)}
                      </span>
                    </div>

                    <span className={`badge bg-${STATUS_COLORS[e.status]} fs-6`}>{e.status}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Join queue */}
      <h5 className="text-muted mb-3">Join a Queue</h5>
      <div className="row g-3 mb-4">
        {services.map(svc => (
          <div className="col-md-4" key={svc.id}>
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <h6 className="fw-semibold">{svc.serviceName}</h6>
                <p className="text-muted small mb-0"><i className="bi bi-clock me-1"></i>{svc.duration} min</p>
              </div>
              <div className="card-footer bg-transparent border-0 pb-3">
                <button className="btn btn-success btn-sm w-100" onClick={() => join(svc.id)}>
                  <i className="bi bi-plus-circle me-1"></i>Join Queue
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Past entries */}
      {past.length > 0 && (
        <>
          <h5 className="text-muted mb-3">Queue History</h5>
          <div className="card border-0 shadow-sm">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr><th>Service</th><th>Token</th><th>Status</th><th>Joined</th></tr>
                </thead>
                <tbody>
                  {past.map(e => (
                    <tr key={e.id}>
                      <td>{e.serviceName}</td>
                      <td><span className="badge bg-secondary">{formatToken(e.queueNumber)}</span></td>
                      <td><span className={`badge bg-${STATUS_COLORS[e.status]}`}>{e.status}</span></td>
                      <td className="text-muted small">{e.joinedAt.replace('T', ' ').slice(0, 16)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      <p className="text-muted small mt-3">
        <i className="bi bi-arrow-clockwise me-1"></i>Auto-refreshes every 15 seconds
      </p>
    </>
  )
}
