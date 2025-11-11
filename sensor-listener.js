#!/usr/bin/env node

/**
 * Sensor Packet Listener for AWS
 * Simple TCP server that receives numbered packets and echoes them back
 * 
 * Usage: node sensor-listener.js
 */

const net = require('net');

const PORT = 5000;
const HOST = '0.0.0.0';

let activeConnections = 0;
let receivedPackets = []; // Store all received packet numbers

const server = net.createServer((socket) => {
  activeConnections++;
  const client = `${socket.remoteAddress}:${socket.remotePort}`;
  
  console.log(`\n🔌 Client connected: ${client}`);
  console.log(`📊 Active connections: ${activeConnections}`);
  
  socket.on('data', (data) => {
    const lines = data.toString().trim().split('\n');
    
    lines.forEach(line => {
      if (line.trim()) {
        // Format: SEQ:TIMESTAMP or just SEQ (for backward compatibility)
        const parts = line.trim().split(':');
        const packetNum = parseInt(parts[0]);
        
        if (!isNaN(packetNum)) {
          receivedPackets.push(packetNum);
          
          // Log every 100th packet to avoid spam
          if (packetNum % 100 === 0) {
            console.log(`📥 Received packet #${packetNum} (Total: ${receivedPackets.length})`);
          }
          
          // Echo back with timestamp (same format received)
          socket.write(line + '\n');
        }
      }
    });
  });
  
  socket.on('end', () => {
    activeConnections--;
    console.log(`\n👋 Client disconnected: ${client}`);
    console.log(`📊 Active connections: ${activeConnections}`);
    
    // Show last 20 received packets
    const lastPackets = receivedPackets.slice(-20);
    console.log(`📋 Last packets received: ${lastPackets.join(', ')}`);
  });
  
  socket.on('error', (err) => {
    console.error(`❌ Error: ${err.message}`);
    activeConnections--;
  });
});

server.listen(PORT, HOST, () => {
  console.log('\n' + '═'.repeat(50));
  console.log('📡 Sensor Packet Listener Running');
  console.log('═'.repeat(50));
  console.log(`🌐 Listening on: ${HOST}:${PORT}`);
  console.log('🛑 Press Ctrl+C to stop\n');
});

process.on('SIGINT', () => {
  console.log(`\n\n📊 Final Statistics:`);
  console.log(`   Total connections: ${activeConnections}`);
  
  // Show last 50 received packets
  const lastPackets = receivedPackets.slice(-50);
  console.log(`\n📋 Last 50 packets received:`);
  console.log(`   ${lastPackets.join(', ')}`);
  
  console.log('\n👋 Goodbye!\n');
  process.exit(0);
});
