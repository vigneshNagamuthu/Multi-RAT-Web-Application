const express = require('express');
const net = require('net');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const app = express();
const HLS_DIR = path.join(__dirname, 'hls');
const TCP_PORT = 6060;
const HTTP_PORT = 6061;

// Enable CORS
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  next();
});

// Create HLS directory
if (!fs.existsSync(HLS_DIR)) {
  fs.mkdirSync(HLS_DIR, { recursive: true });
}

// Serve HLS files
app.use('/hls', express.static(HLS_DIR));

app.get('/health', (req, res) => {
  const files = fs.existsSync(HLS_DIR) ? fs.readdirSync(HLS_DIR) : [];
  res.json({
    status: 'ok',
    hlsFiles: files,
    tcpPort: TCP_PORT,
    httpPort: HTTP_PORT
  });
});

let ffmpegProcess = null;

// TCP server to receive stream
const tcpServer = net.createServer((socket) => {
  console.log('✅ Client connected to TCP port', TCP_PORT);

  // Clean old HLS files
  if (fs.existsSync(HLS_DIR)) {
    fs.readdirSync(HLS_DIR).forEach(file => {
      try {
        fs.unlinkSync(path.join(HLS_DIR, file));
      } catch (e) {}
    });
  }

  // Start FFmpeg to convert stream to HLS
  if (ffmpegProcess) {
    ffmpegProcess.kill();
  }

  const args = [
    '-i', 'pipe:0',  // Read from stdin
    '-c:v', 'copy',  // Don't re-encode, just copy (faster!)
    '-f', 'hls',
    '-hls_time', '2',  // Minimum 2 second segments
    '-hls_list_size', '3',  // Keep only 3 segments (6 seconds total)
    '-hls_flags', 'delete_segments+append_list',
    '-hls_segment_type', 'mpegts',
    '-start_number', '0',
    '-hls_segment_filename', path.join(HLS_DIR, 'segment%d.ts'),
    path.join(HLS_DIR, 'stream.m3u8')
  ];

  ffmpegProcess = spawn('ffmpeg', args);

  // Pipe TCP data to FFmpeg
  socket.pipe(ffmpegProcess.stdin);

  ffmpegProcess.stderr.on('data', (data) => {
    const msg = data.toString();
    if (msg.includes('frame=') || msg.includes('time=')) {
      process.stdout.write('.');
    } else {
      // Show all other FFmpeg output (especially errors)
      console.log('[FFmpeg]', msg.trim());
    }
  });

  ffmpegProcess.on('close', (code) => {
    console.log(`\n⚠️ FFmpeg stopped (code ${code})`);
  });

  ffmpegProcess.on('error', (err) => {
    console.error('❌ FFmpeg process error:', err.message);
  });

  socket.on('error', (err) => {
    console.error('Socket error:', err.message);
  });

  socket.on('close', () => {
    console.log('🔌 Client disconnected');
    if (ffmpegProcess) {
      ffmpegProcess.kill();
    }

    // Clean up HLS files immediately when stream stops
    console.log('🧹 Cleaning up HLS segments...');
    if (fs.existsSync(HLS_DIR)) {
      fs.readdirSync(HLS_DIR).forEach(file => {
        try {
          fs.unlinkSync(path.join(HLS_DIR, file));
        } catch (e) {
          console.error('Error deleting file:', e.message);
        }
      });
    }
  });
});

tcpServer.listen(TCP_PORT, '0.0.0.0', () => {
  console.log(`🎥 TCP server listening on port ${TCP_PORT}`);
});

// Start HTTP server
app.listen(HTTP_PORT, '0.0.0.0', () => {
  console.log(`🚀 HLS server listening on port ${HTTP_PORT}`);
  console.log(`📺 Stream URL: http://13.212.221.200:${HTTP_PORT}/hls/stream.m3u8`);
});

// Graceful shutdown newwwww
process.on('SIGINT', () => {
  console.log('\n🛑 Shutting down...');
  if (ffmpegProcess) ffmpegProcess.kill();
  tcpServer.close();
  process.exit();
});