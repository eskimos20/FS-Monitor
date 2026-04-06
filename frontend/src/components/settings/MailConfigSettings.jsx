import React, { useState, useEffect } from 'react';
import { Edit, Save, X } from 'lucide-react';

const MailConfigSettings = ({ mailConfig, onSave }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [useAuthentication, setUseAuthentication] = useState(false);
  const [formData, setFormData] = useState({
    host: '',
    port: 587,
    fromEmail: '',
    toEmail: '',
    username: '',
    password: ''
  });

  useEffect(() => {
    if (mailConfig) {
      setFormData(mailConfig);
      setUseAuthentication(!!mailConfig.username);
    }
  }, [mailConfig]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const dataToSave = {
      ...formData,
      username: useAuthentication ? formData.username : '',
      password: useAuthentication ? formData.password : ''
    };
    
    await onSave(dataToSave);
    setIsEditing(false);
  };

  const handleCancel = () => {
    if (mailConfig) {
      setFormData(mailConfig);
      setUseAuthentication(!!mailConfig.username);
    }
    setIsEditing(false);
  };

  if (!isEditing && mailConfig) {
    return (
      <div className="space-y-4">
        <div className="flex justify-end">
          <button
            onClick={() => setIsEditing(true)}
            className="btn-secondary flex items-center"
          >
            <Edit className="h-4 w-4 mr-2" />
            Edit Configuration
          </button>
        </div>

        <div className="bg-gray-50 rounded-lg p-4 space-y-3">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">SMTP Host</p>
              <p className="text-sm text-gray-900">{mailConfig.host}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Port</p>
              <p className="text-sm text-gray-900">{mailConfig.port}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">From Email</p>
              <p className="text-sm text-gray-900">{mailConfig.fromEmail}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">To Email</p>
              <p className="text-sm text-gray-900">{mailConfig.toEmail}</p>
            </div>
          </div>
          {mailConfig.username && (
            <div>
              <p className="text-sm font-medium text-gray-500">Authentication</p>
              <p className="text-sm text-gray-900">Enabled ({mailConfig.username})</p>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">SMTP Host</label>
          <input
            type="text"
            value={formData.host}
            onChange={(e) => setFormData({ ...formData, host: e.target.value })}
            className="input-field"
            placeholder="smtp.gmail.com"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Port</label>
          <input
            type="number"
            value={formData.port}
            onChange={(e) => setFormData({ ...formData, port: parseInt(e.target.value) })}
            className="input-field"
            required
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">From Email (Sender)</label>
          <input
            type="email"
            value={formData.fromEmail}
            onChange={(e) => setFormData({ ...formData, fromEmail: e.target.value })}
            className="input-field"
            placeholder="noreply@company.com"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">To Email (Recipient)</label>
          <input
            type="email"
            value={formData.toEmail}
            onChange={(e) => setFormData({ ...formData, toEmail: e.target.value })}
            className="input-field"
            placeholder="admin@company.com"
            required
          />
        </div>
      </div>

      <div className="pt-2 border-t">
        <label className="flex items-center cursor-pointer">
          <input
            type="checkbox"
            checked={useAuthentication}
            onChange={(e) => setUseAuthentication(e.target.checked)}
            className="rounded border-gray-300 text-primary-600 focus:ring-primary-500 h-4 w-4"
          />
          <span className="ml-2 text-sm font-medium text-gray-700">
            Use SMTP Authentication (username & password)
          </span>
        </label>
      </div>

      {useAuthentication && (
        <div className="grid grid-cols-2 gap-4 pt-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input
              type="text"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              className="input-field"
              placeholder="your-email@gmail.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input
              type="password"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              className="input-field"
              placeholder="Your app password"
            />
          </div>
        </div>
      )}

      <div className="flex justify-end gap-3 pt-4 border-t">
        {mailConfig && (
          <button type="button" onClick={handleCancel} className="btn-secondary flex items-center">
            <X className="h-4 w-4 mr-2" />
            Cancel
          </button>
        )}
        <button type="submit" className="btn-primary flex items-center">
          <Save className="h-4 w-4 mr-2" />
          Save Configuration
        </button>
      </div>
    </form>
  );
};

export default MailConfigSettings;
