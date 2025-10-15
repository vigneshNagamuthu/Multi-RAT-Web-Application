import VideoPlayer from '../components/streaming/VideoPlayer';
import MetricsDisplay from '../components/streaming/MetricsDisplay';
import useStreamingWebSocket from '../hooks/useStreamingWebSocket';

export default function StreamingPage() {
  const { metrics, isConnected, error } = useStreamingWebSocket('ws://localhost:8080/ws/streaming');

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-xl p-6 shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold mb-2">📹 Video Streaming</h2>
            <p className="opacity-90">Port 6060 - LRTT Scheduler (Lowest RTT First)</p>
          </div>
          <div className="text-right">
            <div className="text-sm opacity-80">Connection Status</div>
            <div className="flex items-center gap-2 mt-1">
              <span className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-300 animate-pulse' : 'bg-red-300'}`}></span>
              <span className="font-semibold">{isConnected ? 'Connected' : 'Disconnected'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Video Feed */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h3 className="text-xl font-semibold mb-4 text-gray-800">📹 Video Feed</h3>
          <VideoPlayer />
        </div>

        {/* Metrics */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h3 className="text-xl font-semibold mb-4 text-gray-800">📊 Real-Time Metrics</h3>
          <MetricsDisplay metrics={metrics} isConnected={isConnected} error={error} />
        </div>
      </div>

      {/* Info Banner */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-gray-700">
          <strong>ℹ️ Note:</strong> This page demonstrates MPTCP streaming using the LRTT scheduler. 
          The scheduler automatically selects the subflow with the lowest round-trip time for optimal latency.
        </p>
      </div>
    </div>
  );
}