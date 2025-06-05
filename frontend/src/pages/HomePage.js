import React, { useState } from 'react';

function HomePage() {
  const [scheduler, setScheduler] = useState('');

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
