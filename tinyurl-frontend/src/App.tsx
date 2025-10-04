import React, { useState, useEffect } from 'react';
import { LoginForm } from './components/LoginForm';
import { Dashboard } from './components/Dashboard';
import { apiService } from './services/api';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userId, setUserId] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is already authenticated on app load
    if (apiService.isAuthenticated()) {
      const storedUserId = apiService.getUserId();
      if (storedUserId) {
        setUserId(storedUserId);
        setIsAuthenticated(true);
      }
    }
    setLoading(false);
  }, []);

  const handleLogin = (newUserId: string) => {
    setUserId(newUserId);
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    apiService.logout();
    setIsAuthenticated(false);
    setUserId('');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-lg text-gray-600">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginForm onLogin={handleLogin} />;
  }

  return (
    <Dashboard userId={userId} onLogout={handleLogout} />
  );
}

export default App;
