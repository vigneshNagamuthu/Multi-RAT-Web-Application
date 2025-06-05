import React, { useState } from 'react';
import axios from 'axios';

function SettingsPage() {
  const [if1, setIf1] = useState('');
  const [if2, setIf2] = useState('');
  const [server, setServer] = useState('');

  const isValidIP = (ip) => {
    const ipRegex = /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;
    return ipRegex.test(ip);
  };

  const handleUpdate = async (label, value) => {
    if (!value.trim()) {
      alert(`${label} cannot be empty.`);
      return;
    }

    if (!isValidIP(value)) {
      alert(`${label} must be a valid IP address (e.g., 192.168.1.1).`);
      return;
    }

    try {
      await axios.post(`http://localhost:8080/api/settings/${label.toLowerCase()}`, {
        ip: value
      });
      alert(`${label} IP updated to: ${value}`);
    } catch (error) {
      console.error("Update failed:", error);
      alert(`Failed to update ${label}`);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <h2>Network Settings</h2>
        <p style={styles.description}>Configure the IP addresses for your network devices</p>

        <div style={styles.inputRow}>
          <label>Modem 1 IP</label>
          <input
            style={styles.input}
            value={if1}
            onChange={e => setIf1(e.target.value)}
            placeholder="e.g., 192.168.1.1"
          />
          <button style={styles.button} onClick={() => handleUpdate('IF1', if1)}>Update</button>
        </div>

        <div style={styles.inputRow}>
          <label>Modem 2 IP</label>
          <input
            style={styles.input}
            value={if2}
            onChange={e => setIf2(e.target.value)}
            placeholder="e.g., 192.168.2.1"
          />
          <button style={styles.button} onClick={() => handleUpdate('IF2', if2)}>Update</button>
        </div>

        <div style={styles.inputRow}>
          <label>Server IP</label>
          <input
            style={styles.input}
            value={server}
            onChange={e => setServer(e.target.value)}
            placeholder="e.g., 47.129.143.46"
          />
          <button style={styles.button} onClick={() => handleUpdate('Server', server)}>Update</button>
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
    maxWidth: '600px',
    margin: '80px auto 0',
    padding: '0 20px',
  },
  description: {
    color: '#666',
    marginBottom: '30px',
  },
  inputRow: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '20px',
    gap: '10px',
    justifyContent: 'center',
  },
  input: {
    flex: 1,
    padding: '10px',
    fontSize: '16px',
    maxWidth: '200px',
    border: '1px solid #ddd',
    borderRadius: '4px',
  },
  button: {
    backgroundColor: '#333',
    color: 'white',
    border: 'none',
    padding: '10px 20px',
    cursor: 'pointer',
    fontSize: '16px',
    borderRadius: '5px',
    transition: 'background-color 0.2s',
  },
};

export default SettingsPage;
