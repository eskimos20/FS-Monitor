import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

const AuthContext = createContext(null);

// Parse JWT token to get expiration time
const parseJwt = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
};

// Check if token is expired
const isTokenExpired = (token) => {
  if (!token) return true;
  const decoded = parseJwt(token);
  if (!decoded || !decoded.exp) return true;
  // Add 10 second buffer to avoid edge cases
  return decoded.exp * 1000 < Date.now() + 10000;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const clearAuth = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
  }, []);

  // Check token validity
  const checkTokenValidity = useCallback(() => {
    const token = localStorage.getItem('token');
    if (token && isTokenExpired(token)) {
      clearAuth();
      window.location.href = '/login';
      return false;
    }
    return true;
  }, [clearAuth]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    
    // Check if token exists and is valid
    if (token && !isTokenExpired(token)) {
      // Fetch user data from API
      const fetchUserData = async () => {
        try {
          const userResponse = await api.get('/auth/me');
          const userData = userResponse.data;
          
          // If user must change password, don't restore session from old token.
          // They must go through login first.
          if (userData.mustChangePassword) {
            clearAuth();
          } else {
            setUser(userData);
          }
        } catch (err) {
          if (err.code === 'ECONNREFUSED' || err.message?.includes('Network Error')) {
            console.log('Backend not ready yet, will retry...');
            return;
          }
          clearAuth();
        } finally {
          setLoading(false);
        }
      };
      fetchUserData();
    } else if (token && isTokenExpired(token)) {
      clearAuth();
      setLoading(false);
    } else {
      setLoading(false);
    }
  }, [clearAuth]);

  // Periodically check token validity (every 30 seconds)
  useEffect(() => {
    if (!user) return;
    
    const interval = setInterval(() => {
      checkTokenValidity();
    }, 30000);

    return () => clearInterval(interval);
  }, [user, checkTokenValidity]);

  const login = async (username, password) => {
    const response = await api.post('/auth/login', { usernameOrEmail: username, password });
    const loginData = response.data;
    
    // Save token first
    localStorage.setItem('token', loginData.token);
    
    // Fetch full user data from /auth/me
    try {
      const userResponse = await api.get('/auth/me');
      setUser(userResponse.data);
    } catch (err) {
      // Fallback: set basic user data from login response
      setUser({
        username: loginData.username || loginData.user?.username,
        email: loginData.email || loginData.user?.email,
        mustChangePassword: !loginData.passwordChanged
      });
    }
    
    return loginData;
  };

  const logout = () => {
    localStorage.removeItem('token');
    // Hard redirect BEFORE setting state to avoid React re-render race conditions
    window.location.href = '/login';
  };

  const changePassword = async (currentPassword, newPassword) => {
    await api.post('/auth/change-password', { 
      currentPassword, 
      newPassword,
      confirmPassword: newPassword 
    });
    logout();
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, changePassword, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
