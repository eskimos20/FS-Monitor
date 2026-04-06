import { useState, useEffect } from 'react';
import { integrationAPI } from '../api/axios';

export const useIntegrations = () => {
  const [integrations, setIntegrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchIntegrations = async () => {
    try {
      const response = await integrationAPI.getAll();
      setIntegrations(response.data);
      setError('');
    } catch (error) {
      setError('Failed to load integrations');
    } finally {
      setLoading(false);
    }
  };

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
  }, []);

  return {
    integrations,
    loading,
    error,
    fetchIntegrations,
    toggleIntegration
  };
};
