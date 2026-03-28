import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Modal } from 'bootstrap'
import api from '../../api/client'

const closeModal = (id) => {
  const el = document.getElementById(id)
  if (el) Modal.getOrCreateInstance(el).hide()
}

// Returns today's date in YYYY-MM-DD using local timezone (not UTC)
const localToday = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default function ServicesPage() {
  const [services, setServices]         = useState([])
  const [selected, setSelected]         = useState(null)
  const [date, setDate]                 = useState('')
  const [timeSlot, setTimeSlot]         = useState('')
  const [availableSlots, setAvailableSlots] = useState([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [message, setMessage]           = useState(null)
  const [loading, setLoading]           = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/api/customer/services').then(r => setServices(r.data))
    setDate(localToday())
  }, [])

  // Fetch available slots whenever service or date changes
  useEffect(() => {
    if (!selected || !date) return
    setSlotsLoading(true)
    setTimeSlot('')
    api.get(`/api/customer/services/${selected.id}/available-slots?date=${date}`)
      .then(r => setAvailableSlots(r.data))
      .catch(() => setAvailableSlots([]))
      .finally(() => setSlotsLoading(false))
  }, [selected, date])

  const openBook = (svc) => {
    setSelected(svc)
    setMessage(null)
    setTimeSlot('')
    setAvailableSlots([])
  }

  const joinQueue = async (svc) => {
    try {
      const r = await api.post('/api/customer/queue/join', { serviceId: svc.id })
      navigate('/customer/queue', {
        state: { success: `Joined queue for ${svc.serviceName}. Queue #${r.data.queueNumber}, Position: ${r.data.position}` }
      })
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Could not join queue' })
    }
  }

  const submitBooking = async (e) => {
    e.preventDefault()
    if (!selected) return
    setLoading(true)
    try {
      await api.post('/api/customer/appointments', {
        serviceId: String(selected.id),
        date,
        timeSlot
      })
      closeModal('bookModal')
      setMessage({ type: 'success', text: `Appointment booked for ${selected.serviceName} on ${date} at ${timeSlot}. You will be added to the queue automatically at your appointment time.` })
      setSelected(null)
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Booking failed — slot may be unavailable' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <h2 className="mb-4"><i className="bi bi-grid me-2 text-primary"></i>Available Services</h2>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      <div className="row g-3">
        {services.map(svc => (
          <div className="col-md-6 col-lg-4" key={svc.id}>
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <div className="d-flex align-items-center mb-3">
                  <div className="rounded-circle bg-primary-subtle d-flex align-items-center justify-content-center"
                       style={{ width: 48, height: 48 }}>
                    <i className="bi bi-gear text-primary fs-5"></i>
                  </div>
                  <div className="ms-3">
                    <h5 className="mb-0">{svc.serviceName}</h5>
                    <small className="text-muted"><i className="bi bi-clock me-1"></i>{svc.duration} min</small>
                  </div>
                </div>
                {svc.description && <p className="text-muted small">{svc.description}</p>}
              </div>
              <div className="card-footer bg-transparent border-0 pb-3">
                <div className="d-flex gap-2">
                  <button className="btn btn-primary btn-sm flex-fill"
                          data-bs-toggle="modal" data-bs-target="#bookModal"
                          onClick={() => openBook(svc)}>
                    <i className="bi bi-calendar-plus me-1"></i>Book Appointment
                  </button>
                  <button className="btn btn-outline-success btn-sm flex-fill"
                          onClick={() => joinQueue(svc)}>
                    <i className="bi bi-list-ol me-1"></i>Join Queue
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
        {services.length === 0 && (
          <div className="col-12 text-center text-muted py-5">
            <i className="bi bi-inbox fs-1"></i>
            <p className="mt-2">No services available.</p>
          </div>
        )}
      </div>

      {/* Book Appointment Modal */}
      <div className="modal fade" id="bookModal" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header bg-primary text-white">
              <h5 className="modal-title">
                <i className="bi bi-calendar-plus me-2"></i>
                Book — {selected?.serviceName}
              </h5>
              <button type="button" className="btn-close btn-close-white"
                      data-bs-dismiss="modal"></button>
            </div>
            <form onSubmit={submitBooking}>
              <div className="modal-body">
                <div className="mb-3">
                  <label className="form-label fw-semibold">Date</label>
                  <input type="date" className="form-control" value={date}
                         min={localToday()}
                         onChange={e => setDate(e.target.value)} required />
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Time Slot</label>
                  {slotsLoading ? (
                    <div className="d-flex align-items-center gap-2 text-muted small py-2">
                      <span className="spinner-border spinner-border-sm"></span> Loading available slots…
                    </div>
                  ) : (
                    <select className="form-select" value={timeSlot}
                            onChange={e => setTimeSlot(e.target.value)} required>
                      <option value="">Select a time…</option>
                      {availableSlots.map(t => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>
                  )}
                  {!slotsLoading && availableSlots.length === 0 && (
                    <div className="text-danger small mt-1">
                      <i className="bi bi-exclamation-circle me-1"></i>No available slots for this date.
                    </div>
                  )}
                </div>
                <p className="text-muted small mb-0">
                  <i className="bi bi-info-circle me-1"></i>
                  Only available slots are shown. Each slot accepts one booking per service.
                </p>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary"
                        data-bs-dismiss="modal">Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading && <span className="spinner-border spinner-border-sm me-1"></span>}
                  Confirm Booking
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}
