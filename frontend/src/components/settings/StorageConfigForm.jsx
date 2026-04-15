import { useState, useEffect } from 'react';
import { X, FolderOpen, Copy } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const StorageConfigForm = ({ storageConfig, allStorageConfigs, onSave, onCancel }) => {
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [selectedToCopy, setSelectedToCopy] = useState('');
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

  const handleCopyExisting = () => {
    if (selectedToCopy) {
      const itemToCopy = allStorageConfigs.find(s => s.id === parseInt(selectedToCopy));
      if (itemToCopy) {
        const { id, createdAt, updatedAt, lastCheck, ...dataWithoutId } = itemToCopy;
        setFormData({ ...dataWithoutId, name: itemToCopy.name + ' (Copy)' });
        setShowCopyModal(false);
        setSelectedToCopy('');
      }
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-800">
              {storageConfig ? 'Edit Storage Config' : 'Add Storage Config'}
            </h3>
            <button onClick={onCancel} className="text-gray-400 hover:text-gray-600">
              <X className="h-5 w-5" />
            </button>
          </div>
          {!storageConfig && allStorageConfigs && allStorageConfigs.length > 0 && (
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

      {/* Copy Existing Modal */}
      {showCopyModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Copy Existing Storage Config</h3>
            <p className="text-sm text-gray-600 mb-4">Select a storage config to copy its settings:</p>
            
            <select
              value={selectedToCopy}
              onChange={(e) => setSelectedToCopy(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent mb-4"
            >
              <option value="">-- Select Storage Config --</option>
              {allStorageConfigs.map((item) => (
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

export default StorageConfigForm;
