import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const dashboardPath = () => {
    if (!user) return '/login'
    if (user.role === 'ADMIN')    return '/admin/dashboard'
    if (user.role === 'STAFF')    return '/staff/dashboard'
    return '/customer/dashboard'
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
      <div className="container">
        <Link className="navbar-brand fw-bold" to={dashboardPath()}>
          <i className="bi bi-people-fill me-2"></i>SmartQueue
        </Link>
        {user && (
          <>
            <button className="navbar-toggler" type="button" data-bs-toggle="collapse"
                    data-bs-target="#navMenu">
              <span className="navbar-toggler-icon"></span>
            </button>
            <div className="collapse navbar-collapse" id="navMenu">
              <ul className="navbar-nav me-auto">
                {user.role === 'CUSTOMER' && (
                  <>
                    <li className="nav-item">
                      <Link className="nav-link" to="/customer/services">Services</Link>
                    </li>
                    <li className="nav-item">
                      <Link className="nav-link" to="/customer/appointments">Appointments</Link>
                    </li>
                    <li className="nav-item">
                      <Link className="nav-link" to="/customer/queue">My Queue</Link>
                    </li>
                  </>
                )}
                {user.role === 'STAFF' && (
                  <li className="nav-item">
                    <Link className="nav-link" to="/staff/dashboard">Queue Management</Link>
                  </li>
                )}
                {user.role === 'ADMIN' && (
                  <>
                    <li className="nav-item">
                      <Link className="nav-link" to="/admin/services">Services</Link>
                    </li>
                    <li className="nav-item">
                      <Link className="nav-link" to="/admin/staff">Staff</Link>
                    </li>
                  </>
                )}
              </ul>
              <span className="navbar-text me-3 text-white-50">
                <i className="bi bi-person-circle me-1"></i>{user.name}
              </span>
              <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
                <i className="bi bi-box-arrow-right me-1"></i>Logout
              </button>
            </div>
          </>
        )}
      </div>
    </nav>
  )
}
