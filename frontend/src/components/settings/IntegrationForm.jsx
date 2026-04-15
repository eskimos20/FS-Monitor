import React, { useState, useEffect } from 'react';
import { X, Folder, Copy } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const IntegrationForm = ({ integration, allIntegrations, fileTypes, onSave, onClose }) => {
  const [showFileBrowser, setShowFileBrowser] = useState(false);
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [selectedToCopy, setSelectedToCopy] = useState('');
  const [formData, setFormData] = useState({
    name: '',
    path: '',
    monitoringEnabled: true,
    checkIntervalValue: 5,
    checkIntervalUnit: 'MINUTES',
    thresholdValue: 15,
    thresholdUnit: 'MINUTES',
    monitorAllFiles: false,
    monitoredFileTypes: [],
    scheduleDays: [],
    scheduleEnabled: false,
    activeDays: '',
    activeStartHour: 0,
    activeEndHour: 24
  });

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

  const handleCopyExisting = () => {
    if (selectedToCopy) {
      const itemToCopy = allIntegrations.find(i => i.id === parseInt(selectedToCopy));
      if (itemToCopy) {
        const { id, ...dataWithoutId } = itemToCopy;
        setFormData({ ...dataWithoutId, name: itemToCopy.name + ' (Copy)' });
        setShowCopyModal(false);
        setSelectedToCopy('');
      }
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-semibold text-gray-900">
              {integration ? 'Edit Integration' : 'New Integration'}
            </h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="h-5 w-5" />
            </button>
          </div>
          {!integration && allIntegrations && allIntegrations.length > 0 && (
            <div className="mt-3">
              <button
                type="button"
                onClick={() => setShowCopyModal(true)}
                className="inline-flex items-center px-4 py-2 bg-yellow-400 hover:bg-yellow-500 text-gray-900 font-medium rounded-lg transition-colors"
              >
                <Copy className="h-4 w-4 mr-2" />
                Copy existing
              </button>
            </div>
          )}
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

      {/* Copy Existing Modal */}
      {showCopyModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Copy Existing Integration</h3>
            <p className="text-sm text-gray-600 mb-4">Select an integration to copy its settings:</p>
            
            <select
              value={selectedToCopy}
              onChange={(e) => setSelectedToCopy(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent mb-4"
            >
              <option value="">-- Select Integration --</option>
              {allIntegrations.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} ({item.path})
                </option>
              ))}
            </select>

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setShowCopyModal(false);
                  setSelectedToCopy('');
                }}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleCopyExisting}
                disabled={!selectedToCopy}
                className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default IntegrationForm;
