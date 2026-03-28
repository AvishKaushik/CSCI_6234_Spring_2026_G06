import { useEffect, useState } from 'react'
import api from '../../api/client'

const STATUS_COLORS = {
  CONFIRMED: 'success', CANCELLED: 'secondary', COMPLETED: 'info', NO_SHOW: 'danger'
}

export default function AppointmentsPage() {
  const [appointments, setAppointments] = useState([])
  const [message, setMessage]           = useState(null)

  const load = () => api.get('/api/customer/appointments').then(r => setAppointments(r.data))
  useEffect(() => { load() }, [])

  const cancel = async (id) => {
    if (!window.confirm('Cancel this appointment?')) return
    try {
      await api.delete(`/api/customer/appointments/${id}`)
      setMessage({ type: 'success', text: 'Appointment cancelled.' })
      load()
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Could not cancel' })
    }
  }

  return (
    <>
      <h2 className="mb-4"><i className="bi bi-calendar-check me-2 text-info"></i>My Appointments</h2>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      {appointments.length === 0 ? (
        <div className="card border-0 shadow-sm p-5 text-center text-muted">
          <i className="bi bi-calendar-x fs-1 mb-3"></i>
          <p className="mb-0">No appointments yet. <a href="/customer/services">Book one now.</a></p>
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Service</th>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map(a => (
                  <tr key={a.id}>
                    <td className="fw-semibold">{a.serviceName}</td>
                    <td>{a.date}</td>
                    <td>{a.timeSlot}</td>
                    <td>
                      <span className={`badge bg-${STATUS_COLORS[a.status] || 'secondary'}`}>
                        {a.status}
                      </span>
                    </td>
                    <td>
                      {a.status === 'CONFIRMED' && (
                        <button className="btn btn-outline-danger btn-sm"
                                onClick={() => cancel(a.id)}>
                          <i className="bi bi-x-circle me-1"></i>Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}
