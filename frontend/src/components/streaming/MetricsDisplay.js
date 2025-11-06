export default function MetricsDisplay({ metrics, isConnected, error }) {
  if (error) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        ❌ {error}
      </div>
    );
  }

  if (!isConnected) {
    return (
      <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded">
        🔌 Connecting to streaming server...
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="bg-gray-100 border border-gray-400 text-gray-700 px-4 py-3 rounded">
        ⏳ Waiting for metrics data...
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-sm opacity-80">Latency</div>
          <div className="text-3xl font-bold mt-2">{metrics.latency.toFixed(2)}</div>
          <div className="text-sm opacity-80 mt-1">milliseconds</div>
        </div>
        
        <div className="bg-gradient-to-br from-purple-500 to-purple-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-sm opacity-80">Packet Loss</div>
          <div className="text-3xl font-bold mt-2">{metrics.packetLoss.toFixed(2)}%</div>
          <div className="text-sm opacity-80 mt-1">loss rate</div>
        </div>
        
        <div className="bg-gradient-to-br from-green-500 to-green-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-sm opacity-80">Frame Rate</div>
          <div className="text-3xl font-bold mt-2">{metrics.fps}</div>
          <div className="text-sm opacity-80 mt-1">fps</div>
        </div>
        
        <div className="bg-gradient-to-br from-orange-500 to-orange-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-sm opacity-80">Scheduler</div>
          <div className="text-2xl font-bold mt-2">{metrics.scheduler}</div>
          <div className="text-sm opacity-80 mt-1">Port {metrics.port}</div>
        </div>
      </div>

      <div className="bg-gray-100 p-4 rounded text-sm text-gray-600">
        <div className="flex items-center justify-between">
          <span>Last updated: {new Date(metrics.timestamp).toLocaleTimeString()}</span>
          <span className="flex items-center gap-2">
            <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
            Live
          </span>
        </div>
      </div>
    </div>
  );
}