import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function HomePage() {
  const [scheduler, setScheduler] = useState('');
  const navigate = useNavigate();

  const handleSelectChange = (e) => {
    setScheduler(e.target.value);
  };

  const handleUpload = () => {
    alert(`Uploading with scheduler: ${scheduler}`);
    // You can add your upload logic here
  };

  const handleDownload = () => {
    alert('Downloading...');
    // You can add your download logic here
  };

  return (
    <div style={styles.page}>
      <nav style={styles.navbar}>
        <div style={styles.logo}>🌐</div>
        <div style={styles.navLinks}>
          <span onClick={() => navigate('/HomePage')}>Home</span>
          <span onClick={() => navigate('/dashboard')}>Dashboard</span>
          <span onClick={() => navigate('/analysis')}>Analysis</span>
          <span onClick={() => navigate('/settings')}>Settings</span>
        </div>
      </nav>

      <div style={styles.container}>
        <h2>Choose a Scheduler</h2>

        <label htmlFor="scheduler">Scheduler Type</label>
        <select
          id="scheduler"
          value={scheduler}
          onChange={handleSelectChange}
          style={styles.select}
        >
          <option value="">Select</option>
          <option value="Round Robin">Round Robin</option>
          <option value="RTT">RTT</option>
        </select>

        <div style={styles.buttonGroup}>
          <button style={styles.button} onClick={handleUpload}>Upload</button>
          <button style={styles.button} onClick={handleDownload}>Download</button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: {
    fontFamily: 'sans-serif',
  },
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '20px 40px',
    borderBottom: '1px solid #eee',
    alignItems: 'center',
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
    textAlign: 'center',
    marginTop: '80px',
  },
  select: {
    display: 'block',
    margin: '20px auto',
    padding: '10px',
    fontSize: '16px',
    width: '200px',
  },
  buttonGroup: {
    display: 'flex',
    justifyContent: 'center',
    gap: '20px',
    marginTop: '30px',
  },
  button: {
    padding: '10px 20px',
    backgroundColor: '#333',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    fontSize: '16px',
    cursor: 'pointer',
  },
};

export default HomePage;