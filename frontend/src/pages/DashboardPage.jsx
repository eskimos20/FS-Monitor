import React, { useState, useEffect } from "react";
import { integrationAPI } from "../api/axios";
import api from "../api/axios";
import { 
  Plus, 
  FolderOpen, 
  Clock, 
  CheckCircle, 
  XCircle, 
  Settings,
  Calendar,
  FileText,
  Activity,
  Power,
  Server
} from 'lucide-react';

const DashboardPage = () => {
  const [integrations, setIntegrations] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [secondsUntilNextRun, setSecondsUntilNextRun] = useState(60);
  const [integrationTimers, setIntegrationTimers] = useState({});

  useEffect(() => {
    fetchIntegrations();
    fetchServices();
  }, []);

  // Fetch monitoring status and sync all timers from backend
  const fetchMonitoringStatus = async () => {
    try {
      const response = await api.get('/monitoring/status');
      setSecondsUntilNextRun(response.data.secondsUntilNextRun);
      
      // Sync integration timers from backend
      const backendTimers = response.data.integrationTimers || {};
      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, timerData] of Object.entries(backendTimers)) {
          newTimers[id] = timerData.secondsUntilNextRun;
        }
        return newTimers;
      });
    } catch (error) {
      console.error('Failed to fetch monitoring status:', error);
    }
  };

  // Sync countdown with backend
  useEffect(() => {
    fetchMonitoringStatus(); // Initial sync - get real values from backend
    
    const timer = setInterval(() => {
      setSecondsUntilNextRun(prev => {
        if (prev <= 1) {
          // Main timer reached 0 - backend is running now
          // Re-fetch real values from backend after a short delay
          setTimeout(() => {
            fetchIntegrations();
            fetchServices();
            fetchMonitoringStatus();
          }, 2000); // Wait 2s for backend to finish processing
          return 60; // Temporary value until backend responds
        }
        return prev - 1;
      });
      
      // Count down all integration timers by 1 second
      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, seconds] of Object.entries(prev)) {
          newTimers[id] = Math.max(0, seconds - 1);
        }
        return newTimers;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const fetchIntegrations = async () => {
    try {
      const response = await integrationAPI.getAll();
      setIntegrations(response.data);
    } catch (error) {
      setError('Failed to load integrations');
    } finally {
      setLoading(false);
    }
  };

  const fetchServices = async () => {
    try {
      const response = await api.get('/services');
      setServices(response.data);
    } catch (error) {
      console.log('Failed to load services:', error.message);
    }
  };

  const toggleIntegration = async (integrationId, currentStatus) => {
    try {
      const integration = integrations.find(i => i.id === integrationId);
      const newStatus = !currentStatus;
      
      const updatedIntegration = {
        ...integration,
        isActive: newStatus,
        monitoringEnabled: newStatus // Disable monitoring when inactive
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

  const formatTimeAgo = (minutes) => {
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${Math.round(minutes)} min ago`;
    
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = Math.round(minutes % 60);
    
    if (hours < 24) {
      if (remainingMinutes === 0) return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
      return `${hours}h ${remainingMinutes}m ago`;
    }
    
    const days = Math.floor(hours / 24);
    const remainingHours = hours % 24;
    
    if (days < 365) {
      if (remainingHours === 0 && remainingMinutes === 0) return `${days} day${days !== 1 ? 's' : ''} ago`;
      return `${days}d ${remainingHours}h ago`;
    }
    
    const years = Math.floor(days / 365);
    const remainingDays = days % 365;
    
    if (remainingDays === 0) return `${years} year${years !== 1 ? 's' : ''} ago`;
    return `${years}y ${remainingDays}d ago`;
  };

  const getStatusColor = (isActive, lastFileFound, thresholdMinutes) => {
    if (!isActive) return 'status-inactive';
    
    if (!lastFileFound) return 'status-inactive';
    
    const now = new Date();
    const lastFileTime = new Date(lastFileFound);
    const minutesSinceLastFile = (now - lastFileTime) / (1000 * 60);
    
    return minutesSinceLastFile <= thresholdMinutes ? 'status-active' : 'status-inactive';
  };

  const getStatusText = (isActive, lastFileFound, thresholdMinutes) => {
    if (!isActive) return 'Disabled';
    
    if (!lastFileFound) return 'No files found';
    
    const now = new Date();
    const lastFileTime = new Date(lastFileFound);
    const minutesSinceLastFile = (now - lastFileTime) / (1000 * 60);
    
    if (minutesSinceLastFile <= thresholdMinutes) {
      return 'Active';
    } else {
      return `Inactive (${formatTimeAgo(minutesSinceLastFile)})`;
    }
  };

  const formatCheckInterval = (integration) => {
    const value = integration.checkIntervalValue || 5;
    const unit = integration.checkIntervalUnit || 'MINUTES';
    return `${value} ${unit.toLowerCase()}`;
  };

  const getCheckIntervalMinutes = (integration) => {
    const value = integration.checkIntervalValue || 5;
    const unit = integration.checkIntervalUnit || 'MINUTES';
    
    switch (unit) {
      case 'MINUTES': return value;
      case 'HOURS': return value * 60;
      case 'DAYS': return value * 60 * 24;
      case 'WEEKS': return value * 60 * 24 * 7;
      case 'MONTHS': return value * 60 * 24 * 30; // Approximate
      default: return value;
    }
  };

  const formatIntegrationTimer = (integrationId) => {
    const integration = integrations.find(i => i.id === integrationId);
    
    // Don't show timer for inactive integrations
    if (!integration || !integration.isActive || !integration.monitoringEnabled) {
      return '--';
    }
    
    const seconds = integrationTimers[integrationId] || 0;
    if (seconds === 0) return '...';
    
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    
    if (minutes > 0) {
      return `${minutes}m ${remainingSeconds}s`;
    }
    return `${remainingSeconds}s`;
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return 'Never';
    
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600">Monitor your file integrations</p>
      </div>

      {/* Stats Cards */}
      <div className="space-y-6">
        {/* First Row - Integrations */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <FolderOpen className="h-8 w-8 text-primary-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Integrations: {integrations.length}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active: {integrations.filter(i => i.isActive && i.monitoringEnabled).length}</p>
              </div>
            </div>
          </div>

          {integrations.filter(i => !i.isActive || !i.monitoringEnabled).length > 0 && (
            <div className="card">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <XCircle className="h-8 w-8 text-red-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Inactive: {integrations.filter(i => !i.isActive || !i.monitoringEnabled).length}</p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Second Row - Services */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Server className="h-8 w-8 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Services: {services.length}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Server className="h-8 w-8 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active: {services.filter(s => s.isActive).length}</p>
              </div>
            </div>
          </div>

          {services.filter(s => !s.isActive).length > 0 && (
            <div className="card">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <Server className="h-8 w-8 text-gray-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Inactive: {services.filter(s => !s.isActive).length}</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* File Integrations List */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">File Integrations</h2>
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Activity className="h-4 w-4" />
            <span>Next check in {secondsUntilNextRun}s</span>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md text-sm mb-4">
            {error}
          </div>
        )}

        {integrations.length === 0 ? (
          <div className="text-center py-12">
            <FolderOpen className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No integrations</h3>
            <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first integration.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Integration
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Check Interval
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Next Run
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last File Found
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    On/Off
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {integrations.map((integration) => (
                  <tr key={integration.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="text-sm font-medium text-gray-900">{integration.name}</div>
                        <div className="text-sm text-gray-500 truncate max-w-xs">{integration.path}</div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className={`w-3 h-3 rounded-full mr-2 ${
                          getStatusColor(integration.isActive, integration.lastFileFound, integration.thresholdMinutes) === 'status-active' 
                            ? 'bg-green-500 animate-pulse' 
                            : 'bg-red-500'
                        }`}></div>
                        <span className={getStatusColor(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}>
                          {getStatusText(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div className="flex items-center">
                        <Clock className="h-4 w-4 mr-1" />
                        {formatCheckInterval(integration)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div className="flex items-center">
                        <Activity className="h-4 w-4 mr-1 text-primary-500" />
                        <span className="font-mono text-xs">{formatIntegrationTimer(integration.id)}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDateTime(integration.lastFileFound)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          className="sr-only peer"
                          checked={integration.isActive}
                          onChange={() => toggleIntegration(integration.id, integration.isActive)}
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                        <span className="ml-3 text-sm font-medium text-gray-700">
                          {integration.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </label>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Service Integrations List */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Service Integrations</h2>
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Server className="h-4 w-4" />
            <span>Service Monitoring</span>
          </div>
        </div>

        {services.length === 0 ? (
          <div className="text-center py-12">
            <Server className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No services configured</h3>
            <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first service integration.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Service</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Host:Port</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Last Check</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Active</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {services.map((service) => (
                  <tr key={service.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {service.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {service.type}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {service.host}:{service.port}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        service.status === 'ONLINE' 
                          ? 'bg-green-100 text-green-800'
                          : service.status === 'OFFLINE'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}>
                        {service.status || 'UNKNOWN'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {service.lastCheckedAt ? new Date(service.lastCheckedAt).toLocaleString() : 'Never'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          className="sr-only peer"
                          checked={service.isActive}
                          onChange={() => toggleService(service.id, service.isActive)}
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                        <span className="ml-3 text-sm font-medium text-gray-700">
                          {service.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </label>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
