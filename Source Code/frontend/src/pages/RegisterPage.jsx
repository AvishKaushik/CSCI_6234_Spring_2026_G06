import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/client'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm]     = useState({ name: '', email: '', phoneNumber: '', password: '', confirmPassword: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }
    setLoading(true)
    try {
      await api.post('/api/auth/register', form)
      navigate('/login?registered=1')
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-6 col-lg-5">
        <div className="card shadow-sm border-0 mt-4">
          <div className="card-header bg-success text-white text-center py-3">
            <h4 className="mb-0"><i className="bi bi-person-plus me-2"></i>Create Account</h4>
          </div>
          <div className="card-body p-4">
            {error && <div className="alert alert-danger py-2">{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label fw-semibold">Full Name</label>
                <input className="form-control" value={form.name} onChange={update('name')} required />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">Email</label>
                <input type="email" className="form-control" value={form.email} onChange={update('email')} required />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">Phone Number</label>
                <input className="form-control" value={form.phoneNumber} onChange={update('phoneNumber')} placeholder="+1 555 0000" />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">Password</label>
                <input type="password" className="form-control" value={form.password} onChange={update('password')} required minLength={6} />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">Confirm Password</label>
                <input type="password" className="form-control" value={form.confirmPassword} onChange={update('confirmPassword')} required />
              </div>
              <button type="submit" className="btn btn-success w-100" disabled={loading}>
                {loading ? <span className="spinner-border spinner-border-sm me-2"></span> : null}
                Register
              </button>
            </form>
          </div>
          <div className="card-footer text-center text-muted py-3">
            Already have an account? <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
