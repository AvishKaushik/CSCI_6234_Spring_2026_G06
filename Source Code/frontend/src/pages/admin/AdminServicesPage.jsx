import { useEffect, useRef, useState } from 'react'
import { Modal } from 'bootstrap'
import api from '../../api/client'

const EMPTY = { serviceName: '', description: '', duration: 30, available: true }

const closeModal = (id) => {
  const el = document.getElementById(id)
  if (el) Modal.getOrCreateInstance(el).hide()
}

export default function AdminServicesPage() {
  const [services, setServices] = useState([])
  const [form, setForm]         = useState(EMPTY)
  const [editId, setEditId]     = useState(null)
  const [message, setMessage]   = useState(null)
  const [saving, setSaving]     = useState(false)

  const load = () => api.get('/api/admin/services').then(r => setServices(r.data))
  useEffect(() => { load() }, [])

  const update = (field) => (e) => {
    const val = e.target.type === 'checkbox' ? e.target.checked
              : e.target.type === 'number'   ? parseInt(e.target.value)
              : e.target.value
    setForm(f => ({ ...f, [field]: val }))
  }

  const openAdd  = () => { setForm(EMPTY); setEditId(null) }
  const openEdit = (svc) => { setForm(svc); setEditId(svc.id) }

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editId) await api.put(`/api/admin/services/${editId}`, form)
      else        await api.post('/api/admin/services', form)
      setMessage({ type: 'success', text: editId ? 'Service updated.' : 'Service created.' })
      closeModal('serviceModal')
      load()
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Error saving service' })
    } finally {
      setSaving(false)
    }
  }

  const del = async (id) => {
    if (!window.confirm('Delete this service?')) return
    try {
      await api.delete(`/api/admin/services/${id}`)
      setMessage({ type: 'success', text: 'Service deleted.' })
      load()
    } catch {
      setMessage({ type: 'danger', text: 'Could not delete service' })
    }
  }

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0"><i className="bi bi-gear me-2 text-primary"></i>Manage Services</h2>
        <button className="btn btn-primary" data-bs-toggle="modal"
                data-bs-target="#serviceModal" onClick={openAdd}>
          <i className="bi bi-plus me-1"></i>Add Service
        </button>
      </div>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      <div className="card border-0 shadow-sm">
        {services.length === 0 ? (
          <div className="p-5 text-center text-muted">
            <i className="bi bi-inbox fs-1"></i><p className="mt-2">No services yet.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr><th>Name</th><th>Description</th><th>Duration</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {services.map(svc => (
                  <tr key={svc.id}>
                    <td className="fw-semibold">{svc.serviceName}</td>
                    <td className="text-muted small">{svc.description}</td>
                    <td><span className="badge bg-light text-dark border">{svc.duration} min</span></td>
                    <td>
                      <span className={`badge ${svc.available ? 'bg-success' : 'bg-secondary'}`}>
                        {svc.available ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div className="d-flex gap-2">
                        <button className="btn btn-outline-primary btn-sm"
                                data-bs-toggle="modal" data-bs-target="#serviceModal"
                                onClick={() => openEdit(svc)}>
                          <i className="bi bi-pencil me-1"></i>Edit
                        </button>
                        <button className="btn btn-outline-danger btn-sm"
                                onClick={() => del(svc.id)}>
                          <i className="bi bi-trash me-1"></i>Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      <div className="modal fade" id="serviceModal" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className={`modal-header ${editId ? 'bg-warning' : 'bg-primary text-white'}`}>
              <h5 className="modal-title">
                <i className={`bi ${editId ? 'bi-pencil' : 'bi-plus-circle'} me-2`}></i>
                {editId ? 'Edit Service' : 'Add Service'}
              </h5>
              <button className={`btn-close ${!editId ? 'btn-close-white' : ''}`}
                      data-bs-dismiss="modal"></button>
            </div>
            <form onSubmit={submit}>
              <div className="modal-body">
                <div className="mb-3">
                  <label className="form-label fw-semibold">Service Name</label>
                  <input className="form-control" value={form.serviceName}
                         onChange={update('serviceName')} required />
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Description</label>
                  <textarea className="form-control" rows="2" value={form.description}
                            onChange={update('description')}></textarea>
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Duration (minutes)</label>
                  <input type="number" className="form-control" value={form.duration}
                         min="5" max="240" onChange={update('duration')} required />
                </div>
                {editId && (
                  <div className="form-check">
                    <input className="form-check-input" type="checkbox" id="svcAvail"
                           checked={form.available} onChange={update('available')} />
                    <label className="form-check-label" htmlFor="svcAvail">Service is Active</label>
                  </div>
                )}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary"
                        data-bs-dismiss="modal">Cancel</button>
                <button type="submit" className={`btn ${editId ? 'btn-warning' : 'btn-primary'}`}
                        disabled={saving}>
                  {saving ? <span className="spinner-border spinner-border-sm me-1"></span> : null}
                  <i className="bi bi-check me-1"></i>{editId ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}
