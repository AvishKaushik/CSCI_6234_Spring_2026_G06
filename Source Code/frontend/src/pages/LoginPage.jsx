import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  if (user) {
    if (user.role === 'ADMIN') navigate('/admin/dashboard', { replace: true })
    else if (user.role === 'STAFF') navigate('/staff/dashboard', { replace: true })
    else navigate('/customer/dashboard', { replace: true })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const me = await login(email, password)
      if (me.role === 'ADMIN')    navigate('/admin/dashboard')
      else if (me.role === 'STAFF') navigate('/staff/dashboard')
      else                          navigate('/customer/dashboard')
    } catch (err) {
      const msg = err.response?.data?.error ?? err.response?.data ?? err.message
      setError(typeof msg === 'string' ? msg : 'Invalid email or password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-5 col-lg-4">
        <div className="card shadow-sm border-0 mt-4">
          <div className="card-header bg-primary text-white text-center py-3">
            <h4 className="mb-0"><i className="bi bi-people-fill me-2"></i>SmartQueue</h4>
            <small>Sign in to your account</small>
          </div>
          <div className="card-body p-4">
            {error && <div className="alert alert-danger py-2">{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label fw-semibold">Email</label>
                <input type="email" className="form-control" value={email}
                       onChange={e => setEmail(e.target.value)} required
                       placeholder="you@example.com" autoFocus />
              </div>
              <div className="mb-3">
                <label className="form-label fw-semibold">Password</label>
                <input type="password" className="form-control" value={password}
                       onChange={e => setPassword(e.target.value)} required />
              </div>
              <button type="submit" className="btn btn-primary w-100" disabled={loading}>
                {loading ? <span className="spinner-border spinner-border-sm me-2"></span> : null}
                Sign In
              </button>
            </form>
          </div>
          <div className="card-footer text-center text-muted py-3">
            New customer? <Link to="/register">Create account</Link>
          </div>
        </div>
        <div className="mt-3 text-center text-muted small">
          <strong>Demo:</strong> admin@smartqueue.com / admin123 &nbsp;|&nbsp;
          alice@demo.com / customer123
        </div>
      </div>
    </div>
  )
}
