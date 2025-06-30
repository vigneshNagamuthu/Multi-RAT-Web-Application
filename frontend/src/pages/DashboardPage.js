import React, { useEffect, useState } from 'react';
import './DashboardPage.css';

function DashboardPage() {
  const [modems, setModems] = useState([]);
  const [interfaces, setInterfaces] = useState([]);
  const [enabledModems, setEnabledModems] = useState({});

  // Fetch modem settings and interface info
  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then(res => res.json())
      .then(data => {
        setModems(data);
        const enabledState = {};
        data.forEach(modem => {
          enabledState[modem.id] = modem.power; // reflects backend's power state
        });
        setEnabledModems(enabledState);
      })
      .catch(err => console.error('❌ Error fetching modem settings:', err));

    fetch('http://localhost:8080/api/network-info')
      .then(res => res.json())
      .then(json => {
        const ifaceList = [];
        for (const key of Object.keys(json)) {
          if (json[key] && json[key].ip && json[key].interface) {
            ifaceList.push({
              label: key,
              ip: json[key].ip,
              interface: json[key].interface,
              state: json[key].state
            });
          }
        }
        setInterfaces(ifaceList);
      })
      .catch(err => {
        console.error('❌ Error fetching network info:', err);
        setInterfaces([]);
      });
  }, []);

  const getModemInterface = (i) => interfaces[i] || { ip: '-', interface: '-', state: '-' };

  const toggleModem = (modemId, newPower) => {
    fetch(`http://localhost:8080/api/settings/${modemId}/power`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ on: newPower })
    })
      .then(async res => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || 'Failed to toggle power');
        }
        return data;
      })
      .then(() => {
        setEnabledModems(prev => ({
          ...prev,
          [modemId]: newPower
        }));
      })
      .catch((err) => {
        alert(err.message || 'Failed to toggle modem power');
      });
  };

  return (
    <div className="dashboard-page">
      <div className="dashboard-container">
        <h2 className="dashboard-title">Network Dashboard</h2>

        <div className="devices-grid">
          {modems.map((modem, i) => {
            const iface = getModemInterface(i);
            const isEnabled = enabledModems[modem.id];

            return (
              <div
                key={modem.id}
                className={`device-card ${isEnabled ? '' : 'disabled'}`}
              >
                <div className="device-header">
                  <h3 className="device-name">Network {i + 1}</h3>
                  <div className="device-status">
                    <span
                      className={`status-indicator ${isEnabled ? 'active' : 'inactive'}`}
                      title={isEnabled ? "Connected" : "Disabled"}
                    ></span>
                    <label className="toggle-switch">
                      <input
                        type="checkbox"
                        checked={isEnabled}
                        onChange={() => toggleModem(modem.id, !isEnabled)}
                      />
                      <span className="slider"></span>
                    </label>
                  </div>
                </div>

                <div className="device-details">
                  <div className="detail-item">
                    <span className="detail-label">Client Interface:</span>
                    <span className="detail-value">{iface.interface}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">IP Address:</span>
                    <span className="detail-value">{iface.ip}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;