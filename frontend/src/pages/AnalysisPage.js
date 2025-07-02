import React, { useEffect, useState } from "react";
import axios from "axios";
import "./Analysis.css";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  ResponsiveContainer,
} from "recharts";

export default function AnalysisPage() {
  const [data, setData] = useState(undefined);
  const [schedulerType, setSchedulerType] = useState("Unknown");

  useEffect(() => {
    const iperfResult = localStorage.getItem('iperfResult');
    if (iperfResult) {
      // Parse iperf3 output for bandwidth/time
      const { output } = JSON.parse(iperfResult);
      const lines = output.split('\n');
      const dataPoints = [];
      // Only match lines like: [  5]   1.00-2.00   sec  49.8 MBytes   418 Mbits/sec  ...
      const regex = /^\[\s*\d+\]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+[\d.]+\s+MBytes\s+([\d.]+)\s+Mbits\/sec/;
      lines.forEach(line => {
        const match = line.match(regex);
        if (match) {
          const bandwidth = parseFloat(match[3]);
          dataPoints.push({ total: bandwidth });
        }
      });
      // Add tick (x-axis) as index, but exclude last two points (summary lines)
      const trimmed = dataPoints.length > 2 ? dataPoints.slice(0, -2) : dataPoints;
      const dataWithTicks = trimmed.map((d, i) => ({ ...d, tick: i }));
      setData(dataWithTicks);
      return;
    }
    // No iperfResult present
    setData(null);
  }, []);

  const handleExportCSV = () => {
    if (!data.length) return;

    const header = Object.keys(data[0]).join(",") + "\n";
    const rows = data.map((row) => Object.values(row).join(",")).join("\n");

    const blob = new Blob([header + rows], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = `${schedulerType.replace(/\s+/g, '_')}_analysis.csv`;
    a.click();

    window.URL.revokeObjectURL(url);
  };

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      const row = payload[0].payload;
      return (
        <div className="custom-tooltip">
          <p><strong>Time: {label}</strong></p>
          <p className="total">Total: {row.total} Mbps</p>
          <p className="m1">
            M1: {row.m1} Mbps<br />
            Latency: {row.latencyM1} ms<br />
            Throughput: {row.throughputM1} Mbps
          </p>
          <p className="m2">
            M2: {row.m2} Mbps<br />
            Latency: {row.latencyM2} ms<br />
            Throughput: {row.throughputM2} Mbps
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="analysis-container">
      <h2>📊 Transfer Speed Analysis</h2>
      <p style={{ textAlign: "center", fontSize: "16px", color: "#666" }}>
        Scheduler Type: <strong>{schedulerType}</strong>
      </p>

      {data === undefined ? (
        <p style={{textAlign: 'center', margin: '2rem'}}>Loading test results...</p>
      ) : data === null ? (
        <p style={{color: 'red', textAlign: 'center'}}>No iperf3 test data available. Please run an Upload or Download test first.</p>
      ) : data.length === 0 ? (
        <p>Loading or no data available...</p>
      ) : (
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="tick" />
            <YAxis />
            <Tooltip content={<CustomTooltip />} />
            <Legend />
            <Line type="monotone" dataKey="total" stroke="#8884d8" name="Total" />
            <Line type="monotone" dataKey="m1" stroke="#82ca9d" name="Network 1" />
            <Line type="monotone" dataKey="m2" stroke="#ff7300" name="Network 2" />
          </LineChart>
        </ResponsiveContainer>
      )}

      <button className="export-button" onClick={handleExportCSV}>
        Export as CSV
      </button>
    </div>
  );
}
