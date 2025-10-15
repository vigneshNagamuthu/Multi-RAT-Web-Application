import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import NavBar from './components/NavBar';
import HomePage from './pages/HomePage';
import StreamingPage from './pages/StreamingPage';
import SensorPage from './pages/SensorPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <NavBar />
        <main>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/streaming" element={<StreamingPage />} />
            <Route path="/sensor" element={<SensorPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;