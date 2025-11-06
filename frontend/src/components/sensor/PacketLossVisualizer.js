import './PacketLossVisualizer.css';

export default function PacketLossVisualizer({ packets, isConnected, error }) {
  if (error) {
    return (
      <div className="alert alert-error">
        ❌ {error}
      </div>
    );
  }

  if (!isConnected) {
    return (
      <div className="alert alert-warning">
        🔌 Connecting to sensor server...
      </div>
    );
  }

  const lostPackets = packets.filter(p => p.isLost).length;
  const totalPackets = packets.length;
  const lossRate = totalPackets > 0 ? ((lostPackets / totalPackets) * 100).toFixed(2) : 0;

  return (
    <div className="visualizer-container">
      {/* Stats Summary */}
      <div className="stats-grid">
        <div className="stat-card total">
          <div className="stat-label">Total Packets</div>
          <div className="stat-value">{totalPackets}</div>
          <div className="stat-unit">received</div>
        </div>
        
        <div className="stat-card lost">
          <div className="stat-label">Lost Packets</div>
          <div className="stat-value">{lostPackets}</div>
          <div className="stat-unit">dropped</div>
        </div>
        
        <div className="stat-card rate">
          <div className="stat-label">Loss Rate</div>
          <div className="stat-value">{lossRate}%</div>
          <div className="stat-unit">average</div>
        </div>
      </div>

      {/* Packet Sequence List */}
      <div className="packet-monitor">
        <div className="monitor-header">
          <div className="monitor-title-section">
            <div className="monitor-title">
              <span>📡</span>
              <span>Packet Sequence Monitor</span>
            </div>
            <div className="monitor-subtitle">
              Port 5000 - Redundant Scheduler
            </div>
          </div>
          <span className="packet-count">
            {packets.length} packets
          </span>
        </div>
        
        <div className="packet-list">
          {packets.length === 0 ? (
            <div className="packet-empty">
              ⏳ Waiting for packets...
            </div>
          ) : (
            <>
              {packets.slice().reverse().map((packet, idx) => (
                <div
                  key={packets.length - idx}
                  className={`packet-item ${packet.isLost ? 'lost' : 'received'}`}
                >
                  <div className="packet-info">
                    <div className="packet-icon">
                      {packet.isLost ? '❌' : '✅'}
                    </div>
                    <div className="packet-details">
                      <div className="packet-seq">
                        Seq: {packet.sequenceNumber}
                      </div>
                      <span className={`packet-status ${packet.isLost ? 'lost' : 'ok'}`}>
                        {packet.isLost ? 'LOST' : 'OK'}
                      </span>
                    </div>
                  </div>
                  
                  <span className="packet-time">
                    {new Date(packet.timestamp).toLocaleTimeString()}
                  </span>
                </div>
              ))}
            </>
          )}
        </div>
      </div>

      {/* Legend */}
      <div className="legend">
        <div className="legend-title">📊 Legend</div>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-box received"></span>
            <span>Packet Received</span>
          </div>
          <div className="legend-item">
            <span className="legend-box lost"></span>
            <span>Packet Lost</span>
          </div>
        </div>
      </div>
    </div>
  );
}