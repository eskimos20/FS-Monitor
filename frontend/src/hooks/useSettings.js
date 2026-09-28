import { useState, useEffect } from 'react';
import { integrationAPI, fileTypeAPI, mailConfigAPI, logConfigAPI, appSettingsAPI } from '../api/axios';
import api from '../api/axios';

export const useSettings = () => {
  const [integrations, setIntegrations] = useState([]);
  const [fileTypes, setFileTypes] = useState([]);
  const [mailConfig, setMailConfig] = useState(null);
  const [services, setServices] = useState([]);
  const [logConfigs, setLogConfigs] = useState([]);
  const [storageConfigs, setStorageConfigs] = useState([]);
  const [deleteServices, setDeleteServices] = useState([]);
  const [appSettings, setAppSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchData = async () => {
    try {
      const [integrationsRes, fileTypesRes, servicesRes, logConfigsRes, storageConfigsRes, deleteServicesRes, mailConfigRes, appSettingsRes] = await Promise.all([
        integrationAPI.getAll().catch(() => ({ data: [] })),
        fileTypeAPI.getAll().catch(() => ({ data: [] })),
        api.get('/services').catch(() => ({ data: [] })),
        logConfigAPI.getAll().catch(() => ({ data: [] })),
        api.get('/storage-configs').catch(() => ({ data: [] })),
        api.get('/delete-services').catch(() => ({ data: [] })),
        mailConfigAPI.getCurrent().catch(() => ({ data: null })),
        appSettingsAPI.getCurrent().catch(() => ({ data: { refreshIntervalSeconds: 5 } }))
      ]);

      setIntegrations(integrationsRes.data);
      setFileTypes(fileTypesRes.data);
      setServices(servicesRes.data);
      setLogConfigs(logConfigsRes.data);
      setStorageConfigs(storageConfigsRes.data);
      setDeleteServices(deleteServicesRes.data);
      setMailConfig(mailConfigRes.data);
      setAppSettings(appSettingsRes.data);

      setLoading(false);
    } catch (err) {
      setError('Failed to load settings');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Shared save/delete plumbing: run action, refresh, flash a message
  const runAction = async (action, successMessage, errorMessage) => {
    try {
      await action();
      setSuccess(successMessage);
      await fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(errorMessage);
      setTimeout(() => setError(''), 3000);
    }
  };

  const confirmAndRun = (entityLabel, action, successMessage) => {
    if (!window.confirm(`Are you sure you want to delete this ${entityLabel}?`)) return;
    return runAction(action, successMessage, `Failed to delete ${entityLabel}`);
  };

  const saveIntegration = (data, id) =>
    runAction(
      () => (id ? integrationAPI.update(id, data) : integrationAPI.create(data)),
      `Integration ${id ? 'updated' : 'created'} successfully`,
      'Failed to save integration'
    );

  const deleteIntegration = (id) =>
    confirmAndRun('integration', () => integrationAPI.delete(id), 'Integration deleted successfully');

  const saveMailConfig = (data) =>
    runAction(
      () => (mailConfig?.id ? mailConfigAPI.update(mailConfig.id, data) : mailConfigAPI.save(data)),
      'Mail configuration saved successfully',
      'Failed to save mail configuration'
    );

  const saveService = (data, id) =>
    runAction(
      () => (id ? api.put(`/services/${id}`, data) : api.post('/services', data)),
      `Service ${id ? 'updated' : 'created'} successfully`,
      'Failed to save service'
    );

  const deleteService = (id) =>
    confirmAndRun('service', () => api.delete(`/services/${id}`), 'Service deleted successfully');

  const saveLogConfig = (data, id) =>
    runAction(
      () => (id ? logConfigAPI.update(id, data) : logConfigAPI.create(data)),
      `Log configuration ${id ? 'updated' : 'created'} successfully`,
      'Failed to save log configuration'
    );

  const deleteLogConfig = (id) =>
    confirmAndRun('log configuration', () => logConfigAPI.delete(id), 'Log configuration deleted successfully');

  const saveStorageConfig = (data, id) =>
    runAction(
      () => (id ? api.put(`/storage-configs/${id}`, data) : api.post('/storage-configs', data)),
      `Storage configuration ${id ? 'updated' : 'created'} successfully`,
      'Failed to save storage configuration'
    );

  const deleteStorageConfig = (id) =>
    confirmAndRun('storage configuration', () => api.delete(`/storage-configs/${id}`), 'Storage configuration deleted successfully');

  const saveDeleteService = (data, id) =>
    runAction(
      () => (id ? api.put(`/delete-services/${id}`, data) : api.post('/delete-services', data)),
      `Delete service ${id ? 'updated' : 'created'} successfully`,
      'Failed to save delete service'
    );

  const deleteDeleteService = (id) =>
    confirmAndRun('delete service', () => api.delete(`/delete-services/${id}`), 'Delete service deleted successfully');

  const saveAppSettings = (data) =>
    runAction(
      () => appSettingsAPI.save(data),
      'App settings saved successfully',
      'Failed to save app settings'
    );

  return {
    integrations,
    fileTypes,
    mailConfig,
    services,
    logConfigs,
    storageConfigs,
    deleteServices,
    appSettings,
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
    saveDeleteService,
    deleteDeleteService,
    saveAppSettings,
    refreshData: fetchData
  };
};
