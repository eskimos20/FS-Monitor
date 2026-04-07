import { useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

export const useServices = (refreshInterval = null) => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchServices = useCallback(async () => {
    try {
      const response = await api.get('/services');
      setServices(response.data);
      setError('');
    } catch (error) {
      setError('Failed to load services');
      console.log('Failed to load services:', error.message);
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

  useEffect(() => {
    fetchServices();
    
    if (refreshInterval && refreshInterval > 0) {
      const interval = setInterval(fetchServices, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [fetchServices, refreshInterval]);

  return {
    services,
    loading,
    error,
    fetchServices,
    toggleService
  };
};
