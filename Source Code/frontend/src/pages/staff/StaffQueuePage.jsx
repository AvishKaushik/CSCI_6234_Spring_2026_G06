import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api from '../../api/client'

const STATUS_COLORS = {
  WAITING: 'warning', CALLED: 'primary', IN_SERVICE: 'success',
  COMPLETED: 'info', NO_SHOW: 'danger'
}

const formatToken = (n) => `SQ-${String(n).padStart(3, '0')}`

export default function StaffQueuePage() {
  const { serviceId } = useParams()
  const [entries, setEntries]         = useState([])
  const [message, setMessage]         = useState(null)
  const [serviceName, setServiceName] = useState('')

  const load = () => {
    api.get(`/api/staff/queue/${serviceId}`).then(r => setEntries(r.data))
    api.get('/api/staff/services').then(r => {
      const svc = r.data.find(s => String(s.id) === String(serviceId))
      if (svc) setServiceName(svc.serviceName)
    })
  }

  useEffect(() => {
    load()
    const interval = setInterval(load, 10000)
    return () => clearInterval(interval)
  }, [serviceId])

  const callNext = async () => {
    try {
      const r = await api.post(`/api/staff/queue/${serviceId}/call-next`)
      if (r.data.id) {
        const apptNote = r.data.hasAppointment ? ' (appointment priority)' : ''
        setMessage({
          type: 'success',
          text: `Called: ${r.data.customerName} [${formatToken(r.data.queueNumber)}]${apptNote}`
        })
      } else {
        setMessage({ type: 'info', text: r.data.message })
      }
      load()
    } catch (err) {
      if (err.response?.status === 409) {
        setMessage({ type: 'warning', text: `⚠️ ${err.response.data.error}` })
      } else {
        setMessage({ type: 'danger', text: 'Error calling next customer' })
      }
    }
  }

  const complete = async (entryId) => {
    await api.post(`/api/staff/entries/${entryId}/complete`)
    setMessage({ type: 'success', text: 'Service completed' })
    load()
  }

  const noShow = async (entryId) => {
    await api.post(`/api/staff/entries/${entryId}/no-show`)
    setMessage({ type: 'warning', text: 'Marked as no-show' })
    load()
  }

  const waiting   = entries.filter(e => e.status === 'WAITING')
  const inService = entries.find(e => e.status === 'IN_SERVICE' || e.status === 'CALLED')
  const done      = entries.filter(e => ['COMPLETED', 'NO_SHOW'].includes(e.status))

  return (
    <>
      <div className="d-flex align-items-center gap-3 mb-4">
        <Link to="/staff/dashboard" className="btn btn-outline-secondary btn-sm">
          <i className="bi bi-arrow-left me-1"></i>Back
        </Link>
        <h2 className="mb-0">
          <i className="bi bi-list-ul me-2 text-primary"></i>{serviceName}
        </h2>
        <span className="badge bg-warning text-dark fs-6">{waiting.length} waiting</span>
      </div>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      <div className="row g-4">
        {/* Left: current + call next */}
        <div className="col-md-5">
          <div className="card border-0 shadow-sm mb-3">
            <div className="card-header bg-success text-white">
              <i className="bi bi-person-check me-2"></i>Currently Serving
            </div>
            <div className="card-body text-center py-4">
              {inService ? (
                <>
                  <div className="text-muted small text-uppercase fw-semibold mb-1">Position</div>
                  <div style={{ fontSize: '3rem', fontWeight: 700, color: '#198754', lineHeight: 1 }}>
                    {inService.position}
                  </div>
                  <div className="mt-1 mb-1">
                    <span className="badge bg-secondary">{formatToken(inService.queueNumber)}</span>
                  </div>
                  <h5 className="mt-2">{inService.customerName}</h5>
                  {inService.hasAppointment && (
                    <span className="badge bg-info text-dark mb-2">
                      <i className="bi bi-calendar-check me-1"></i>Has Appointment
                    </span>
                  )}
                  {inService.vip && !inService.hasAppointment && (
                    <span className="badge bg-danger mb-2">VIP</span>
                  )}
                  <div className="d-flex gap-2 justify-content-center mt-3">
                    <button className="btn btn-success" onClick={() => complete(inService.id)}>
                      <i className="bi bi-check-circle me-1"></i>Complete
                    </button>
                  </div>
                </>
              ) : (
                <p className="text-muted mb-0">No one currently being served</p>
              )}
            </div>
          </div>

          <button className="btn btn-primary w-100 py-3 fs-5" onClick={callNext}
                  disabled={waiting.length === 0 || !!inService}>
            <i className="bi bi-megaphone me-2"></i>Call Next Customer
          </button>

          {inService && (
            <div className="alert alert-warning mt-2 py-2 small">
              <i className="bi bi-lock me-1"></i>
              Complete or mark no-show for the current customer before calling next.
            </div>
          )}

          {!inService && waiting.some(e => e.hasAppointment) && (
            <div className="alert alert-info mt-2 py-2 small">
              <i className="bi bi-calendar-check me-1"></i>
              Customers with appointments will be called first automatically.
            </div>
          )}
        </div>

        {/* Right: waiting list */}
        <div className="col-md-7">
          <div className="card border-0 shadow-sm">
            <div className="card-header">
              <i className="bi bi-people me-2"></i>Waiting Queue ({waiting.length})
            </div>
            {waiting.length === 0 ? (
              <div className="card-body text-center text-muted py-4">
                <i className="bi bi-inbox fs-2"></i>
                <p className="mt-2 mb-0">Queue is empty</p>
              </div>
            ) : (
              <div className="list-group list-group-flush">
                {waiting.map(e => (
                  <div key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                      {/* Fix 6: position bold + big, token alphanumeric */}
                      <span className="fw-bold fs-5 text-primary me-2">{e.position}</span>
                      <span className="badge bg-secondary me-2">{formatToken(e.queueNumber)}</span>
                      <strong>{e.customerName}</strong>
                      {e.hasAppointment && (
                        <span className="badge bg-info text-dark ms-2">
                          <i className="bi bi-calendar-check me-1"></i>Appt
                        </span>
                      )}
                      {e.vip && !e.hasAppointment && (
                        <span className="badge bg-danger ms-2">VIP</span>
                      )}
                    </div>
                    <button className="btn btn-outline-danger btn-sm"
                            onClick={() => noShow(e.id)}>
                      No-Show
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {done.length > 0 && (
            <div className="card border-0 shadow-sm mt-3">
              <div className="card-header text-muted">Completed Today ({done.length})</div>
              <div className="list-group list-group-flush">
                {done.slice(0, 5).map(e => (
                  <div key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                    <span>
                      <span className="badge bg-secondary me-2">{formatToken(e.queueNumber)}</span>
                      {e.customerName}
                    </span>
                    <span className={`badge bg-${STATUS_COLORS[e.status]}`}>{e.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      <p className="text-muted small mt-3">
        <i className="bi bi-arrow-clockwise me-1"></i>Auto-refreshes every 10 seconds
      </p>
    </>
  )
}
