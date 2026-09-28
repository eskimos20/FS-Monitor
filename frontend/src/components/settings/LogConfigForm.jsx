import React, { useState, useEffect } from 'react';
import { X, Plus, Trash2, Folder, Copy } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const LogConfigForm = ({ logConfig, allLogConfigs, onSave, onCancel }) => {
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [selectedToCopy, setSelectedToCopy] = useState('');
  const [formData, setFormData] = useState({
    name: '',
    path: '',
    fileTypes: '.log,.txt',
    keywords: '',
    checkIntervalMinutes: 5,
    recursive: true,
    active: true
  });
  const [showFileBrowser, setShowFileBrowser] = useState(false);

  useEffect(() => {
    if (logConfig) {
      setFormData({
        name: logConfig.name || '',
        path: logConfig.path || '',
        fileTypes: logConfig.fileTypes || '.log,.txt',
        keywords: logConfig.keywords || '',
        checkIntervalMinutes: logConfig.checkIntervalMinutes || 5,
        recursive: logConfig.recursive !== undefined ? logConfig.recursive : true,
        active: logConfig.active !== undefined ? logConfig.active : true
      });
    }
  }, [logConfig]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handlePathSelect = (path) => {
    if (path) {
      setFormData({ ...formData, path });
    }
    setShowFileBrowser(false);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  const handleCopyExisting = () => {
    if (selectedToCopy) {
      const itemToCopy = allLogConfigs.find(l => l.id === parseInt(selectedToCopy));
      if (itemToCopy) {
        const { id, createdAt, updatedAt, lastCheck, ...dataWithoutId } = itemToCopy;
        setFormData({ ...dataWithoutId, name: itemToCopy.name + ' (Copy)' });
        setShowCopyModal(false);
        setSelectedToCopy('');
      }
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-panel max-w-2xl">
        <div className="modal-header">
          <div className="flex justify-between items-center">
            <h2 className="modal-title">
              {logConfig ? 'Edit Log Configuration' : 'Add Log Configuration'}
            </h2>
            <button
              onClick={onCancel}
              className="text-surface-400 hover:text-surface-600"
          >
            <X className="h-6 w-6" />
          </button>
          </div>
          {!logConfig && allLogConfigs && allLogConfigs.length > 0 && (
            <div className="mt-3">
              <button
                type="button"
                onClick={() => setShowCopyModal(true)}
                className="btn-secondary"
              >
                <Copy className="h-4 w-4 mr-2" />
                Copy existing
              </button>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Configuration Name *
            </label>
            <input
              type="text"
              name="name"
              required
              className="input-field"
              placeholder="e.g., Application Logs"
              value={formData.name}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Log Path *
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                name="path"
                required
                className="input-field flex-1"
                placeholder="/var/log/app or /var/log/app/application.log"
                value={formData.path}
                onChange={handleChange}
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
            <p className="mt-1 text-sm text-surface-500">
              Path to a directory (searches all matching files) or a specific log file
            </p>
            
            <div className="mt-3 flex items-center">
              <input
                type="checkbox"
                id="recursive"
                name="recursive"
                checked={formData.recursive}
                onChange={handleChange}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-surface-300 rounded"
              />
              <label htmlFor="recursive" className="ml-2 block text-sm text-surface-700">
                Search subdirectories recursively
              </label>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              File Types *
            </label>
            <input
              type="text"
              name="fileTypes"
              required
              className="input-field"
              placeholder=".log,.txt"
              value={formData.fileTypes}
              onChange={handleChange}
            />
            <p className="mt-1 text-sm text-surface-500">
              Comma-separated file extensions (e.g., .log,.txt,.out)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Keywords to Search *
            </label>
            <textarea
              name="keywords"
              required
              rows="3"
              className="input-field"
              placeholder="Exception,ERROR,4002,FATAL"
              value={formData.keywords}
              onChange={handleChange}
            />
            <p className="mt-1 text-sm text-surface-500">
              Comma-separated keywords to search for in log files
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Check Interval (minutes) *
            </label>
            <input
              type="number"
              name="checkIntervalMinutes"
              required
              min="1"
              className="input-field"
              placeholder="5"
              value={formData.checkIntervalMinutes}
              onChange={handleChange}
            />
            <p className="mt-1 text-sm text-surface-500">
              How often to check the log files for new matches
            </p>
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              name="active"
              id="active"
              className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-surface-300 rounded"
              checked={formData.active}
              onChange={handleChange}
            />
            <label htmlFor="active" className="ml-2 block text-sm text-surface-900">
              Active
            </label>
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-surface-200">
            <button
              type="button"
              onClick={onCancel}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
            >
              {logConfig ? 'Update' : 'Create'}
            </button>
          </div>
        </form>

        {showFileBrowser && (
          <AdvancedFileBrowser
            isOpen={showFileBrowser}
            onPathSelect={handlePathSelect}
            onClose={() => setShowFileBrowser(false)}
            allowFileSelection={true}
          />
        )}
      </div>

      {/* Copy Existing Modal */}
      {showCopyModal && (
        <div className="modal-overlay z-[60]">
          <div className="modal-panel max-w-md p-6">
            <h3 className="text-lg font-semibold text-surface-900 mb-4">Copy Existing Log Config</h3>
            <p className="text-sm text-surface-600 mb-4">Select a log config to copy its settings:</p>
            
            <select
              value={selectedToCopy}
              onChange={(e) => setSelectedToCopy(e.target.value)}
              className="input-field mb-4"
            >
              <option value="">-- Select Log Config --</option>
              {allLogConfigs.map((item) => (
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

export default LogConfigForm;
