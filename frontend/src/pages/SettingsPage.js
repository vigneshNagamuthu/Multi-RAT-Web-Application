import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Settings.css';

function SettingsPage() {
  const [if1, setIf1] = useState('');
  const [if2, setIf2] = useState('');
  const [server, setServer] = useState('');
  const [modem1On, setModem1On] = useState(false);
  const [modem2On, setModem2On] = useState(false);
  const [modem1Name, setModem1Name] = useState('Modem 1');
  const [modem2Name, setModem2Name] = useState('Modem 2');
  const [edit1, setEdit1] = useState(false);
  const [edit2, setEdit2] = useState(false);

  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then((res) => res.json())
      .then((data) => {
        setIf1(data.if1);
        setIf2(data.if2);
        setServer(data.server);
        setModem1Name(data.modem1Name || 'Modem 1');
        setModem2Name(data.modem2Name || 'Modem 2');
      })
      .catch((err) => console.error('❌ Failed to load initial settings:', err));
  }, []);

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
      await axios.post(`http://localhost:8080/api/settings/${label.toLowerCase()}`, { ip: value });
      alert(`${label} IP updated to: ${value}`);
    } catch (error) {
      console.error("Update failed:", error);
      alert(`Failed to update ${label}`);
    }
  };

  const handleToggleModem = async (modem, currentState, setState) => {
    const newState = !currentState;
    try {
      await axios.post(`http://localhost:8080/api/settings/${modem}/power`, { on: newState });
      setState(newState);
    } catch (error) {
      console.error(`Failed to toggle ${modem}`, error);
      alert(`Failed to toggle ${modem}`);
    }
  };

  const saveModemName = async (modem, name) => {
    try {
      await axios.post(`http://localhost:8080/api/settings/${modem}/name`, { name });
    } catch (error) {
      console.error(`Failed to update ${modem} name`, error);
      alert(`Failed to save ${modem} name`);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <h2>Network Settings</h2>
        <p style={styles.description}>Configure the IP addresses for your network devices</p>

        {/* Modem 1 IP + Name */}
        <div style={styles.inputRow}>
          {edit1 ? (
            <>
              <input style={styles.input} value={modem1Name} onChange={(e) => setModem1Name(e.target.value)} />
              <button
                style={styles.button}
                onClick={() => {
                  setEdit1(false);
                  saveModemName('modem1', modem1Name);
                }}
              >
                Save
              </button>
            </>
          ) : (
            <>
              <label>{modem1Name} IP</label>
              <button style={styles.button} onClick={() => setEdit1(true)}>Edit Name</button>
            </>
          )}
          <input
            style={styles.input}
            value={if1}
            onChange={e => setIf1(e.target.value)}
            placeholder="e.g., 192.168.1.1"
          />
          <button style={styles.button} onClick={() => handleUpdate('IF1', if1)}>Update</button>
        </div>

        {/* Modem 1 Power */}
        <div style={styles.inputRow}>
          <label>{modem1Name} Power</label>
          <label className="switch">
            <input
              type="checkbox"
              checked={modem1On}
              onChange={() => handleToggleModem('modem1', modem1On, setModem1On)}
            />
            <span className="slider round"></span>
          </label>
          <span>{modem1On ? 'ON' : 'OFF'}</span>
        </div>

        {/* Modem 2 IP + Name */}
        <div style={styles.inputRow}>
          {edit2 ? (
            <>
              <input style={styles.input} value={modem2Name} onChange={(e) => setModem2Name(e.target.value)} />
              <button
                style={styles.button}
                onClick={() => {
                  setEdit2(false);
                  saveModemName('modem2', modem2Name);
                }}
              >
                Save
              </button>
            </>
          ) : (
            <>
              <label>{modem2Name} IP</label>
              <button style={styles.button} onClick={() => setEdit2(true)}>Edit Name</button>
            </>
          )}
          <input
            style={styles.input}
            value={if2}
            onChange={e => setIf2(e.target.value)}
            placeholder="e.g., 192.168.2.1"
          />
          <button style={styles.button} onClick={() => handleUpdate('IF2', if2)}>Update</button>
        </div>

        {/* Modem 2 Power */}
        <div style={styles.inputRow}>
          <label>{modem2Name} Power</label>
          <label className="switch">
            <input
              type="checkbox"
              checked={modem2On}
              onChange={() => handleToggleModem('modem2', modem2On, setModem2On)}
            />
            <span className="slider round"></span>
          </label>
          <span>{modem2On ? 'ON' : 'OFF'}</span>
        </div>

        {/* Server IP */}
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
    maxWidth: '700px',
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
    flexWrap: 'wrap',
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
    padding: '10px 15px',
    cursor: 'pointer',
    fontSize: '14px',
    borderRadius: '5px',
  },
};

export default SettingsPage;
