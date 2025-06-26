import React, { useEffect, useState } from 'react';
import laptopImg from '../assets/laptop.png';
import routerImg from '../assets/router.png';
import './DashboardPage.css';

function DashboardPage() {
  const [modems, setModems] = useState([]);
  const [interfaces, setInterfaces] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then(res => res.json())
      .then(data => setModems(data))
      .catch(err => console.error('❌ Error fetching modem settings:', err));

    fetch('http://localhost:8080/api/network-info')
      .then(res => res.json())
      .then(json => {
        // Collect all interface info from the backend response
        const ifaceList = [];
        for (const key of Object.keys(json)) {
          if (json[key] && json[key].ip && json[key].interface) {
            ifaceList.push({
              label: key,
              ip: json[key].ip,
              interface: json[key].interface
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

  return (
    <div className="dashboard-page">
      <div className="dashboard-container">
        <h2 className="dashboard-title">Network Dashboard</h2>

        <div className="network-container">
          {/* Draw lines dynamically */}
          <svg className="dashboard-svg" viewBox="0 0 900 600" preserveAspectRatio="xMidYMid meet">
            {modems.map((_, i) => {
              const modemBoxTop = 100 + i * 120;
              const modemBoxHeight = 100;
              const y2 = modemBoxTop + modemBoxHeight / 2;
              return (
                <line
                  key={`line-${i}`}
                  x1="210"
                  y1="150"
                  x2="500"
                  y2={y2}
                  className="dashboard-line"
                />
              );
            })}
          </svg>

          {/* Floating IP labels along the lines */}
          {modems.map((modem, i) => {
            // Map interface to modem by order
            const iface = interfaces[i];
            const clientX = 210, clientY = 150;
            const modemX = 500, modemY = 100 + i * 120 + 50; // 50 is half the modem box height
            const midX = (clientX + modemX) / 2;
            const midY = (clientY + modemY) / 2;
            return iface ? (
              <div
                key={`ip-label-${i}`}
                className="ip-label"
                style={{
                  position: 'absolute',
                  left: `${midX}px`,
                  top: `${midY - 20}px`, // slightly above the line
                  background: 'rgba(255,255,255,0.95)',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  color: '#222',
                  boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
                  pointerEvents: 'none',
                  zIndex: 2,
                  border: '1px solid #eee',
                  minWidth: '120px',
                  textAlign: 'center',
                }}
              >
                <b>{iface.interface}</b>: {iface.ip}
              </div>
            ) : null;
          })}

          {/* Client Node */}
          <div className="dashboard-node client-node" style={{ top: '100px', left: '100px' }}>
            <img src={laptopImg} alt="Client" className="dashboard-icon" />
            <p className="dashboard-node-label">
              Client
            </p>
          </div>

          {/* Modem Nodes */}
          {modems.map((modem, i) => {
            const top = 100 + i * 120;
            return (
              <div key={modem.id} className="dashboard-node modem-node" style={{ top: `${top}px`, left: '500px' }}>
                <img src={routerImg} alt={modem.name} className="dashboard-icon" />
                <p className="dashboard-node-label">
                  {modem.name}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;
