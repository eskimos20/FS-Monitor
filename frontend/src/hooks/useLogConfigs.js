import { useState, useEffect, useCallback } from 'react';
import api, { logConfigAPI } from '../api/axios';

export const useLogConfigs = (refreshInterval = null) => {
  const [logConfigs, setLogConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLogConfigs = useCallback(async () => {
    try {
      setLoading(true);
      const response = await logConfigAPI.getAll();
      setLogConfigs(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLogConfigs();
    
    // Set up periodic refresh if interval is provided
    if (refreshInterval && refreshInterval > 0) {
      const interval = setInterval(fetchLogConfigs, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [fetchLogConfigs, refreshInterval]);

  const createLogConfig = async (data) => {
    try {
      await logConfigAPI.create(data);
      await fetchLogConfigs();
    } catch (err) {
      throw err;
    }
  };

  const updateLogConfig = async (id, data) => {
    try {
      await logConfigAPI.update(id, data);
      await fetchLogConfigs();
    } catch (err) {
      throw err;
    }
  };

  const deleteLogConfig = async (id) => {
    try {
      await logConfigAPI.delete(id);
      await fetchLogConfigs();
    } catch (err) {
      throw err;
    }
  };

  const toggleLogConfig = async (id) => {
    try {
      await logConfigAPI.toggle(id);
      await fetchLogConfigs();
    } catch (err) {
      throw err;
    }
  };

  return {
    logConfigs,
    loading,
    error,
    createLogConfig,
    updateLogConfig,
    deleteLogConfig,
    toggleLogConfig,
    refreshLogConfigs: fetchLogConfigs
  };
};
