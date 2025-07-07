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
  const [interfaces, setInterfaces] = useState([]);

  useEffect(() => {
    const iperfResult = localStorage.getItem('iperfResult');
    if (iperfResult) {
      const parsed = JSON.parse(iperfResult);
      const samples = parsed.samples;
      const ifaces = parsed.interfaces || [];
      setInterfaces(ifaces);
      if (Array.isArray(samples) && samples.length > 0) {
        setData(samples.map((s, i) => {
          const row = { tick: s.second || i + 1, total: s.total || 0 };
          ifaces.forEach(iface => {
            row[iface] = s[iface] || 0;
          });
          return row;
        }));
        return;
      }
    }
    // No iperfResult present or no samples
    setData(null);
    setInterfaces([]);
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
          <p className="total">Total Throughput: {row.total} Mbps</p>
          {interfaces.map(iface => (
            <p key={iface} className="m1">{iface} Throughput: {row[iface]} Mbps</p>
          ))}
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
            <Line type="monotone" dataKey="total" stroke="#8884d8" name="Total Throughput" />
            {interfaces.map((iface, idx) => (
              <Line
                key={iface}
                type="monotone"
                dataKey={iface}
                stroke={['#82ca9d', '#ff7300', '#0088FE', '#FFBB28', '#00C49F', '#FF8042'][idx % 6]}
                name={`${iface} Throughput`}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      )}

      <button className="export-button" onClick={handleExportCSV}>
        Export as CSV
      </button>
    </div>
  );
}
