import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './HomePage.css';

function HomePage() {
  const [scheduler, setScheduler] = useState('');
  const [schedulers, setSchedulers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [targetIp, setTargetIp] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [schedulerTouched, setSchedulerTouched] = useState(false);
  const [recentIps, setRecentIps] = useState([]);
  const [progress, setProgress] = useState(0);
  const [protocol, setProtocol] = useState('TCP'); // TCP or MPTCP
  const navigate = useNavigate();

  useEffect(() => {
    fetch('http://localhost:8080/api/schedulers')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setSchedulers(data);
        } else if (typeof data === 'string') {
          setSchedulers(data.trim().split(/\s+/));
        } else {
          setSchedulers([]);
        }
      })
      .catch(() => setSchedulers([]));
    // Load recent IPs from localStorage
    const saved = JSON.parse(localStorage.getItem('recentIps') || '[]');
    setRecentIps(saved);
  }, []);

  // Animate loading bar progress
  useEffect(() => {
    if (loading && !errorMsg) {
      setProgress(0);
      let start = Date.now();
      const duration = 10000; // 10 seconds
      const step = () => {
        const elapsed = Date.now() - start;
        const percent = Math.min(100, (elapsed / duration) * 100);
        setProgress(percent);
        if (percent < 100 && loading && !errorMsg) {
          requestAnimationFrame(step);
        }
      };
      requestAnimationFrame(step);
    } else {
      setProgress(0);
    }
  }, [loading, errorMsg]);

  // Helper to save recent IPs
  function saveRecentIp(ip) {
    let updated = [ip, ...recentIps.filter(item => item !== ip)];
    if (updated.length > 5) updated = updated.slice(0, 5);
    setRecentIps(updated);
    localStorage.setItem('recentIps', JSON.stringify(updated));
  }

  const handleSelectChange = (e) => {
    setScheduler(e.target.value);
    setSchedulerTouched(true);
  };

  const handleUpload = async () => {
    if (!targetIp) {
      alert('Please enter a target IP address.');
      return;
    }
    if (!scheduler) {
      alert('Please select a scheduler.');
      return;
    }
    try {
      setLoading(true);
      setErrorMsg('');
      const res = await fetch('http://localhost:8080/api/analysis/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ip: targetIp, scheduler, protocol })
      });
      const data = await res.json();
      if (data.error) {
        setLoading(false);
        setErrorMsg('Error: check IP or network Iperf3 compatibility');
        setTimeout(() => setErrorMsg(''), 5000);
        return;
      }
      localStorage.setItem('iperfResult', JSON.stringify(data));
      localStorage.setItem('selectedScheduler', scheduler);
      saveRecentIp(targetIp); // Save IP after success
      navigate('/analysis'); // Navigate to AnalysisPage
    } catch (err) {
      setLoading(false);
      setErrorMsg('Error: check IP or network Iperf3 compatibility');
      setTimeout(() => setErrorMsg(''), 5000);
    }
  };

  const handleDownload = async () => {
    if (!targetIp) {
      alert('Please enter a target IP address.');
      return;
    }
    if (!scheduler) {
      alert('Please select a scheduler.');
      return;
    }
    try {
      setLoading(true);
      setErrorMsg('');
      const res = await fetch('http://localhost:8080/api/analysis/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ip: targetIp, scheduler, protocol })
      });
      const data = await res.json();
      if (data.error) {
        setLoading(false);
        setErrorMsg('Error: check IP or network Iperf3 compatibility');
        setTimeout(() => setErrorMsg(''), 5000);
        return;
      }
      localStorage.setItem('iperfResult', JSON.stringify(data));
      localStorage.setItem('selectedScheduler', scheduler);
      saveRecentIp(targetIp); // Save IP after success
      navigate('/analysis');
    } catch (err) {
      setLoading(false);
      setErrorMsg('Error: check IP or network Iperf3 compatibility');
      setTimeout(() => setErrorMsg(''), 5000);
    }
  };

  return (
    <div className="page">
      {/* Overlay to block all interaction when loading */}
      {loading && !errorMsg && (
        <div className="global-loading-overlay"></div>
      )}
      <div className="container" style={loading || errorMsg ? { filter: 'blur(4px)' } : {}}>
        <h2>Choose a Scheduler</h2>

        <label htmlFor="scheduler">Scheduler Type</label>
        <select
          id="scheduler"
          value={scheduler}
          onChange={handleSelectChange}
          onBlur={() => setSchedulerTouched(true)}
        >
          <option value="">Select</option>
          {schedulers.map(s => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        {schedulerTouched && !scheduler && (
          <div style={{ color: 'red', marginBottom: '10px' }}>Please select a scheduler.</div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <input
            type="text"
            list="recent-ip-list"
            placeholder="Enter target IP address"
            value={targetIp}
            onChange={e => setTargetIp(e.target.value)}
            style={{ marginBottom: '20px', padding: '10px', fontSize: '16px', width: '250px', textAlign: 'center' }}
            disabled={loading}
          />
          <datalist id="recent-ip-list">
            {recentIps.map(ip => <option key={ip} value={ip} />)}
          </datalist>
          {/* TCP/MPTCP Slider */}
          <div className="protocol-toggle">
            <label>
              <input
                type="radio"
                name="protocol"
                value="TCP"
                checked={protocol === 'TCP'}
                onChange={() => setProtocol('TCP')}
                disabled={loading}
              /> TCP
            </label>
            <label style={{ marginLeft: '20px' }}>
              <input
                type="radio"
                name="protocol"
                value="MPTCP"
                checked={protocol === 'MPTCP'}
                onChange={() => setProtocol('MPTCP')}
                disabled={loading}
              /> MPTCP
            </label>
          </div>
          <div className="button-group">
            <button className="button" onClick={handleUpload} disabled={loading || !targetIp || !scheduler}>Upload</button>
            <button className="button" onClick={handleDownload} disabled={loading || !targetIp || !scheduler}>Download</button>
          </div>
        </div>
      </div>
      {loading && !errorMsg && (
        <div className="loading-bar-container">
          <div className="loading-bar-label">Loading...</div>
          <div className="loading-bar-outer">
            <div className="loading-bar-inner" style={{ width: `${progress}%` }}></div>
          </div>
        </div>
      )}
      {errorMsg && (
        <div className="error-message-container">
          <span className="error-icon" aria-label="error">&#10060;</span>
          <span className="error-message-text">{errorMsg}</span>
        </div>
      )}
    </div>
  );
}

export default HomePage;
