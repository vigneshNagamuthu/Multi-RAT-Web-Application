import React, { useEffect, useState } from 'react';
import laptopImg from '../assets/laptop.png';
import routerImg from '../assets/router.png';

function DashboardPage() {
  const [modems, setModems] = useState([]);
  const [clientInfo, setClientInfo] = useState({
    ip: 'Loading...',
    interface: 'Loading...'
  });

  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then(res => res.json())
      .then(data => setModems(data))
      .catch(err => console.error('❌ Error fetching modem settings:', err));

    fetch('http://localhost:8080/api/network-info')
      .then(res => res.json())
      .then(json => {
        if (json.ip && json.interface) {
          setClientInfo({ ip: json.ip, interface: json.interface });
        } else {
          setClientInfo({ ip: 'Not found', interface: 'Unknown' });
        }
      })
      .catch(err => {
        console.error('❌ Error fetching network info:', err);
        setClientInfo({ ip: 'Unavailable', interface: 'Unavailable' });
      });
  }, []);

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <h2 style={styles.title}>Network Dashboard</h2>

        <div style={styles.networkContainer}>
          {/* Draw lines dynamically */}
          <svg style={styles.svg} viewBox="0 0 900 600" preserveAspectRatio="xMidYMid meet">
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
                  style={styles.line}
                />
              );
            })}
          </svg>

          {/* Client Node */}
          <div style={{ ...styles.node, top: '100px', left: '100px' }}>
            <img src={laptopImg} alt="Client" style={styles.icon} />
            <p style={styles.nodeLabel}>
              Client<br />
              <small>{clientInfo.ip}</small><br />
              <small>({clientInfo.interface})</small>
            </p>
          </div>

          {/* Modem Nodes */}
          {modems.map((modem, i) => {
            const top = 100 + i * 120;
            return (
              <div key={modem.id} style={{ ...styles.node, top: `${top}px`, left: '500px' }}>
                <img src={routerImg} alt={modem.name} style={styles.icon} />
                <p style={styles.nodeLabel}>
                  {modem.name}<br />
                  <small>{modem.ip}</small>
                </p>
              </div>
            );
          })}
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
    marginTop: '40px',
    padding: '0 20px',
  },
  title: {
    fontSize: '24px',
    marginBottom: '40px',
  },
  networkContainer: {
    position: 'relative',
    width: '100%',
    height: '600px',
    backgroundColor: '#fff',
    border: '1px solid #eee',
    borderRadius: '8px',
    margin: '0 auto',
    maxWidth: '1000px',
    overflow: 'hidden',
  },
  node: {
    position: 'absolute',
    textAlign: 'center',
    backgroundColor: '#fff',
    padding: '10px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    zIndex: 1,
    width: '120px'
  },
  nodeLabel: {
    margin: '8px 0 0',
    fontSize: '14px',
    color: '#333',
    lineHeight: 1.4,
  },
  icon: {
    width: '60px',
    height: '60px',
    objectFit: 'contain',
  },
  svg: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    zIndex: 0,
  },
  line: {
    stroke: '#666',
    strokeWidth: 2,
  },
};

export default DashboardPage;
