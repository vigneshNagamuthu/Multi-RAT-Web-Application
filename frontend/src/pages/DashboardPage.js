import React, { useEffect, useState } from 'react';
import './DashboardPage.css';

function DashboardPage() {
  const [modems, setModems] = useState([]);
  const [interfaces, setInterfaces] = useState([]);
  const [enabledModems, setEnabledModems] = useState({});
  const [ispInfo, setIspInfo] = useState({}); // Store ISP info for each interface
  const [loadingIsp, setLoadingIsp] = useState({}); // Track loading state

  // Fetch ISP info for a specific IP
  const fetchIspInfo = async (ip, interfaceName) => {
    if (!ip || ip === '-' || ip === '127.0.0.1') return;
    
    setLoadingIsp(prev => ({ ...prev, [interfaceName]: true }));
    
    try {
      const response = await fetch(`http://localhost:8080/api/isp-info/${ip}`);
      const data = await response.json();
      let isp = data.isp || 'Unknown ISP';
      let error = data.error;
      // Fallback: If ISP is N/A, Unknown, or error, fetch public ISP
      if (!isp || isp === 'N/A' || isp === 'Unknown ISP' || error) {
        const pubRes = await fetch('http://localhost:8080/api/isp-info/all');
        const pubData = await pubRes.json();
        isp   = pubData[interfaceName]?.isp || 'Unknown ISP';
        error = pubData.error;
      }
      setIspInfo(prev => ({
        ...prev,
        [interfaceName]: {
          isp,
          city: data.city || 'Unknown',
          country: data.country || 'Unknown',
          error
        }
      }));
    } catch (error) {
      console.error(`Failed to fetch ISP info for ${ip}:`, error);
      setIspInfo(prev => ({
        ...prev,
        [interfaceName]: { error: 'Failed to fetch ISP info' }
      }));
    } finally {
      setLoadingIsp(prev => ({ ...prev, [interfaceName]: false }));
    }
  };

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
            const iface = {
              label: key,
              ip: json[key].ip,
              interface: json[key].interface,
              state: json[key].state
            };
            ifaceList.push(iface);
            
            // Fetch ISP info for each interface with a valid IP
            if (iface.ip && iface.ip !== '-' && iface.ip !== '127.0.0.1') {
              fetchIspInfo(iface.ip, iface.interface);
            }
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
            const currentIspInfo = ispInfo[iface.interface];
            const isLoadingIspInfo = loadingIsp[iface.interface];
            
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
                  
                  {/* ISP Information Section */}
                  <div className="detail-item">
                    <span className="detail-label">Service Provider:</span>
                    <span className="detail-value isp-value">
                      {isLoadingIspInfo ? (
                        <span className="loading-text">Loading...</span>
                      ) : currentIspInfo ? (
                        currentIspInfo.error ? (
                          <span className="error-text">Failed to load</span>
                        ) : (
                          currentIspInfo.isp
                        )
                      ) : iface.ip === '-' || iface.ip === '127.0.0.1' ? (
                        <span className="no-ip-text">No public IP</span>
                      ) : (
                        <span className="loading-text">Loading...</span>
                      )}
                    </span>
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
