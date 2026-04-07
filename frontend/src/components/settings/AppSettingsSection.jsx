import React, { useState, useEffect } from 'react';
import { Edit, Save, X } from 'lucide-react';

const AppSettingsSection = ({ appSettings, onSave }) => {
  const [isEditing, setIsEditing] = useState(!appSettings);
  const [formData, setFormData] = useState({
    refreshIntervalSeconds: appSettings?.refreshIntervalSeconds || 5
  });

  useEffect(() => {
    if (appSettings) {
      setFormData({
        refreshIntervalSeconds: appSettings.refreshIntervalSeconds || 5
      });
      setIsEditing(false);
    }
  }, [appSettings]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    await onSave(formData);
    setIsEditing(false);
  };

  const handleCancel = () => {
    if (appSettings) {
      setFormData({
        refreshIntervalSeconds: appSettings.refreshIntervalSeconds || 5
      });
    }
    setIsEditing(false);
  };

  if (!isEditing) {
    return (
      <div className="space-y-4">
        <div className="flex justify-end">
          <button
            onClick={() => setIsEditing(true)}
            className="btn-secondary flex items-center"
          >
            <Edit className="h-4 w-4 mr-2" />
            Edit Settings
          </button>
        </div>

        <div className="bg-gray-50 rounded-lg p-4 space-y-3">
          <div>
            <p className="text-sm font-medium text-gray-500">Dashboard Refresh Interval</p>
            <p className="text-sm text-gray-900">{formData.refreshIntervalSeconds} seconds</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Dashboard Refresh Interval (seconds)
        </label>
        <input
          type="number"
          min="1"
          max="60"
          value={formData.refreshIntervalSeconds}
          onChange={(e) => setFormData({ ...formData, refreshIntervalSeconds: parseInt(e.target.value) || 5 })}
          className="input-field w-32"
          required
        />
        <p className="text-xs text-gray-500 mt-1">
          How often CPU and Memory stats refresh on the Dashboard (1-60 seconds)
        </p>
      </div>

      <div className="flex justify-end gap-3 pt-4 border-t">
        {appSettings && (
          <button type="button" onClick={handleCancel} className="btn-secondary flex items-center">
            <X className="h-4 w-4 mr-2" />
            Cancel
          </button>
        )}
        <button type="submit" className="btn-primary flex items-center">
          <Save className="h-4 w-4 mr-2" />
          Save Settings
        </button>
      </div>
    </form>
  );
};

export default AppSettingsSection;
