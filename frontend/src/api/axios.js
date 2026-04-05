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

export default api;
