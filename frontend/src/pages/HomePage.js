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
  }, []);

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
        body: JSON.stringify({ ip: targetIp, scheduler })
      });
      const data = await res.json();
      if (data.error) {
        setLoading(false);
        setErrorMsg('Error: check IP or network Iperf3 compatibility');
        setTimeout(() => setErrorMsg(''), 5000);
        return;
      }
      localStorage.setItem('iperfResult', JSON.stringify(data));
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
        body: JSON.stringify({ ip: targetIp, scheduler })
      });
      const data = await res.json();
      if (data.error) {
        setLoading(false);
        setErrorMsg('Error: check IP or network Iperf3 compatibility');
        setTimeout(() => setErrorMsg(''), 5000);
        return;
      }
      localStorage.setItem('iperfResult', JSON.stringify(data));
      navigate('/analysis');
    } catch (err) {
      setLoading(false);
      setErrorMsg('Error: check IP or network Iperf3 compatibility');
      setTimeout(() => setErrorMsg(''), 5000);
    }
  };

  return (
    <div className="page">
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
            placeholder="Enter target IP address"
            value={targetIp}
            onChange={e => setTargetIp(e.target.value)}
            style={{ marginBottom: '20px', padding: '10px', fontSize: '16px', width: '250px', textAlign: 'center' }}
            disabled={loading}
          />
          <div className="button-group">
            <button className="button" onClick={handleUpload} disabled={loading || !targetIp || !scheduler}>Upload</button>
            <button className="button" onClick={handleDownload} disabled={loading || !targetIp || !scheduler}>Download</button>
          </div>
        </div>
      </div>
      {loading && !errorMsg && (
        <div style={{
          textAlign: 'center',
          marginTop: '-60px',
          fontWeight: 'bold',
          fontSize: '36px',
          position: 'absolute',
          left: 0,
          right: 0,
          top: '50%',
          transform: 'translateY(-50%)',
          zIndex: 10,
          color: 'white',
          textShadow: `
            -2px -2px 0 #000, 2px -2px 0 #000, -2px 2px 0 #000, 2px 2px 0 #000,
            0px 2px 0 #000, 2px 0px 0 #000, 0px -2px 0 #000, -2px 0px 0 #000
          `
        }}>
          Please wait while we plot your graph
        </div>
      )}
      {errorMsg && (
        <div style={{
          textAlign: 'center',
          marginTop: '-60px',
          fontWeight: 'bold',
          fontSize: '28px',
          position: 'absolute',
          left: 0,
          right: 0,
          top: '50%',
          transform: 'translateY(-50%)',
          zIndex: 10,
          color: 'white',
          textShadow: `
            -2px -2px 0 #b00, 2px -2px 0 #b00, -2px 2px 0 #b00, 2px 2px 0 #b00,
            0px 2px 0 #b00, 2px 0px 0 #b00, 0px -2px 0 #b00, -2px 0px 0 #b00
          `
        }}>
          {errorMsg}
        </div>
      )}
    </div>
  );
}

export default HomePage;
