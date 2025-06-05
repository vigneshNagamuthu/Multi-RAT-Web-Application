import React, { useEffect, useState } from 'react';
import laptopImg from '../assets/laptop.png';
import routerImg from '../assets/router.png';
import cloudImg from '../assets/cloud.png';

function DashboardPage() {
  const [data, setData] = useState({
    if1: 'Loading...',
    if2: 'Loading...',
    server: 'Loading...',
    modem1Name: 'Modem 1',
    modem2Name: 'Modem 2',
  });

  useEffect(() => {
    fetch('http://localhost:8080/api/settings')
      .then(res => res.json())
      .then(json => setData(json))
      .catch(err => console.error('❌ Error fetching settings:', err));
  }, []);

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <h2 style={styles.title}>Network Dashboard</h2>

        <div style={styles.networkContainer}>
          <svg style={styles.svg} viewBox="0 0 800 500" preserveAspectRatio="xMidYMid meet">
            <line x1="150" y1="150" x2="400" y2="100" style={styles.line} />
            <line x1="150" y1="150" x2="400" y2="340" style={styles.line} />
            <line x1="440" y1="100" x2="650" y2="200" style={styles.dashedLine} />
            <line x1="440" y1="340" x2="650" y2="200" style={styles.dashedLine} />
          </svg>

          <div style={{ ...styles.node, top: '100px', left: '150px' }}>
            <img src={laptopImg} alt="Client" style={styles.icon} />
            <p style={styles.nodeLabel}>Client<br /><small>192.168.100.1</small></p>
          </div>

          <div style={{ ...styles.node, top: '60px', left: '400px' }}>
            <img src={routerImg} alt="Modem 1" style={styles.icon} />
            <p style={styles.nodeLabel}>{data.modem1Name}<br /><small>{data.if1}</small></p>
          </div>

          <div style={{ ...styles.node, top: '300px', left: '400px' }}>
            <img src={routerImg} alt="Modem 2" style={styles.icon} />
            <p style={styles.nodeLabel}>{data.modem2Name}<br /><small>{data.if2}</small></p>
          </div>

          <div style={{ ...styles.node, top: '180px', left: '650px' }}>
            <img src={cloudImg} alt="Server" style={styles.icon} />
            <p style={styles.nodeLabel}>Server<br /><small>{data.server}</small></p>
          </div>
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
    maxWidth: '900px',
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
  },
  nodeLabel: {
    margin: '8px 0 0',
    fontSize: '14px',
    color: '#333',
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
  dashedLine: {
    stroke: '#666',
    strokeWidth: 2,
    strokeDasharray: '5,5',
  },
};

export default DashboardPage;
