import { useState, useEffect } from 'react';
import { X, FolderOpen } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const StorageConfigForm = ({ storageConfig, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    name: '',
    path: '',
    checkIntervalMinutes: 60,
    intervalUnit: 'MINUTES',
    recursive: true,
    active: true
  });
  const [showFileBrowser, setShowFileBrowser] = useState(false);

  useEffect(() => {
    if (storageConfig) {
      setFormData({
        name: storageConfig.name || '',
        path: storageConfig.path || '',
        recursive: storageConfig.recursive !== undefined ? storageConfig.recursive : true,
        checkIntervalMinutes: storageConfig.checkIntervalMinutes || 60,
        intervalUnit: storageConfig.intervalUnit || 'MINUTES',
        active: storageConfig.active !== undefined ? storageConfig.active : true
      });
    }
  }, [storageConfig]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  const handlePathSelect = (path) => {
    setFormData({ ...formData, path });
    setShowFileBrowser(false);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-800">
            {storageConfig ? 'Edit Storage Config' : 'Add Storage Config'}
          </h3>
          <button onClick={onCancel} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Name
            </label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              className="input-field"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Path
            </label>
            <div className="flex space-x-2">
              <input
                type="text"
                name="path"
                value={formData.path}
                onChange={handleChange}
                className="input-field flex-1"
                required
              />
              <button
                type="button"
                onClick={() => setShowFileBrowser(true)}
                className="btn-secondary flex items-center gap-2"
              >
                <FolderOpen className="h-4 w-4" />
                Browse
              </button>
            </div>
          </div>

          <div>
            <label className="flex items-center space-x-2">
              <input
                type="checkbox"
                name="recursive"
                checked={formData.recursive}
                onChange={handleChange}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">Scan subdirectories (recursive)</span>
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Check Interval
            </label>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="number"
                name="checkIntervalMinutes"
                value={formData.checkIntervalMinutes}
                onChange={handleChange}
                className="input-field"
                min="1"
                required
              />
              <select
                name="intervalUnit"
                value={formData.intervalUnit}
                onChange={handleChange}
                className="input-field"
              >
                <option value="MINUTES">Minutes</option>
                <option value="HOURS">Hours</option>
                <option value="DAYS">Days</option>
                <option value="MONTHS">Months</option>
              </select>
            </div>
          </div>

          <div>
            <label className="flex items-center space-x-2">
              <input
                type="checkbox"
                name="active"
                checked={formData.active}
                onChange={handleChange}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">Active</span>
            </label>
          </div>

          <div className="flex justify-end space-x-2 pt-4">
            <button type="button" onClick={onCancel} className="btn-secondary">
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
            allowFileSelection={false}
          />
        )}
      </div>
    </div>
  );
};

export default StorageConfigForm;
