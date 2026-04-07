import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// API exports
export const integrationAPI = {
  getAll: () => api.get('/integrations'),
  getById: (id) => api.get(`/integrations/${id}`),
  create: (data) => api.post('/integrations', data),
  update: (id, data) => api.put(`/integrations/${id}`, data),
  delete: (id) => api.delete(`/integrations/${id}`)
};

export const fileTypeAPI = {
  getAll: () => api.get('/file-types'),
  getById: (id) => api.get(`/file-types/${id}`),
  create: (data) => api.post('/file-types', data),
  update: (id, data) => api.put(`/file-types/${id}`, data),
  delete: (id) => api.delete(`/file-types/${id}`)
};

export const mailConfigAPI = {
  getCurrent: () => api.get('/mail-config'),
  save: (data) => api.post('/mail-config', data),
  update: (id, data) => api.put(`/mail-config/${id}`, data),
  delete: (id) => api.delete(`/mail-config/${id}`)
};

export const systemAPI = {
  getStats: () => api.get('/system/stats')
};

export const serviceAPI = {
  testConnection: (data) => api.post('/services/test-connection', data),
  testCredentials: (data) => api.post('/services/test-credentials', data)
};

export const logConfigAPI = {
  getAll: () => api.get('/log-configs'),
  getById: (id) => api.get(`/log-configs/${id}`),
  create: (data) => api.post('/log-configs', data),
  update: (id, data) => api.put(`/log-configs/${id}`, data),
  delete: (id) => api.delete(`/log-configs/${id}`),
  toggle: (id) => api.post(`/log-configs/${id}/toggle`),
  search: (id) => api.get(`/log-configs/${id}/search`),
  getStats: () => api.get('/log-configs/stats'),
  getRecentMatches: (hours = 24) => api.get(`/log-configs/matches/recent?hours=${hours}`),
  getMatchesForConfig: (id) => api.get(`/log-configs/${id}/matches`)
};

export const appSettingsAPI = {
  getCurrent: () => api.get('/app-settings'),
  save: (data) => api.post('/app-settings', data)
};

export const cacheAPI = {
  getStats: () => api.get('/monitoring-cache/stats'),
  getIntegrationStatuses: () => api.get('/monitoring-cache/integrations/status'),
  getIntegrationStatus: (id) => api.get(`/monitoring-cache/integrations/${id}/status`),
  getServiceStatuses: () => api.get('/monitoring-cache/services/status'),
  getServiceStatus: (id) => api.get(`/monitoring-cache/services/${id}/status`),
  clearAll: () => api.delete('/monitoring-cache/clear'),
  clearIntegration: (id) => api.delete(`/monitoring-cache/integrations/${id}/clear`),
  clearService: (id) => api.delete(`/monitoring-cache/services/${id}/clear`)
};

export default api;
