import React, { useState, useEffect } from 'react';
import { X, Folder } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const IntegrationForm = ({ integration, fileTypes, onSave, onClose }) => {
  const [showFileBrowser, setShowFileBrowser] = useState(false);
  const [formData, setFormData] = useState({
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

  useEffect(() => {
    if (integration) {
      setFormData(integration);
    }
  }, [integration]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  const handlePathSelect = (path) => {
    if (path) {
      setFormData({ ...formData, path });
    }
    setShowFileBrowser(false);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h3 className="text-lg font-semibold text-gray-900">
            {integration ? 'Edit Integration' : 'New Integration'}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Basic Info */}
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Path</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={formData.path}
                  onChange={(e) => setFormData({ ...formData, path: e.target.value })}
                  className="input-field flex-1"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowFileBrowser(true)}
                  className="btn-secondary flex items-center gap-2"
                  title="Browse for folder"
                >
                  <Folder className="h-4 w-4" />
                  Browse
                </button>
              </div>
            </div>
          </div>

          {/* Intervals */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Check Interval</label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  value={formData.checkIntervalValue}
                  onChange={(e) => setFormData({ ...formData, checkIntervalValue: parseInt(e.target.value) })}
                  className="input-field"
                  min="1"
                />
                <select
                  value={formData.checkIntervalUnit}
                  onChange={(e) => setFormData({ ...formData, checkIntervalUnit: e.target.value })}
                  className="input-field"
                >
                  <option value="MINUTES">Minutes</option>
                  <option value="HOURS">Hours</option>
                  <option value="DAYS">Days</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Threshold</label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  value={formData.thresholdValue}
                  onChange={(e) => setFormData({ ...formData, thresholdValue: parseInt(e.target.value) })}
                  className="input-field"
                  min="1"
                />
                <select
                  value={formData.thresholdUnit}
                  onChange={(e) => setFormData({ ...formData, thresholdUnit: e.target.value })}
                  className="input-field"
                >
                  <option value="MINUTES">Minutes</option>
                  <option value="HOURS">Hours</option>
                  <option value="DAYS">Days</option>
                  <option value="MONTHS">Months</option>
                </select>
              </div>
            </div>
          </div>

          {/* Toggles */}
          <div className="flex gap-6">
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={formData.monitoringEnabled}
                onChange={(e) => setFormData({ ...formData, monitoringEnabled: e.target.checked })}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <span className="ml-2 text-sm text-gray-700">Monitoring Enabled</span>
            </label>
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={formData.cleanupEnabled}
                onChange={(e) => setFormData({ ...formData, cleanupEnabled: e.target.checked })}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <span className="ml-2 text-sm text-gray-700">Cleanup Enabled</span>
            </label>
          </div>

          {/* Cleanup Settings - shown when cleanup is enabled */}
          {formData.cleanupEnabled && (
            <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Delete files older than
              </label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  value={formData.cleanupAgeValue}
                  onChange={(e) => setFormData({ ...formData, cleanupAgeValue: parseInt(e.target.value) })}
                  className="input-field"
                  min="1"
                  required
                />
                <select
                  value={formData.cleanupAgeUnit}
                  onChange={(e) => setFormData({ ...formData, cleanupAgeUnit: e.target.value })}
                  className="input-field"
                >
                  <option value="DAYS">Days</option>
                  <option value="MONTHS">Months</option>
                  <option value="YEARS">Years</option>
                </select>
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Files older than {formData.cleanupAgeValue} {formData.cleanupAgeUnit.toLowerCase()} will be automatically deleted
              </p>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary">
              Save
            </button>
          </div>
        </form>

        {showFileBrowser && (
          <AdvancedFileBrowser
            isOpen={showFileBrowser}
            onPathSelect={handlePathSelect}
            onClose={() => setShowFileBrowser(false)}
          />
        )}
      </div>
    </div>
  );
};

export default IntegrationForm;
