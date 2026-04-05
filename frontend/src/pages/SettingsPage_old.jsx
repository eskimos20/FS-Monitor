import React, { useState, useEffect } from "react";
import { integrationAPI, fileTypeAPI, mailConfigAPI } from "../api/axios";
import serviceAPI from "../services/serviceAPI";
import { 
  Plus, 
  FolderOpen, 
  Save, 
  Trash2, 
  Edit, 
  X,
  Check,
  Clock,
  Calendar,
  FileText,
  Settings,
  Server,
  Activity,
  AlertCircle,
  CheckCircle
} from 'lucide-react';

const SettingsPage = () => {
  const [integrations, setIntegrations] = useState([]);
  const [fileTypes, setFileTypes] = useState([]);
  const [mailConfig, setMailConfig] = useState(null);
  const [services, setServices] = useState([]);
  const [serviceTypes, setServiceTypes] = useState([]);
  const [showIntegrationForm, setShowIntegrationForm] = useState(false);
  const [showFileTypeForm, setShowFileTypeForm] = useState(false);
  const [showMailConfigForm, setShowMailConfigForm] = useState(false);
  const [showServiceForm, setShowServiceForm] = useState(false);
  const [editingIntegration, setEditingIntegration] = useState(null);
  const [editingService, setEditingService] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState('integrations');

  const [integrationForm, setIntegrationForm] = useState({
    name: '',
    path: '',
    monitoringEnabled: true,
    cleanupEnabled: false,
    checkIntervalValue: 5,
    checkIntervalUnit: 'MINUTES',
    thresholdValue: 15,
    thresholdUnit: 'MINUTES',
    maxFileAgeMonths: 6,
    monitorAllFiles: false,
    monitoredFileTypes: [],
    scheduleDays: []
  });

  const [fileTypeForm, setFileTypeForm] = useState({
    extension: '',
    description: ''
  });

  const [mailConfigForm, setMailConfigForm] = useState({
    host: '',
    port: 587,
    username: '',
    password: '',
    fromName: '',
    fromEmail: '',
    smtpAuth: true,
    smtpStarttls: true
  });

  const [serviceForm, setServiceForm] = useState({
    name: '',
    type: 'WEB',
    host: '',
    port: '',
    path: '',
    isActive: true,
    checkIntervalMinutes: 5
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [integrationsRes, fileTypesRes, servicesRes, serviceTypesRes] = await Promise.all([
        integrationAPI.getAll(),
        fileTypeAPI.getAll(),
        serviceAPI.getAll(),
        serviceAPI.getTypes()
      ]);
      
      setIntegrations(integrationsRes.data);
      setFileTypes(fileTypesRes.data);
      setServices(servicesRes.data);
      setServiceTypes(serviceTypesRes.data);
      setLoading(false);
    } catch (err) {
      setError('Failed to fetch data');
      setLoading(false);
    }
  };

  const handleServiceSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    
    try {
      const serviceData = {
        ...serviceForm,
        port: serviceForm.port ? parseInt(serviceForm.port) : null,
        checkIntervalMinutes: parseInt(serviceForm.checkIntervalMinutes)
      };

      if (editingService) {
        await serviceAPI.update(editingService.id, serviceData);
        setSuccess('Service updated successfully');
      } else {
        await serviceAPI.create(serviceData);
        setSuccess('Service created successfully');
      }
      
      setShowServiceForm(false);
      setEditingService(null);
      resetServiceForm();
      fetchData();
    } catch (err) {
      setError('Failed to save service');
    }
  };

  const handleServiceEdit = (service) => {
    setEditingService(service);
    setServiceForm({
      name: service.name,
      type: service.type,
      host: service.host,
      port: service.port || '',
      path: service.path || '',
      isActive: service.isActive,
      checkIntervalMinutes: service.checkIntervalMinutes
    });
    setShowServiceForm(true);
  };

  const handleServiceDelete = async (serviceId) => {
    if (window.confirm('Are you sure you want to delete this service?')) {
      try {
        await serviceAPI.delete(serviceId);
        setSuccess('Service deleted successfully');
        fetchData();
      } catch (err) {
        setError('Failed to delete service');
      }
    }
  };

  const resetServiceForm = () => {
    setServiceForm({
      name: '',
      type: 'WEB',
      host: '',
      port: '',
      path: '',
      isActive: true,
      checkIntervalMinutes: 5
    });
  };

  const handleIntegrationSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      if (editingIntegration) {
        await integrationAPI.update(editingIntegration.id, integrationForm);
        setSuccess('Integration updated successfully!');
      } else {
        await integrationAPI.create(integrationForm);
        setSuccess('Integration created successfully!');
      }
      
      setShowIntegrationForm(false);
      setEditingIntegration(null);
      resetIntegrationForm();
      fetchData();
    } catch (error) {
      setError('Failed to save integration');
    }
  };

  const handleFileTypeSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      await fileTypeAPI.create(fileTypeForm);
      setSuccess('File type added successfully!');
      setShowFileTypeForm(false);
      resetFileTypeForm();
      fetchData();
    } catch (error) {
      setError('Failed to add file type');
    }
  };

  const deleteIntegration = async (id) => {
    if (!window.confirm('Are you sure you want to delete this integration?')) return;

    try {
      await integrationAPI.delete(id);
      setSuccess('Integration deleted successfully!');
      fetchData();
    } catch (error) {
      setError('Failed to delete integration');
    }
  };

  const deleteFileType = async (id) => {
    if (!window.confirm('Are you sure you want to delete this file type?')) return;
    
    try {
      await fileTypeAPI.delete(id);
      setFileTypes(fileTypes.filter(ft => ft.id !== id));
      setSuccess('File type deleted successfully');
    } catch (error) {
      setError('Failed to delete file type');
    }
  };

  const handleMailConfigSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    
    try {
      if (mailConfig) {
        await mailConfigAPI.update(mailConfig.id, mailConfigForm);
      } else {
        await mailConfigAPI.save(mailConfigForm);
      }
      const response = await mailConfigAPI.getCurrent();
      setMailConfig(response.data);
      setShowMailConfigForm(false);
      setSuccess('Mail configuration saved successfully');
    } catch (error) {
      setError('Failed to save mail configuration');
    }
  };

  const resetMailConfigForm = () => {
    setMailConfigForm({
      host: '',
      port: 587,
      username: '',
      password: '',
      fromName: '',
      fromEmail: '',
      smtpAuth: true,
      smtpStarttls: true
    });
    setShowMailConfigForm(false);
  };

  const editIntegration = (integration) => {
    setEditingIntegration(integration);
    setIntegrationForm({
      name: integration.name,
      path: integration.path,
      monitoringEnabled: integration.monitoringEnabled,
      cleanupEnabled: integration.cleanupEnabled,
      checkIntervalValue: integration.checkIntervalValue || 5,
      checkIntervalUnit: integration.checkIntervalUnit || 'MINUTES',
      thresholdValue: integration.thresholdValue || 15,
      thresholdUnit: integration.thresholdUnit || 'MINUTES',
      cleanupAgeValue: integration.cleanupAgeValue || 6,
      cleanupAgeUnit: integration.cleanupAgeUnit || 'MONTHS',
      monitorAllFiles: integration.monitorAllFiles || false,
      monitoredFileTypes: integration.monitoredFileTypes || [],
      scheduleDays: integration.scheduleDays || []
    });
    setShowIntegrationForm(true);
  };

  const resetIntegrationForm = () => {
    setIntegrationForm({
      name: '',
      path: '',
      monitoringEnabled: true,
      cleanupEnabled: false,
      checkIntervalValue: 5,
      checkIntervalUnit: 'MINUTES',
      thresholdValue: 15,
      thresholdUnit: 'MINUTES',
      cleanupAgeValue: 6,
      cleanupAgeUnit: 'MONTHS',
      monitorAllFiles: false,
      monitoredFileTypes: [],
      scheduleDays: []
    });
  };

  const toggleFileType = (fileType) => {
    const current = integrationForm.monitoredFileTypes || [];
    const exists = current.some(ft => ft.id === fileType.id);
    if (exists) {
      setIntegrationForm({
        ...integrationForm,
        monitoredFileTypes: current.filter(ft => ft.id !== fileType.id)
      });
    } else {
      setIntegrationForm({
        ...integrationForm,
        monitoredFileTypes: [...current, fileType]
      });
    }
  };

  const isFileTypeSelected = (fileType) => {
    return (integrationForm.monitoredFileTypes || []).some(ft => ft.id === fileType.id);
  };

  const resetFileTypeForm = () => {
    setFileTypeForm({
      extension: '',
      description: ''
    });
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
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600">Configure your monitoring integrations and file types</p>
      </div>

      {/* Alerts */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md text-sm">
          {error}
        </div>
      )}

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-600 px-4 py-3 rounded-md text-sm">
          {success}
        </div>
      )}

      {/* Mail Configuration Section */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Mail Configuration</h2>
          <button 
            onClick={() => {
              resetMailConfigForm();
              setShowMailConfigForm(true);
            }}
            className="btn-primary flex items-center"
          >
            <Settings className="h-4 w-4 mr-2" />
            {mailConfig ? 'Edit Mail Config' : 'Configure Mail'}
          </button>
        </div>

        {showMailConfigForm && (
          <div className="mb-6 p-6 border border-gray-200 rounded-lg bg-gray-50">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-md font-medium text-gray-900">
                {mailConfig ? 'Edit Mail Configuration' : 'Configure Mail Settings'}
              </h3>
              <button 
                onClick={() => setShowMailConfigForm(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleMailConfigSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    SMTP Host
                  </label>
                  <input
                    type="text"
                    value={mailConfigForm.host}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, host: e.target.value})}
                    className="input-field"
                    placeholder="smtp.gmail.com"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Port
                  </label>
                  <input
                    type="number"
                    value={mailConfigForm.port}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, port: parseInt(e.target.value)})}
                    className="input-field"
                    placeholder="587"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Username
                  </label>
                  <input
                    type="text"
                    value={mailConfigForm.username}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, username: e.target.value})}
                    className="input-field"
                    placeholder="your-email@gmail.com"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Password
                  </label>
                  <input
                    type="password"
                    value={mailConfigForm.password}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, password: e.target.value})}
                    className="input-field"
                    placeholder="Your app password"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    From Name
                  </label>
                  <input
                    type="text"
                    value={mailConfigForm.fromName}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, fromName: e.target.value})}
                    className="input-field"
                    placeholder="FS-Monitor"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    From Email
                  </label>
                  <input
                    type="email"
                    value={mailConfigForm.fromEmail}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, fromEmail: e.target.value})}
                    className="input-field"
                    placeholder="noreply@fsmonitor.com"
                    required
                  />
                </div>
              </div>

              <div className="flex items-center space-x-4">
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    checked={mailConfigForm.smtpAuth}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, smtpAuth: e.target.checked})}
                    className="mr-2"
                  />
                  <span className="text-sm text-gray-700">SMTP Authentication</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    checked={mailConfigForm.smtpStarttls}
                    onChange={(e) => setMailConfigForm({...mailConfigForm, smtpStarttls: e.target.checked})}
                    className="mr-2"
                  />
                  <span className="text-sm text-gray-700">STARTTLS</span>
                </label>
              </div>

              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={resetMailConfigForm}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary flex items-center"
                >
                  <Save className="h-4 w-4 mr-2" />
                  Save Configuration
                </button>
              </div>
            </form>
          </div>
        )}

        {!mailConfig && !showMailConfigForm && (
          <div className="text-center py-8">
            <Settings className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No mail configuration</h3>
            <p className="mt-1 text-sm text-gray-500">Configure SMTP settings to enable email notifications.</p>
          </div>
        )}

        {mailConfig && !showMailConfigForm && (
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <div>
                <p className="text-sm font-medium text-gray-900">SMTP Server</p>
                <p className="text-sm text-gray-500">{mailConfig.host}:{mailConfig.port}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-medium text-gray-900">From</p>
                <p className="text-sm text-gray-500">{mailConfig.fromName} &lt;{mailConfig.fromEmail}&gt;</p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Integrations Section */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Integrations</h2>
          <button 
            onClick={() => {
              resetIntegrationForm();
              setEditingIntegration(null);
              setShowIntegrationForm(true);
            }}
            className="btn-primary flex items-center"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Integration
          </button>
        </div>

        {showIntegrationForm && (
          <div className="mb-6 p-6 border border-gray-200 rounded-lg bg-gray-50">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-md font-medium text-gray-900">
                {editingIntegration ? 'Edit Integration' : 'Add New Integration'}
              </h3>
              <button 
                onClick={() => {
                  setShowIntegrationForm(false);
                  setEditingIntegration(null);
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleIntegrationSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Integration Name
                  </label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    placeholder="e.g., 01_T4_RD"
                    value={integrationForm.name}
                    onChange={(e) => setIntegrationForm({...integrationForm, name: e.target.value})}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Path
                  </label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    placeholder="/path/to/monitor"
                    value={integrationForm.path}
                    onChange={(e) => setIntegrationForm({...integrationForm, path: e.target.value})}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Check Interval
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="number"
                      min="1"
                      required
                      className="input-field"
                      value={integrationForm.checkIntervalValue}
                      onChange={(e) => setIntegrationForm({...integrationForm, checkIntervalValue: parseInt(e.target.value)})}
                    />
                    <select
                      className="input-field"
                      value={integrationForm.checkIntervalUnit}
                      onChange={(e) => setIntegrationForm({...integrationForm, checkIntervalUnit: e.target.value})}
                    >
                      <option value="MINUTES">Minutes</option>
                      <option value="HOURS">Hours</option>
                      <option value="DAYS">Days</option>
                      <option value="WEEKS">Weeks</option>
                      <option value="MONTHS">Months</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Threshold (max file age)
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="number"
                      min="1"
                      required
                      className="input-field"
                      value={integrationForm.thresholdValue}
                      onChange={(e) => setIntegrationForm({...integrationForm, thresholdValue: parseInt(e.target.value)})}
                    />
                    <select
                      className="input-field"
                      value={integrationForm.thresholdUnit}
                      onChange={(e) => setIntegrationForm({...integrationForm, thresholdUnit: e.target.value})}
                    >
                      <option value="MINUTES">Minutes</option>
                      <option value="HOURS">Hours</option>
                      <option value="DAYS">Days</option>
                      <option value="WEEKS">Weeks</option>
                      <option value="MONTHS">Months</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Cleanup Age (delete files older than)
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="number"
                      min="1"
                      required
                      className="input-field"
                      value={integrationForm.cleanupAgeValue}
                      onChange={(e) => setIntegrationForm({...integrationForm, cleanupAgeValue: parseInt(e.target.value)})}
                    />
                    <select
                      className="input-field"
                      value={integrationForm.cleanupAgeUnit}
                      onChange={(e) => setIntegrationForm({...integrationForm, cleanupAgeUnit: e.target.value})}
                    >
                      <option value="MINUTES">Minutes</option>
                      <option value="HOURS">Hours</option>
                      <option value="DAYS">Days</option>
                      <option value="WEEKS">Weeks</option>
                      <option value="MONTHS">Months</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* File Types Selection */}
              <div className="col-span-1 md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  File Types to Monitor
                </label>
                <label className="flex items-center mb-3">
                  <input
                    type="checkbox"
                    className="mr-2"
                    checked={integrationForm.monitorAllFiles}
                    onChange={(e) => setIntegrationForm({
                      ...integrationForm,
                      monitorAllFiles: e.target.checked,
                      monitoredFileTypes: e.target.checked ? [] : integrationForm.monitoredFileTypes
                    })}
                  />
                  <span className="text-sm font-medium text-gray-900">All files (monitor every file in directory)</span>
                </label>
                {!integrationForm.monitorAllFiles && (
                  <div className="flex flex-wrap gap-2">
                    {fileTypes.map((ft) => (
                      <button
                        key={ft.id}
                        type="button"
                        onClick={() => toggleFileType(ft)}
                        className={`px-3 py-1 rounded-full text-sm font-medium border transition-colors ${
                          isFileTypeSelected(ft)
                            ? 'bg-primary-600 text-white border-primary-600'
                            : 'bg-white text-gray-700 border-gray-300 hover:border-primary-400'
                        }`}
                      >
                        {ft.extension}
                      </button>
                    ))}
                    {fileTypes.length === 0 && (
                      <p className="text-sm text-gray-500">No file types defined. Add file types below first.</p>
                    )}
                  </div>
                )}
              </div>

              <div className="flex space-x-4 col-span-1 md:col-span-2">
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    className="mr-2"
                    checked={integrationForm.monitoringEnabled}
                    onChange={(e) => setIntegrationForm({...integrationForm, monitoringEnabled: e.target.checked})}
                  />
                  <span className="text-sm text-gray-700">Enable Monitoring</span>
                </label>

                <label className="flex items-center">
                  <input
                    type="checkbox"
                    className="mr-2"
                    checked={integrationForm.cleanupEnabled}
                    onChange={(e) => setIntegrationForm({...integrationForm, cleanupEnabled: e.target.checked})}
                  />
                  <span className="text-sm text-gray-700">Enable Cleanup</span>
                </label>
              </div>

              <div className="flex justify-end space-x-3">
                <button 
                  type="button"
                  onClick={() => {
                    setShowIntegrationForm(false);
                    setEditingIntegration(null);
                  }}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  <Save className="h-4 w-4 mr-2" />
                  {editingIntegration ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="space-y-3">
          {integrations.map((integration) => (
            <div key={integration.id} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50">
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <h3 className="text-md font-medium text-gray-900">{integration.name}</h3>
                  <p className="text-sm text-gray-500 mt-1">{integration.path}</p>
                  <div className="flex items-center space-x-4 mt-2 text-sm text-gray-600">
                    <span className="flex items-center">
                      <Clock className="h-4 w-4 mr-1" />
                      {integration.checkIntervalValue} {integration.checkIntervalUnit?.toLowerCase() || 'min'}
                    </span>
                    <span className="flex items-center">
                      <Calendar className="h-4 w-4 mr-1" />
                      Threshold: {integration.thresholdValue} {integration.thresholdUnit?.toLowerCase() || 'min'}
                    </span>
                    <span className="flex items-center">
                      <FileText className="h-4 w-4 mr-1" />
                      {integration.monitorAllFiles 
                        ? 'All files' 
                        : integration.monitoredFileTypes?.length > 0
                          ? integration.monitoredFileTypes.map(ft => ft.extension).join(', ')
                          : 'No file types selected'}
                    </span>
                  </div>
                </div>
                <div className="flex space-x-2">
                  <button 
                    onClick={() => editIntegration(integration)}
                    className="text-primary-600 hover:text-primary-900"
                  >
                    <Edit className="h-4 w-4" />
                  </button>
                  <button 
                    onClick={() => deleteIntegration(integration.id)}
                    className="text-red-600 hover:text-red-900"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* File Types Section */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">File Types</h2>
          <button 
            onClick={() => {
              resetFileTypeForm();
              setShowFileTypeForm(true);
            }}
            className="btn-primary flex items-center"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add File Type
          </button>
        </div>

        {showFileTypeForm && (
          <div className="mb-6 p-6 border border-gray-200 rounded-lg bg-gray-50">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-md font-medium text-gray-900">Add New File Type</h3>
              <button 
                onClick={() => setShowFileTypeForm(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleFileTypeSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Extension
                  </label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    placeholder=".pdf"
                    value={fileTypeForm.extension}
                    onChange={(e) => setFileTypeForm({...fileTypeForm, extension: e.target.value})}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    placeholder="PDF Document"
                    value={fileTypeForm.description}
                    onChange={(e) => setFileTypeForm({...fileTypeForm, description: e.target.value})}
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-3">
                <button 
                  type="button"
                  onClick={() => setShowFileTypeForm(false)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  <Save className="h-4 w-4 mr-2" />
                  Add File Type
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {fileTypes.map((fileType) => (
            <div key={fileType.id} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50">
              <div className="flex justify-between items-start">
                <div>
                  <div className="flex items-center">
                    <FileText className="h-5 w-5 text-primary-600 mr-2" />
                    <span className="font-medium text-gray-900">{fileType.extension}</span>
                  </div>
                  <p className="text-sm text-gray-500 mt-1">{fileType.description}</p>
                </div>
                <button 
                  onClick={() => deleteFileType(fileType.id)}
                  className="text-red-600 hover:text-red-900"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
