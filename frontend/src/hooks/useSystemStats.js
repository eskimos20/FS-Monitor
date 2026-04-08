import { useState, useEffect } from 'react';
import { systemAPI } from '../api/axios';

export const useSystemStats = (refreshInterval) => {
  const [systemStats, setSystemStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchSystemStats = async () => {
    try {
      const response = await systemAPI.getStats();
      setSystemStats(response.data);
      setError('');
    } catch (error) {
      setError('Failed to load system stats');
      // Error already set in state
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSystemStats();
    
    const interval = setInterval(fetchSystemStats, refreshInterval);
    
    return () => clearInterval(interval);
  }, [refreshInterval]);

  return {
    systemStats,
    loading,
    error,
    fetchSystemStats
  };
};
