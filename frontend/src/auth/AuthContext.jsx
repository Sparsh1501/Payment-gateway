import { createContext, useContext, useEffect, useState } from 'react';
import api, { tokenStore } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [merchant, setMerchant] = useState(null);
  const [loading, setLoading] = useState(true);

  async function loadProfile() {
    if (!tokenStore.access) {
      setLoading(false);
      return;
    }
    try {
      const { data } = await api.get('/api/v1/merchant/profile');
      setMerchant(data.data);
    } catch {
      setMerchant(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadProfile();
  }, []);

  async function login(email, password) {
    const { data } = await api.post('/api/v1/auth/login', { email, password });
    tokenStore.set(data.data);
    await loadProfile();
  }

  async function register(businessName, email, password) {
    await api.post('/api/v1/auth/register', { businessName, email, password });
    await login(email, password);
  }

  function logout() {
    tokenStore.clear();
    setMerchant(null);
    window.location.href = '/login';
  }

  return (
    <AuthContext.Provider value={{ merchant, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
