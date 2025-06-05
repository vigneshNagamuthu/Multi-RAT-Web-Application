import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();

    if (email === 'admin@example.com' && password === 'password123') {
      localStorage.setItem('isLoggedIn', 'true'); // ✅ Set login flag
      alert('Login successful!');
      navigate('/home');
    } else {
      alert('Invalid email or password');
    }
  };

  return (
    <div style={styles.container}>
      <h2>Login Page</h2>
      <form onSubmit={handleLogin} style={styles.form}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={styles.input}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          style={styles.input}
          required
        />
        <button type="submit" style={styles.button}>Login</button>
      </form>
    </div>
  );
}

const styles = {
  container: { textAlign: 'center', padding: '40px' },
  form: { display: 'inline-block', textAlign: 'left', marginTop: '20px' },
  input: { display: 'block', marginBottom: '10px', padding: '10px', width: '250px', fontSize: '16px' },
  button: { padding: '10px 20px', fontSize: '16px', cursor: 'pointer' },
};

export default LoginPage;
