import { useState, useCallback } from 'react';
import { logConfigAPI } from '../api/axios';
import { usePolling } from './usePolling';
import { sortByName } from '../utils/formatters';

export const useLogConfigs = (refreshInterval = null) => {
  const [logConfigs, setLogConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLogConfigs = useCallback(async () => {
    try {
      setLoading(true);
      const response = await logConfigAPI.getAll();
      setLogConfigs(sortByName(response.data));
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchLogConfigs, refreshInterval);

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
