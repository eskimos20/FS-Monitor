import { useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

const API_URL = '/delete-services';

export const useDeleteServices = (refreshInterval = 60000) => {
  const [deleteServices, setDeleteServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDeleteServices = useCallback(async () => {
    try {
      const response = await api.get(API_URL);
      setDeleteServices(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching delete services:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDeleteServices();
    
    if (refreshInterval > 0) {
      const interval = setInterval(fetchDeleteServices, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [fetchDeleteServices, refreshInterval]);

  const createDeleteService = async (data) => {
    try {
      const response = await api.post(API_URL, data);
      setDeleteServices([...deleteServices, response.data]);
      return response.data;
    } catch (err) {
      console.error('Error creating delete service:', err);
      throw err;
    }
  };

  const updateDeleteService = async (id, data) => {
    try {
      const response = await api.put(`${API_URL}/${id}`, data);
      setDeleteServices(deleteServices.map(s => s.id === id ? response.data : s));
      return response.data;
    } catch (err) {
      console.error('Error updating delete service:', err);
      throw err;
    }
  };

  const deleteDeleteService = async (id) => {
    try {
      await api.delete(`${API_URL}/${id}`);
      setDeleteServices(deleteServices.filter(s => s.id !== id));
    } catch (err) {
      console.error('Error deleting delete service:', err);
      throw err;
    }
  };

  const toggleDeleteService = async (id) => {
    try {
      const response = await api.post(`${API_URL}/${id}/toggle`);
      setDeleteServices(deleteServices.map(s => s.id === id ? response.data : s));
      return response.data;
    } catch (err) {
      console.error('Error toggling delete service:', err);
      throw err;
    }
  };

  return {
    deleteServices,
    loading,
    error,
    createDeleteService,
    updateDeleteService,
    deleteDeleteService,
    toggleDeleteService,
    refresh: fetchDeleteServices
  };
};
