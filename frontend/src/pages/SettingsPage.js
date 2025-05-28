import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function SettingsPage() {
  const navigate = useNavigate();
  const [if1, setIf1] = useState('');
  const [if2, setIf2] = useState('');
  const [server, setServer] = useState('');

  const handleUpdate = (label, value) => {
    alert(`${label} IP updated to: ${value}`);
    // Later: send POST to backend
  };

  return (
    <div style={styles.page}>
      {/* Nav Bar */}
      <nav style={styles.navbar}>
        <div style={styles.logo}>🌐</div>
        <div style={styles.navLinks}>
          <span onClick={() => navigate('/home')}>Home</span>
          <span onClick={() => navigate('/dashboard')}>Dashboard</span>
          <span onClick={() => navigate('/analysis')}>Analysis</span>
          <span onClick={() => navigate('/settings')}>Settings</span>
        </div>
      </nav>

      <div style={styles.container}>
        <h2>Settings</h2>

        <div style={styles.inputRow}>
          <label>IF1</label>
          <input style={styles.input} value={if1} onChange={e => setIf1(e.target.value)} placeholder="IP" />
          <button style={styles.button} onClick={() => handleUpdate('IF1', if1)}>Update</button>
        </div>

        <div style={styles.inputRow}>
          <label>IF2</label>
          <input style={styles.input} value={if2} onChange={e => setIf2(e.target.value)} placeholder="IP" />
          <button style={styles.button} onClick={() => handleUpdate('IF2', if2)}>Update</button>
        </div>

        <div style={styles.inputRow}>
          <label>Server</label>
          <input style={styles.input} value={server} onChange={e => setServer(e.target.value)} placeholder="IP" />
          <button style={styles.button} onClick={() => handleUpdate('Server', server)}>Update</button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: {
    fontFamily: 'sans-serif',
    padding: '20px'
  },
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '20px 40px',
    borderBottom: '1px solid #eee',
    alignItems: 'center',
    marginBottom: '40px'
  },
  logo: {
    fontWeight: 'bold',
    fontSize: '24px',
  },
  navLinks: {
    display: 'flex',
    gap: '30px',
    cursor: 'pointer',
    fontSize: '16px',
  },
  container: {
    maxWidth: '600px',
    margin: '0 auto',
  },
  inputRow: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '20px',
    gap: '10px',
  },
  input: {
    flex: 1,
    padding: '10px',
    fontSize: '16px',
  },
  button: {
    backgroundColor: '#222',
    color: 'white',
    border: 'none',
    padding: '10px 20px',
    cursor: 'pointer',
    fontSize: '16px',
  },
};

export default SettingsPage;