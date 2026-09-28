import { useState, useCallback } from 'react';
import { systemAPI } from '../api/axios';
import { usePolling } from './usePolling';

export const useSystemStats = (refreshInterval) => {
  const [systemStats, setSystemStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchSystemStats = useCallback(async () => {
    try {
      const response = await systemAPI.getStats();
      setSystemStats(response.data);
      setError('');
    } catch (error) {
      setError('Failed to load system stats');
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchSystemStats, refreshInterval);

  return {
    systemStats,
    loading,
    error,
    fetchSystemStats
  };
};
