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
  const [data, setData] = useState([]);
  const [schedulerType, setSchedulerType] = useState("Unknown");

  useEffect(() => {
    const type = localStorage.getItem("schedulerType") || "download";
    setSchedulerType(type);

    axios
      .get(`http://localhost:8080/api/analysis?type=${encodeURIComponent(type)}`)
      .then((response) => {
        const formatted = response.data.map((row) => ({
          time: Number(row.time),
          total: Number(row.total),
          m1: Number(row.m1),
          m2: Number(row.m2),
          latencyM1: Number(row.latencyM1),
          latencyM2: Number(row.latencyM2),
          throughputM1: Number(row.throughputM1),
          throughputM2: Number(row.throughputM2),
        }));
        setData(formatted);
      })
      .catch((error) => {
        console.error("Error fetching analysis data: ", error);
      });
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

      {data.length === 0 ? (
        <p>Loading or no data available...</p>
      ) : (
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip content={<CustomTooltip />} />
            <Legend />
            <Line type="monotone" dataKey="total" stroke="#8884d8" name="Total" />
            <Line type="monotone" dataKey="m1" stroke="#82ca9d" name="M1" />
            <Line type="monotone" dataKey="m2" stroke="#ff7300" name="M2" />
          </LineChart>
        </ResponsiveContainer>
      )}

      <button className="export-button" onClick={handleExportCSV}>
        Export as CSV
      </button>
    </div>
  );
}
