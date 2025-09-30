import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import PrivateRoute from './components/PrivateRoute';
import VideoStreamPage from './pages/VideoStreamPage';
import SensorPage from './pages/SensorPage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/" element={<Layout />}>
          <Route
            path="home"
            element={
              <PrivateRoute>
                <HomePage />
              </PrivateRoute>
            }
          />
          <Route
            path="video"
            element={
              <PrivateRoute>
                <VideoStreamPage />
              </PrivateRoute>
            }
          />
          <Route
            path="sensor"
            element={
              <PrivateRoute>
                <SensorPage />
              </PrivateRoute>
            }
          />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
