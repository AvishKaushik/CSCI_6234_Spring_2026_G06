import { useEffect, useState } from 'react'
import { Modal } from 'bootstrap'
import api from '../../api/client'

const EMPTY = { name: '', email: '', password: '' }

const closeModal = (id) => {
  const el = document.getElementById(id)
  if (el) Modal.getOrCreateInstance(el).hide()
}

export default function AdminStaffPage() {
  const [staff, setStaff]         = useState([])
  const [services, setServices]   = useState([])
  const [form, setForm]           = useState(EMPTY)
  const [editId, setEditId]       = useState(null)
  const [message, setMessage]     = useState(null)
  const [saving, setSaving]       = useState(false)

  // Service assignment state
  const [assignStaff, setAssignStaff]       = useState(null)   // staff member being assigned
  const [selectedSvcIds, setSelectedSvcIds] = useState([])

  const load = () => {
    api.get('/api/admin/staff').then(r => setStaff(r.data))
    api.get('/api/admin/services').then(r => setServices(r.data))
  }
  useEffect(() => { load() }, [])

  const update = (field) => (e) => setForm(f => ({ ...f, [field]: e.target.value }))

  const openAdd  = () => { setForm(EMPTY); setEditId(null) }
  const openEdit = (s) => { setForm({ name: s.name, email: s.email, password: '' }); setEditId(s.id) }

  const openAssign = (s) => {
    setAssignStaff(s)
    setSelectedSvcIds(s.assignedServiceIds || [])
  }

  const toggleService = (id) => {
    setSelectedSvcIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    )
  }

  const saveAssignment = async () => {
    try {
      await api.put(`/api/admin/staff/${assignStaff.id}/services`, selectedSvcIds)
      setMessage({ type: 'success', text: `Services assigned to ${assignStaff.name}.` })
      closeModal('assignModal')
      setAssignStaff(null)
      load()
    } catch {
      setMessage({ type: 'danger', text: 'Error saving assignment' })
    }
  }

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editId) await api.put(`/api/admin/staff/${editId}`, form)
      else        await api.post('/api/admin/staff', form)
      setMessage({ type: 'success', text: editId ? 'Staff updated.' : 'Staff account created.' })
      closeModal('staffModal')
      load()
    } catch (err) {
      setMessage({ type: 'danger', text: err.response?.data?.error || 'Error saving staff' })
    } finally {
      setSaving(false)
    }
  }

  const del = async (id) => {
    if (!window.confirm('Delete this staff account?')) return
    try {
      await api.delete(`/api/admin/staff/${id}`)
      setMessage({ type: 'success', text: 'Staff account deleted.' })
      load()
    } catch {
      setMessage({ type: 'danger', text: 'Could not delete staff' })
    }
  }

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0"><i className="bi bi-people me-2 text-success"></i>Manage Staff</h2>
        <button className="btn btn-success" data-bs-toggle="modal"
                data-bs-target="#staffModal" onClick={openAdd}>
          <i className="bi bi-person-plus me-1"></i>Add Staff
        </button>
      </div>

      {message && (
        <div className={`alert alert-${message.type} alert-dismissible`}>
          {message.text}
          <button className="btn-close" onClick={() => setMessage(null)}></button>
        </div>
      )}

      <div className="card border-0 shadow-sm">
        {staff.length === 0 ? (
          <div className="p-5 text-center text-muted">
            <i className="bi bi-people fs-1"></i><p className="mt-2">No staff accounts yet.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr><th>Name</th><th>Email</th><th>Assigned Services</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {staff.map(s => (
                  <tr key={s.id}>
                    <td>
                      <div className="d-flex align-items-center gap-2">
                        <div className="rounded-circle bg-success-subtle d-flex align-items-center justify-content-center"
                             style={{ width: 36, height: 36 }}>
                          <i className="bi bi-person text-success"></i>
                        </div>
                        <span className="fw-semibold">{s.name}</span>
                      </div>
                    </td>
                    <td className="text-muted">{s.email}</td>
                    <td>
                      {s.assignedServiceIds?.length > 0 ? (
                        s.assignedServiceIds.map(sid => {
                          const svc = services.find(x => x.id === sid)
                          return svc ? (
                            <span key={sid} className="badge bg-primary-subtle text-primary border me-1">
                              {svc.serviceName}
                            </span>
                          ) : null
                        })
                      ) : (
                        <span className="text-muted small">None assigned</span>
                      )}
                    </td>
                    <td>
                      <div className="d-flex gap-2 flex-wrap">
                        <button className="btn btn-outline-info btn-sm"
                                data-bs-toggle="modal" data-bs-target="#assignModal"
                                onClick={() => openAssign(s)}>
                          <i className="bi bi-diagram-3 me-1"></i>Services
                        </button>
                        <button className="btn btn-outline-primary btn-sm"
                                data-bs-toggle="modal" data-bs-target="#staffModal"
                                onClick={() => openEdit(s)}>
                          <i className="bi bi-pencil me-1"></i>Edit
                        </button>
                        <button className="btn btn-outline-danger btn-sm"
                                onClick={() => del(s.id)}>
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

      {/* Add / Edit Staff Modal */}
      <div className="modal fade" id="staffModal" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className={`modal-header ${editId ? 'bg-warning' : 'bg-success text-white'}`}>
              <h5 className="modal-title">
                <i className={`bi ${editId ? 'bi-pencil' : 'bi-person-plus'} me-2`}></i>
                {editId ? 'Edit Staff' : 'Add Staff Account'}
              </h5>
              <button className={`btn-close ${!editId ? 'btn-close-white' : ''}`}
                      data-bs-dismiss="modal"></button>
            </div>
            <form onSubmit={submit}>
              <div className="modal-body">
                <div className="mb-3">
                  <label className="form-label fw-semibold">Full Name</label>
                  <input className="form-control" value={form.name}
                         onChange={update('name')} required />
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Email</label>
                  <input type="email" className="form-control" value={form.email}
                         onChange={update('email')} required />
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">
                    Password {editId && <span className="text-muted small">(leave blank to keep current)</span>}
                  </label>
                  <input type="password" className="form-control" value={form.password}
                         onChange={update('password')}
                         {...(!editId ? { required: true, minLength: 6 } : {})}
                         placeholder={editId ? 'Optional' : 'Min. 6 chars'} />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary"
                        data-bs-dismiss="modal">Cancel</button>
                <button type="submit" className={`btn ${editId ? 'btn-warning' : 'btn-success'}`}
                        disabled={saving}>
                  {saving ? <span className="spinner-border spinner-border-sm me-1"></span> : null}
                  <i className="bi bi-check me-1"></i>{editId ? 'Update' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      {/* Assign Services Modal */}
      <div className="modal fade" id="assignModal" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header bg-info text-white">
              <h5 className="modal-title">
                <i className="bi bi-diagram-3 me-2"></i>
                Assign Services — {assignStaff?.name}
              </h5>
              <button className="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div className="modal-body">
              <p className="text-muted small mb-3">
                Select which services this staff member can manage. They will only see assigned services in their dashboard.
              </p>
              {services.map(svc => (
                <div key={svc.id} className="form-check mb-2">
                  <input className="form-check-input" type="checkbox"
                         id={`svc-${svc.id}`}
                         checked={selectedSvcIds.includes(svc.id)}
                         onChange={() => toggleService(svc.id)} />
                  <label className="form-check-label" htmlFor={`svc-${svc.id}`}>
                    <strong>{svc.serviceName}</strong>
                    <span className="text-muted small ms-2">{svc.duration} min</span>
                  </label>
                </div>
              ))}
              {services.length === 0 && (
                <p className="text-muted">No services available. Create services first.</p>
              )}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary"
                      data-bs-dismiss="modal">Cancel</button>
              <button type="button" className="btn btn-info text-white"
                      onClick={saveAssignment}>
                <i className="bi bi-check me-1"></i>Save Assignment
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
