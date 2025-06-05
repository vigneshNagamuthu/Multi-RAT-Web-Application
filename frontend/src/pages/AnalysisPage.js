import React, { useEffect, useState } from "react";
import axios from "axios";
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

  useEffect(() => {
    axios
      .get("http://localhost:8080/api/analysis?type=download")
      .then((response) => {
        const formatted = response.data.map((row) => ({
          time: Number(row.time),
          total: Number(row.total),
          m1: Number(row.m1),
          m2: Number(row.m2),
        }));
        console.log("Formatted chart data:", formatted);
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
    a.download = "analysis_data.csv";
    a.click();

    window.URL.revokeObjectURL(url);
  };

  return (
    <div style={{ padding: "2rem" }}>
      <h2>📊 Transfer Speed Analysis</h2>

      {data.length === 0 ? (
        <p>Loading or no data available...</p>
      ) : (
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="total" stroke="#8884d8" name="Total" />
            <Line type="monotone" dataKey="m1" stroke="#82ca9d" name="M1" />
            <Line type="monotone" dataKey="m2" stroke="#ff7300" name="M2" />
          </LineChart>
        </ResponsiveContainer>
      )}

      <button
        onClick={handleExportCSV}
        style={{
          marginTop: "1rem",
          padding: "0.5rem 1rem",
          background: "#4CAF50",
          color: "white",
          border: "none",
          borderRadius: "5px",
          cursor: "pointer",
        }}
      >
        Export as CSV
      </button>
    </div>
  );
}
