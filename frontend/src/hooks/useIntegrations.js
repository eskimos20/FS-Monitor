import { useState, useEffect, useCallback } from 'react';
import { integrationAPI, cacheAPI } from '../api/axios';

export const useIntegrations = (refreshInterval = null) => {
  const [integrations, setIntegrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchIntegrations = useCallback(async () => {
    try {
      // Fetch integration configs and statuses in parallel
      const [configResponse, statusResponse] = await Promise.all([
        integrationAPI.getAll(),
        cacheAPI.getIntegrationStatuses()
      ]);
      
      const configs = configResponse.data;
      const statuses = statusResponse.data;
      
      // Create a map of statuses by integration ID
      const statusMap = {};
      statuses.forEach(status => {
        statusMap[status.id] = status;
      });
      
      // Merge configs with statuses
      const mergedIntegrations = configs.map(config => ({
        ...config,
        lastFileFound: statusMap[config.id]?.lastFileFound,
        lastFileName: statusMap[config.id]?.lastFileName,
        lastCheckedAt: statusMap[config.id]?.lastCheckedAt
      }));
      
      setIntegrations(mergedIntegrations);
      setError('');
    } catch (error) {
      setError('Failed to load integrations');
    } finally {
      setLoading(false);
    }
  }, []);

  const toggleIntegration = async (integrationId, currentStatus) => {
    try {
      const integration = integrations.find(i => i.id === integrationId);
      const newStatus = !currentStatus;
      
      const updatedIntegration = {
        ...integration,
        isActive: newStatus,
        monitoringEnabled: newStatus
      };
      
      await integrationAPI.update(integrationId, updatedIntegration);
      
      setIntegrations(prev => 
        prev.map(integration => 
          integration.id === integrationId 
            ? { ...integration, isActive: newStatus, monitoringEnabled: newStatus }
            : integration
        )
      );
    } catch (error) {
      console.error('Failed to toggle integration:', error);
      setError('Failed to update integration status');
    }
  };

  useEffect(() => {
    fetchIntegrations();
    
    if (refreshInterval && refreshInterval > 0) {
      const interval = setInterval(fetchIntegrations, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [fetchIntegrations, refreshInterval]);

  return {
    integrations,
    loading,
    error,
    fetchIntegrations,
    toggleIntegration
  };
};
