import { useState, useEffect } from 'react';
import api, { logConfigAPI } from '../api/axios';

export const useLogConfigs = () => {
  const [logConfigs, setLogConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLogConfigs = async () => {
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
  };

  useEffect(() => {
    fetchLogConfigs();
  }, []);

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
