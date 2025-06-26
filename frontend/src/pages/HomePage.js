import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './HomePage.css';

function HomePage() {
  const [scheduler, setScheduler] = useState('');
  const [schedulers, setSchedulers] = useState([]);
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
  };

  const handleUpload = () => {
    if (!scheduler) {
      alert('Please select a scheduler type.');
      return;
    }

    localStorage.setItem('schedulerType', scheduler); // Store for use in AnalysisPage
    alert(`Uploading with scheduler: ${scheduler}`);
    navigate('/analysis'); // Navigate to AnalysisPage
  };

  const handleDownload = () => {
    alert('Downloading...');
    // You can add your download logic here
  };

  return (
    <div className="page">
      <div className="container">
        <h2>Choose a Scheduler</h2>

        <label htmlFor="scheduler">Scheduler Type</label>
        <select
          id="scheduler"
          value={scheduler}
          onChange={handleSelectChange}
        >
          <option value="">Select</option>
          {schedulers.map(s => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>

        <div className="button-group">
          <button className="button" onClick={handleUpload}>Upload</button>
          <button className="button" onClick={handleDownload}>Download</button>
        </div>
      </div>
    </div>
  );
}

export default HomePage;
