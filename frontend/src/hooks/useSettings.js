import { useState, useEffect } from 'react';
import { integrationAPI, fileTypeAPI, mailConfigAPI, logConfigAPI } from '../api/axios';
import api from '../api/axios';

export const useSettings = () => {
  const [integrations, setIntegrations] = useState([]);
  const [fileTypes, setFileTypes] = useState([]);
  const [mailConfig, setMailConfig] = useState(null);
  const [services, setServices] = useState([]);
  const [logConfigs, setLogConfigs] = useState([]);
  const [storageConfigs, setStorageConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchData = async () => {
    try {
      const [integrationsRes, fileTypesRes, servicesRes, logConfigsRes, storageConfigsRes] = await Promise.all([
        integrationAPI.getAll().catch(() => ({ data: [] })),
        fileTypeAPI.getAll().catch(() => ({ data: [] })),
        api.get('/services').catch(() => ({ data: [] })),
        logConfigAPI.getAll().catch(() => ({ data: [] })),
        api.get('/storage-configs').catch(() => ({ data: [] }))
      ]);

      setIntegrations(integrationsRes.data);
      setFileTypes(fileTypesRes.data);
      setServices(servicesRes.data);
      setLogConfigs(logConfigsRes.data);
      setStorageConfigs(storageConfigsRes.data);

      try {
        const mailRes = await mailConfigAPI.getCurrent();
        setMailConfig(mailRes.data);
      } catch (err) {
        console.log('No mail config found');
      }

      setLoading(false);
    } catch (err) {
      setError('Failed to load settings');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const saveIntegration = async (data, id) => {
    try {
      if (id) {
        await integrationAPI.update(id, data);
        setSuccess('Integration updated successfully');
      } else {
        await integrationAPI.create(data);
        setSuccess('Integration created successfully');
      }
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to save integration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const deleteIntegration = async (id) => {
    if (!window.confirm('Are you sure you want to delete this integration?')) return;
    
    try {
      await integrationAPI.delete(id);
      setSuccess('Integration deleted successfully');
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to delete integration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const saveMailConfig = async (data) => {
    try {
      if (mailConfig?.id) {
        await mailConfigAPI.update(mailConfig.id, data);
      } else {
        await mailConfigAPI.save(data);
      }
      setSuccess('Mail configuration saved successfully');
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to save mail configuration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const saveService = async (data, id) => {
    try {
      if (id) {
        await api.put(`/services/${id}`, data);
        setSuccess('Service updated successfully');
      } else {
        await api.post('/services', data);
        setSuccess('Service created successfully');
      }
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to save service');
      setTimeout(() => setError(''), 3000);
    }
  };

  const deleteService = async (id) => {
    if (!window.confirm('Are you sure you want to delete this service?')) return;
    
    try {
      await api.delete(`/services/${id}`);
      setSuccess('Service deleted successfully');
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to delete service');
      setTimeout(() => setError(''), 3000);
    }
  };

  const saveLogConfig = async (data, id) => {
    try {
      if (id) {
        await logConfigAPI.update(id, data);
        setSuccess('Log configuration updated successfully');
      } else {
        await logConfigAPI.create(data);
        setSuccess('Log configuration created successfully');
      }
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to save log configuration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const deleteLogConfig = async (id) => {
    if (!window.confirm('Are you sure you want to delete this log configuration?')) return;
    
    try {
      await logConfigAPI.delete(id);
      setSuccess('Log configuration deleted successfully');
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to delete log configuration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const saveStorageConfig = async (data, id) => {
    try {
      if (id) {
        await api.put(`/storage-configs/${id}`, data);
        setSuccess('Storage configuration updated successfully');
      } else {
        await api.post('/storage-configs', data);
        setSuccess('Storage configuration created successfully');
      }
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to save storage configuration');
      setTimeout(() => setError(''), 3000);
    }
  };

  const deleteStorageConfig = async (id) => {
    try {
      await api.delete(`/storage-configs/${id}`);
      setSuccess('Storage configuration deleted successfully');
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to delete storage configuration');
      setTimeout(() => setError(''), 3000);
    }
  };

  return {
    integrations,
    fileTypes,
    mailConfig,
    services,
    logConfigs,
    storageConfigs,
    loading,
    error,
    success,
    saveIntegration,
    deleteIntegration,
    saveMailConfig,
    saveService,
    deleteService,
    saveLogConfig,
    deleteLogConfig,
    saveStorageConfig,
    deleteStorageConfig,
    refreshData: fetchData
  };
};
