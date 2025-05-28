import React from 'react';
import laptopImg from '../assets/laptop.png';
import routerImg from '../assets/router.png';
import cloudImg from '../assets/cloud.png';

function DashboardPage() {
  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Dashboard</h2>

      <div style={styles.row}>
        {/* SVG Lines */}
        <svg style={styles.svg}>
          {/* Laptop to M1 */}
          <line x1="190" y1="150" x2="440" y2="100" style={styles.line} />
          {/* Laptop to M2 */}
          <line x1="190" y1="150" x2="440" y2="340" style={styles.line} />
          {/* M1 to Server */}
          <line x1="480" y1="100" x2="690" y2="200" style={styles.line} />
          {/* M2 to Server (dashed) */}
          <line x1="480" y1="340" x2="690" y2="200" style={styles.dashedLine} />
        </svg>

        <div style={{ ...styles.node, top: '100px', left: '150px' }}>
          <img src={laptopImg} alt="Laptop" style={styles.icon} />
          <p>Laptop<br /><small>192.168.100.1</small></p>
        </div>

        <div style={{ ...styles.node, top: '60px', left: '400px' }}>
          <img src={routerImg} alt="M1" style={styles.icon} />
          <p>M1<br /><small>192.168.1.1</small></p>
        </div>

        <div style={{ ...styles.node, top: '300px', left: '400px' }}>
          <img src={routerImg} alt="M2" style={styles.icon} />
          <p>M2<br /><small>192.168.2.1</small></p>
        </div>

        <div style={{ ...styles.node, top: '180px', left: '650px' }}>
          <img src={cloudImg} alt="Server" style={styles.icon} />
          <p>Server<br /><small>47.129.143.46</small></p>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    position: 'relative',
    width: '100%',
    height: '600px',
    backgroundColor: '#fff',
    border: '1px solid #eee',
    fontFamily: 'sans-serif',
  },
  title: {
    textAlign: 'left',
    padding: '20px 40px',
    fontSize: '24px',
  },
  row: {
    position: 'relative',
    width: '100%',
    height: '100%',
  },
  node: {
    position: 'absolute',
    textAlign: 'center',
  },
  icon: {
    width: '80px',
    marginBottom: '8px',
  },
  svg: {
    position: 'absolute',
    width: '100%',
    height: '100%',
    zIndex: 0,
  },
  line: {
    stroke: '#000',
    strokeWidth: 2,
  },
  dashedLine: {
    stroke: '#000',
    strokeWidth: 2,
    strokeDasharray: '6,4',
  },
};

export default DashboardPage;
