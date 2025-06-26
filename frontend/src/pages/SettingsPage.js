import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Settings.css';

function SettingsPage() {
  const [modems, setModems] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then(res => res.json())
      .then(data => setModems(data.map(m => ({ ...m, editing: false }))))
      .catch(err => console.error('❌ Failed to load initial settings:', err));
  }, []);

  const isValidIP = (ip) => {
    const ipRegex = /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;
    return ipRegex.test(ip);
  };

  const handleUpdate = async (id, ip) => {
    if (!ip.trim()) return alert("IP cannot be empty.");
    if (!isValidIP(ip)) return alert("Invalid IP address format.");
    try {
      await axios.post(`http://localhost:8080/api/settings/${id}/ip`, { ip });
      alert(`Updated ${id} to ${ip}`);
    } catch (e) {
      alert(`Failed to update ${id}`);
    }
  };

  const handleToggleModem = async (id, index) => {
    const updated = [...modems];
    const newPower = !updated[index].power;
    try {
      await axios.post(`http://localhost:8080/api/settings/${id}/power`, { on: newPower });
      updated[index].power = newPower;
      setModems(updated);
    } catch (e) {
      alert(`Failed to toggle power for ${id}`);
    }
  };

  const saveModemName = async (id, index) => {
    const updated = [...modems];
    try {
      await axios.post(`http://localhost:8080/api/settings/${id}/name`, { name: updated[index].name });
      updated[index].editing = false;
      setModems(updated);
    } catch (e) {
      alert(`Failed to rename ${id}`);
    }
  };

  const addModem = async () => {
    let name = prompt("Enter a name for the new modem:");
    if (!name || !name.trim()) return alert("Modem name is required.");

    name = name.trim();
    const newId = name.toLowerCase().replace(/\s+/g, '-'); // e.g., "Modem 3" → "modem-3"

    if (modems.some(m => m.id === newId)) {
      return alert("A modem with this name already exists.");
    }

    const newModem = {
      id: newId,
      name,
      ip: '',
      power: false,
    };

    try {
      await axios.post('http://localhost:8080/api/settings/modems', newModem);
      setModems([...modems, { ...newModem, editing: false }]);
    } catch (e) {
      alert('Failed to add new modem');
    }
  };

  const deleteModem = async (id) => {
    if (!window.confirm(`Are you sure you want to delete ${id}?`)) return;
    try {
      await axios.delete(`http://localhost:8080/api/settings/${id}`);
      setModems(modems.filter(m => m.id !== id));
    } catch (e) {
      alert(`Failed to delete ${id}`);
    }
  };

  return (
    <div className="page">
      <div className="container">
        <h2>Network Settings</h2>
        <p className="description">Configure the IP addresses for your network devices</p>

        {modems.map((modem, index) => (
          <div key={modem.id}>
            <div className="input-row">
              {modem.editing ? (
                <>
                  <input
                    className="input"
                    value={modem.name}
                    onChange={(e) => {
                      const updated = [...modems];
                      updated[index].name = e.target.value;
                      setModems(updated);
                    }}
                  />
                  <button className="button" onClick={() => saveModemName(modem.id, index)}>Save</button>
                </>
              ) : (
                <>
                  <label>{modem.name} IP</label>
                  <button className="button" onClick={() => {
                    const updated = [...modems];
                    updated[index].editing = true;
                    setModems(updated);
                  }}>Edit Name</button>
                </>
              )}
              <input
                className="input"
                value={modem.ip}
                onChange={(e) => {
                  const updated = [...modems];
                  updated[index].ip = e.target.value;
                  setModems(updated);
                }}
                placeholder="e.g., 192.168.1.1"
              />
              <button className="button" onClick={() => handleUpdate(modem.id, modem.ip)}>Update</button>

              {/* Power toggle next to Delete */}
              <label className="switch" style={{ marginLeft: '10px' }}>
                <input
                  type="checkbox"
                  checked={modem.power}
                  onChange={() => handleToggleModem(modem.id, index)}
                />
                <span className="slider round"></span>
              </label>
              <span style={{ margin: '0 10px' }}>{modem.power ? 'ON' : 'OFF'}</span>

              <button className="button danger" onClick={() => deleteModem(modem.id)}>Delete</button>
            </div>
          </div>
        ))}

        <div className="input-row">
          <button className="button" onClick={addModem}>+ Add Modem</button>
        </div>
      </div>
    </div>
  );
}

export default SettingsPage;
