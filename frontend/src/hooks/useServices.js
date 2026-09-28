import { useState, useCallback } from 'react';
import api, { cacheAPI } from '../api/axios';
import { usePolling } from './usePolling';

export const useServices = (refreshInterval = null) => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchServices = useCallback(async () => {
    try {
      // Fetch service configs and statuses in parallel
      const [configResponse, statusResponse] = await Promise.all([
        api.get('/services'),
        cacheAPI.getServiceStatuses()
      ]);
      
      const configs = configResponse.data;
      const statuses = statusResponse.data;
      
      // Create a map of statuses by service ID
      const statusMap = {};
      statuses.forEach(status => {
        statusMap[status.id] = status;
      });
      
      // Merge configs with statuses
      const mergedServices = configs.map(config => ({
        ...config,
        status: statusMap[config.id]?.status || 'UNKNOWN',
        lastError: statusMap[config.id]?.lastError,
        lastCheckedAt: statusMap[config.id]?.lastCheckedAt,
        lastSuccessfulCheck: statusMap[config.id]?.lastSuccessfulCheck
      }));
      
      setServices(mergedServices);
      setError('');
    } catch (error) {
      setError('Failed to load services');
      // Error already set in state
    } finally {
      setLoading(false);
    }
  }, []);

  const toggleService = async (serviceId, currentStatus) => {
    try {
      const service = services.find(s => s.id === serviceId);
      const newStatus = !currentStatus;
      
      const updatedService = {
        ...service,
        isActive: newStatus
      };
      
      await api.put(`/services/${serviceId}`, updatedService);
      
      setServices(prev => 
        prev.map(service => 
          service.id === serviceId 
            ? { ...service, isActive: newStatus }
            : service
        )
      );
    } catch (error) {
      console.error('Failed to toggle service:', error);
      setError('Failed to update service status');
    }
  };

  usePolling(fetchServices, refreshInterval);

  return {
    services,
    loading,
    error,
    fetchServices,
    toggleService
  };
};
