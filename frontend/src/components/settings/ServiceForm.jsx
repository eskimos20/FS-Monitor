import React, { useState, useEffect } from 'react';
import { X, Zap, CheckCircle, XCircle, Clock } from 'lucide-react';
import { serviceAPI } from '../../api/axios';

const DEFAULT_PORTS = {
  WEB: 80,
  HTTPS: 443,
  PING: 0,
  FTP: 21,
  SFTP: 22,
  SMB: 445,
  SSH: 22,
  MYSQL: 3306,
  POSTGRESQL: 5432,
  MONGODB: 27017,
  REDIS: 6379,
  MSSQL: 1433,
  DNS: 53,
  LDAP: 389,
  RDP: 3389,
  CUSTOM: 8080
};

const ServiceForm = ({ service, onSave, onClose }) => {
  const [formData, setFormData] = useState({
    name: '',
    type: 'WEB',
    host: '',
    port: 80,
    path: '/',
    isActive: true,
    checkIntervalMinutes: 5,
    checkMethod: 'TCP',
    useCredentials: false,
    username: '',
    password: '',
    sharePath: '',
    privateKey: '',
    scheduleEnabled: false,
    activeDays: '',
    activeStartHour: 0,
    activeEndHour: 24
  });
  
  const [authMethod, setAuthMethod] = useState('password');

  const DAYS = [
    { key: 'MON', label: 'Mon' },
    { key: 'TUE', label: 'Tue' },
    { key: 'WED', label: 'Wed' },
    { key: 'THU', label: 'Thu' },
    { key: 'FRI', label: 'Fri' },
    { key: 'SAT', label: 'Sat' },
    { key: 'SUN', label: 'Sun' }
  ];

  const toggleDay = (dayKey) => {
    const currentDays = formData.activeDays ? formData.activeDays.split(',').filter(d => d) : [];
    const index = currentDays.indexOf(dayKey);
    if (index >= 0) {
      currentDays.splice(index, 1);
    } else {
      currentDays.push(dayKey);
    }
    setFormData({ ...formData, activeDays: currentDays.join(',') });
  };

  const isDaySelected = (dayKey) => {
    return formData.activeDays && formData.activeDays.split(',').includes(dayKey);
  };

  const [testResult, setTestResult] = useState(null);
  const [testing, setTesting] = useState(false);

  useEffect(() => {
    if (service) {
      setFormData({
        ...service,
        checkMethod: service.checkMethod || 'TCP'
      });
    }
  }, [service]);

  const handleTypeChange = (newType) => {
    const updates = {
      type: newType,
      port: DEFAULT_PORTS[newType] || 80
    };
    
    if (newType === 'CUSTOM') {
      updates.checkMethod = 'TCP';
    }
    
    setFormData({
      ...formData,
      ...updates
    });
    setTestResult(null);
  };

  const handleTestConnection = async () => {
    if (!formData.host || !formData.port) {
      alert('Please enter host and port first');
      return;
    }

    setTesting(true);
    setTestResult(null);

    try {
      const response = await serviceAPI.testConnection({
        host: formData.host,
        port: formData.port,
        path: formData.path
      });
      setTestResult(response.data);
      
      // Automatically set checkMethod to recommended method
      if (response.data.recommended) {
        setFormData(prev => ({
          ...prev,
          checkMethod: response.data.recommended
        }));
      }
    } catch (error) {
      console.error('Test connection failed:', error);
      setTestResult({
        error: 'Failed to test connection: ' + (error.response?.data?.error || error.message)
      });
    } finally {
      setTesting(false);
    }
  };

  const handleTestCredentials = async () => {
    if (!formData.host || !formData.username) {
      alert('Please enter host and username first');
      return;
    }

    setTesting(true);
    setTestResult(null);

    try {
      const response = await serviceAPI.testCredentials(formData);
      setTestResult(response.data);
    } catch (error) {
      console.error('Test credentials failed:', error);
      setTestResult({
        success: false,
        message: 'Failed to test credentials: ' + (error.response?.data?.error || error.message),
        error: true
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSelectMethod = (method) => {
    setFormData({
      ...formData,
      checkMethod: method
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    const dataToSave = {
      ...formData,
      checkMethod: formData.type === 'CUSTOM' ? formData.checkMethod : null
    };
    
    onSave(dataToSave);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center rounded-t-lg">
          <h3 className="text-lg font-semibold text-gray-900">
            {service ? 'Edit Service' : 'New Service'}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="input-field"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select
                value={formData.type}
                onChange={(e) => handleTypeChange(e.target.value)}
                className="input-field"
              >
                <optgroup label="Web Services">
                  <option value="WEB">HTTP (Web)</option>
                  <option value="HTTPS">HTTPS (Secure Web)</option>
                </optgroup>
                <optgroup label="File Transfer">
                  <option value="FTP">FTP</option>
                  <option value="SFTP">SFTP</option>
                  <option value="SMB">SMB (Windows Share)</option>
                </optgroup>
                <optgroup label="Databases">
                  <option value="MYSQL">MySQL</option>
                  <option value="POSTGRESQL">PostgreSQL</option>
                  <option value="MONGODB">MongoDB</option>
                  <option value="REDIS">Redis</option>
                  <option value="MSSQL">MS SQL Server</option>
                </optgroup>
                <optgroup label="Other Services">
                  <option value="SSH">SSH</option>
                  <option value="PING">Ping</option>
                  <option value="DNS">DNS</option>
                  <option value="LDAP">LDAP</option>
                  <option value="RDP">RDP (Remote Desktop)</option>
                </optgroup>
                <optgroup label="Custom">
                  <option value="CUSTOM">Custom Service</option>
                </optgroup>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Host</label>
              <input
                type="text"
                value={formData.host}
                onChange={(e) => setFormData({ ...formData, host: e.target.value })}
                className="input-field"
                placeholder="example.com"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Port {formData.type !== 'CUSTOM' && `(Default: ${DEFAULT_PORTS[formData.type] || 'N/A'})`}
              </label>
              <input
                type="number"
                value={formData.port}
                onChange={(e) => setFormData({ ...formData, port: parseInt(e.target.value) })}
                className="input-field"
                required
              />
            </div>
          </div>

          {(formData.type === 'WEB' || formData.type === 'HTTPS' || (formData.type === 'CUSTOM' && formData.checkMethod === 'HTTP')) && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Path</label>
              <input
                type="text"
                value={formData.path}
                onChange={(e) => setFormData({ ...formData, path: e.target.value })}
                className="input-field"
                placeholder="/"
              />
            </div>
          )}

          {/* Credentials Section for FTP, SFTP, SMB */}
          {(formData.type === 'FTP' || formData.type === 'SFTP' || formData.type === 'SMB') && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <div className="flex items-center mb-3">
                <input
                  type="checkbox"
                  id="useCredentials"
                  checked={formData.useCredentials}
                  onChange={(e) => setFormData({ ...formData, useCredentials: e.target.checked })}
                  className="rounded border-gray-300 text-primary-600 focus:ring-primary-500 h-4 w-4"
                />
                <label htmlFor="useCredentials" className="ml-2 text-sm font-medium text-gray-700">
                  Use Credentials (test actual {formData.type} access)
                </label>
              </div>

              {formData.useCredentials && (
                <div className="space-y-4 mt-4">
                  {/* Test Credentials Button */}
                  <div className="flex items-center justify-between bg-blue-50 border border-blue-200 rounded-lg p-3">
                    <div>
                      <p className="text-sm font-medium text-gray-900">Test Connection</p>
                      <p className="text-xs text-gray-600">Verify credentials and access</p>
                    </div>
                    <button
                      type="button"
                      onClick={handleTestCredentials}
                      disabled={testing || !formData.host || !formData.username}
                      className="btn-secondary flex items-center text-sm"
                    >
                      <Zap className="h-4 w-4 mr-1" />
                      {testing ? 'Testing...' : 'Test'}
                    </button>
                  </div>

                  {/* Test Result */}
                  {testResult && !testResult.error && testResult.serviceType && (
                    <div className={`rounded-lg p-4 ${testResult.success ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                      <div className="flex items-start space-x-3">
                        {testResult.success ? (
                          <CheckCircle className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
                        )}
                        <div className="flex-1">
                          <p className={`text-sm font-medium ${testResult.success ? 'text-green-900' : 'text-red-900'}`}>
                            {testResult.message}
                          </p>
                          {testResult.responseTime && (
                            <p className="text-xs text-gray-600 mt-1 flex items-center">
                              <Clock className="h-3 w-3 mr-1" />
                              Response time: {testResult.responseTime}ms
                            </p>
                          )}
                          {testResult.details && (
                            <pre className="text-xs text-gray-700 mt-2 whitespace-pre-wrap font-mono bg-white p-2 rounded">
                              {testResult.details}
                            </pre>
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {testResult && testResult.error && (
                    <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                      <div className="flex items-start space-x-3">
                        <XCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
                        <div className="flex-1">
                          <p className="text-sm font-medium text-red-900">{testResult.message}</p>
                        </div>
                      </div>
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Username {formData.type === 'SMB' && <span className="text-xs text-gray-500">(DOMAIN\user or user)</span>}
                      </label>
                      <input
                        type="text"
                        value={formData.username}
                        onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                        className="input-field"
                        placeholder={formData.type === 'SMB' ? 'DOMAIN\\username' : 'username'}
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Share/Path
                      </label>
                      <input
                        type="text"
                        value={formData.sharePath}
                        onChange={(e) => setFormData({ ...formData, sharePath: e.target.value })}
                        className="input-field"
                        placeholder={formData.type === 'SMB' ? '/share/folder' : '/path/to/folder'}
                      />
                    </div>
                  </div>

                  {formData.type === 'SFTP' && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Authentication Method</label>
                      <div className="flex gap-4 mb-3">
                        <label className="flex items-center cursor-pointer">
                          <input
                            type="radio"
                            name="authMethod"
                            value="password"
                            checked={authMethod === 'password'}
                            onChange={(e) => setAuthMethod(e.target.value)}
                            className="mr-2"
                          />
                          <span className="text-sm text-gray-700">Password</span>
                        </label>
                        <label className="flex items-center cursor-pointer">
                          <input
                            type="radio"
                            name="authMethod"
                            value="key"
                            checked={authMethod === 'key'}
                            onChange={(e) => setAuthMethod(e.target.value)}
                            className="mr-2"
                          />
                          <span className="text-sm text-gray-700">SSH Private Key</span>
                        </label>
                      </div>
                    </div>
                  )}

                  {(formData.type !== 'SFTP' || authMethod === 'password') && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                      <input
                        type="password"
                        value={formData.password}
                        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                        className="input-field"
                        placeholder="Enter password"
                      />
                    </div>
                  )}

                  {formData.type === 'SFTP' && authMethod === 'key' && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        SSH Private Key
                      </label>
                      <textarea
                        value={formData.privateKey}
                        onChange={(e) => setFormData({ ...formData, privateKey: e.target.value })}
                        className="input-field font-mono text-xs"
                        rows="6"
                        placeholder="-----BEGIN RSA PRIVATE KEY-----&#10;...&#10;-----END RSA PRIVATE KEY-----"
                      />
                      <p className="text-xs text-gray-500 mt-1">Paste your SSH private key here</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Test Connection Section - Available for all service types */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h4 className="text-sm font-medium text-gray-900">Test Connection</h4>
                <p className="text-xs text-gray-600">Test which methods work with {formData.host}:{formData.port}</p>
              </div>
              <button
                type="button"
                onClick={handleTestConnection}
                disabled={testing || !formData.host || !formData.port}
                className="btn-secondary flex items-center text-sm"
              >
                <Zap className="h-4 w-4 mr-1" />
                {testing ? 'Testing...' : 'Test All Methods'}
              </button>
            </div>

            {testResult && !testResult.error && (
              <div className="space-y-2">
                <div className="bg-white rounded-lg p-2 mb-3">
                  <p className="text-xs font-medium text-gray-700">
                    ✨ Recommended: <span className="text-primary-600">{testResult.recommended}</span>
                  </p>
                </div>

                {formData.type === 'CUSTOM' && (
                  <p className="text-xs font-medium text-gray-700 mb-2">Select a method to use:</p>
                )}
                
                {testResult.methods.map((method, index) => (
                  <div
                    key={index}
                    onClick={() => formData.type === 'CUSTOM' ? handleSelectMethod(method.method) : null}
                    className={`p-3 rounded-lg border ${formData.type === 'CUSTOM' ? 'cursor-pointer' : ''} transition-colors ${
                      formData.checkMethod === method.method && formData.type === 'CUSTOM'
                        ? 'border-primary-500 bg-primary-50 ring-2 ring-primary-200'
                        : method.success
                        ? 'border-green-200 bg-white hover:bg-green-50'
                        : 'border-red-200 bg-white opacity-75'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start space-x-3 flex-1">
                        {method.success ? (
                          <CheckCircle className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
                        )}
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-medium text-gray-900">{method.name}</p>
                            {testResult.recommended === method.method && (
                              <span className="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded">
                                Recommended
                              </span>
                            )}
                            {formData.checkMethod === method.method && formData.type === 'CUSTOM' && (
                              <span className="text-xs bg-primary-100 text-primary-800 px-2 py-0.5 rounded">
                                Selected
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-gray-500 mt-0.5">{method.description}</p>
                          <div className="mt-1 flex items-center gap-3">
                            <p className={`text-xs font-medium ${method.success ? 'text-green-600' : 'text-red-600'}`}>
                              {method.success ? '✓ Success' : '✗ Failed'}
                            </p>
                            <p className="text-xs text-gray-500">
                              Tested with {formData.host}:{formData.port}
                            </p>
                            {method.success && (
                              <div className="flex items-center text-xs text-gray-500">
                                <Clock className="h-3 w-3 mr-1" />
                                {method.responseTime}ms
                              </div>
                            )}
                          </div>
                          <p className="text-xs text-gray-600 mt-1 italic">{method.message}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {testResult && testResult.error && (
              <div className="bg-red-50 border border-red-200 rounded p-3">
                <p className="text-sm text-red-600">{testResult.error}</p>
              </div>
            )}

            {!testResult && (
              <div className="text-center py-4">
                <p className="text-xs text-gray-600">
                  Click "Test All Methods" to automatically detect which protocols work
                </p>
              </div>
            )}
          </div>

          {formData.type === 'CUSTOM' && !testResult && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Check Method (or test connection above for recommendation)
              </label>
              <select
                value={formData.checkMethod}
                onChange={(e) => setFormData({ ...formData, checkMethod: e.target.value })}
                className="input-field"
              >
                <option value="TCP">TCP Connection</option>
                <option value="HTTP">HTTP Request</option>
                <option value="PING">Ping</option>
              </select>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Check Interval (minutes)</label>
            <input
              type="number"
              value={formData.checkIntervalMinutes}
              onChange={(e) => setFormData({ ...formData, checkIntervalMinutes: parseInt(e.target.value) })}
              className="input-field"
              min="1"
            />
          </div>

          {/* Schedule Settings */}
          <div className="border border-gray-200 rounded-lg p-4">
            <label className="flex items-center mb-3">
              <input
                type="checkbox"
                checked={formData.scheduleEnabled || false}
                onChange={(e) => setFormData({ ...formData, scheduleEnabled: e.target.checked })}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <span className="ml-2 text-sm font-medium text-gray-700">Enable Schedule</span>
            </label>

            {formData.scheduleEnabled && (
              <div className="space-y-4 mt-3 pt-3 border-t border-gray-200">
                {/* Day Selection */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Active Days</label>
                  <div className="flex gap-2">
                    {DAYS.map(day => (
                      <button
                        key={day.key}
                        type="button"
                        onClick={() => toggleDay(day.key)}
                        className={`px-3 py-1.5 text-sm font-medium rounded-md border transition-colors ${
                          isDaySelected(day.key)
                            ? 'bg-primary-600 text-white border-primary-600'
                            : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                        }`}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Time Range */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Active Hours: {String(formData.activeStartHour || 0).padStart(2, '0')}:00 - {String(formData.activeEndHour || 24).padStart(2, '0')}:00
                  </label>
                  <div className="flex items-center gap-4">
                    <div className="flex-1">
                      <input
                        type="range"
                        min="0"
                        max="24"
                        value={formData.activeStartHour || 0}
                        onChange={(e) => {
                          const val = parseInt(e.target.value);
                          if (val < (formData.activeEndHour || 24)) {
                            setFormData({ ...formData, activeStartHour: val });
                          }
                        }}
                        className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
                      />
                    </div>
                    <span className="text-sm text-gray-500 w-8">to</span>
                    <div className="flex-1">
                      <input
                        type="range"
                        min="0"
                        max="24"
                        value={formData.activeEndHour || 24}
                        onChange={(e) => {
                          const val = parseInt(e.target.value);
                          if (val > (formData.activeStartHour || 0)) {
                            setFormData({ ...formData, activeEndHour: val });
                          }
                        }}
                        className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
                      />
                    </div>
                  </div>
                  <div className="flex justify-between text-xs text-gray-400 mt-1">
                    <span>00:00</span>
                    <span>06:00</span>
                    <span>12:00</span>
                    <span>18:00</span>
                    <span>24:00</span>
                  </div>
                </div>

                <p className="text-xs text-gray-500">
                  Monitoring will only run on selected days between the specified hours.
                </p>
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary">
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ServiceForm;
