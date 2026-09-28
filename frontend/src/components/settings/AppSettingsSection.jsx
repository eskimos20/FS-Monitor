import React, { useState, useEffect } from 'react';
import { Pencil, Save, X } from 'lucide-react';
import FormField from '../ui/FormField';
import DetailItem from '../ui/DetailItem';

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
          <button onClick={() => setIsEditing(true)} className="btn-secondary">
            <Pencil className="h-4 w-4 mr-1.5" />
            Edit Settings
          </button>
        </div>

        <div className="bg-surface-50/70 border border-surface-200 rounded-xl p-5">
          <DetailItem label="Dashboard Refresh Interval">
            {formData.refreshIntervalSeconds} seconds
          </DetailItem>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <FormField
        label="Dashboard Refresh Interval (seconds)"
        type="number"
        min="1"
        max="60"
        value={formData.refreshIntervalSeconds}
        onChange={(e) => setFormData({ ...formData, refreshIntervalSeconds: parseInt(e.target.value) || 5 })}
        className="w-32"
        hint="How often CPU and Memory stats refresh on the Dashboard (1–60 seconds)"
        required
      />

      <div className="flex justify-end gap-2 pt-4 border-t border-surface-200">
        {appSettings && (
          <button type="button" onClick={handleCancel} className="btn-secondary">
            <X className="h-4 w-4 mr-1.5" />
            Cancel
          </button>
        )}
        <button type="submit" className="btn-primary">
          <Save className="h-4 w-4 mr-1.5" />
          Save Settings
        </button>
      </div>
    </form>
  );
};

export default AppSettingsSection;
