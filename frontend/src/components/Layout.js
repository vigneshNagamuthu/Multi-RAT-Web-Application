import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import logoImg from '../assets/logo.png';

export default function Layout() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('isLoggedIn'); // ✅ Clear login
    navigate('/');
  };

  return (
    <div style={styles.page}>
      <nav style={styles.navbar}>
        <img src={logoImg} alt="Logo" style={styles.logo} />
        <div style={styles.navLinks}>
          <span onClick={() => navigate('/home')}>Home</span>
          <span onClick={() => navigate('/dashboard')}>Dashboard</span>
          <span onClick={() => navigate('/analysis')}>Analysis</span>
          <span onClick={() => navigate('/settings')}>Settings</span>
          <span onClick={handleLogout} style={styles.logout}>Logout</span>
        </div>
      </nav>
      <Outlet />
    </div>
  );
}

const styles = {
  page: { fontFamily: 'sans-serif' },
  navbar: {
    display: 'flex', justifyContent: 'space-between',
    padding: '20px 40px', borderBottom: '1px solid #eee',
    alignItems: 'center',
  },
  logo: { height: '32px', width: 'auto' },
  navLinks: {
    display: 'flex', gap: '30px', cursor: 'pointer', fontSize: '16px',
  },
  logout: {
    color: 'red', fontWeight: 'bold',
  },
};
