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
          enabledState[modem.interfaceName] = modem.power; // key by interfaceName
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

  // Find modem by interface name
  const getModemByInterface = (ifaceName) =>
    modems.find(m => m.interfaceName === ifaceName);

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
          {interfaces.map((iface, i) => {
            const modem = getModemByInterface(iface.interface);
            const isEnabled = modem ? enabledModems[modem.interfaceName] : undefined;
            return (
              <div
                key={iface.interface}
                className={`device-card ${modem && isEnabled ? '' : modem ? 'disabled' : ''}`}
              >
                <div className="device-header">
                  <h3 className="device-name">{iface.label}</h3>
                  <div className="device-status">
                    <span
                      className={`status-indicator ${modem ? (isEnabled ? 'active' : 'inactive') : 'unknown'}`}
                      title={modem ? (isEnabled ? 'Connected' : 'Disabled') : 'No toggle available'}
                    ></span>
                    {modem && (
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={isEnabled}
                          onChange={() => toggleModem(modem.id, !isEnabled)}
                        />
                        <span className="slider"></span>
                      </label>
                    )}
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