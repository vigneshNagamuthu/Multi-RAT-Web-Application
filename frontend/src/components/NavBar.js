import { Link, useLocation } from 'react-router-dom';
import './NavBar.css';

export default function NavBar() {
  const location = useLocation();
  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-brand">
          <h1>ITP2 - Hybrid MPTCP Scheduler</h1>
          <p>Port-Based Scheduler Demo</p>
        </div>
        
        <div className="navbar-links">
          <Link to="/" className={isActive('/') ? 'active' : ''}>
            🏠 Home
          </Link>
          <Link to="/streaming" className={isActive('/streaming') ? 'active' : ''}>
            📹 Streaming (6060)
          </Link>
          <Link to="/sensor" className={isActive('/sensor') ? 'active' : ''}>
            📡 Sensor (5000)
          </Link>
        </div>
      </div>
    </nav>
  );
}