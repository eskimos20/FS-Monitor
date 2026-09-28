import React, { useState, useEffect } from 'react';
import { X, Folder, Copy } from 'lucide-react';
import AdvancedFileBrowser from '../AdvancedFileBrowser';

const DeleteServiceForm = ({ deleteService, allDeleteServices, onSave, onCancel }) => {
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [selectedToCopy, setSelectedToCopy] = useState('');
  const [formData, setFormData] = useState({
    name: '',
    path: '',
    fileTypes: '',
    cleanupEnabled: true,
    cleanupIntervalValue: 1,
    cleanupIntervalUnit: 'HOURS',
    deleteAgeValue: 30,
    deleteAgeUnit: 'DAYS',
    recursive: true,
    deleteEmptyDirectories: false
  });
  const [showFileBrowser, setShowFileBrowser] = useState(false);

  useEffect(() => {
    if (deleteService) {
      setFormData({
        name: deleteService.name || '',
        path: deleteService.path || '',
        fileTypes: deleteService.fileTypes || '',
        cleanupEnabled: deleteService.cleanupEnabled !== undefined ? deleteService.cleanupEnabled : true,
        cleanupIntervalValue: deleteService.cleanupIntervalValue || 1,
        cleanupIntervalUnit: deleteService.cleanupIntervalUnit || 'HOURS',
        deleteAgeValue: deleteService.deleteAgeValue || 30,
        deleteAgeUnit: deleteService.deleteAgeUnit || 'DAYS',
        recursive: deleteService.recursive !== undefined ? deleteService.recursive : true,
        deleteEmptyDirectories: deleteService.deleteEmptyDirectories !== undefined ? deleteService.deleteEmptyDirectories : false
      });
    }
  }, [deleteService]);

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
      const itemToCopy = allDeleteServices.find(s => s.id === parseInt(selectedToCopy));
      if (itemToCopy) {
        const { id, filesDeletedLastScan, foldersDeletedLastScan, lastCleanup, createdAt, updatedAt, ...dataWithoutId } = itemToCopy;
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
              {deleteService ? 'Edit Delete Service' : 'Add Delete Service'}
            </h2>
            <button
              onClick={onCancel}
              className="text-surface-400 hover:text-surface-600"
            >
              <X className="h-6 w-6" />
            </button>
          </div>
          {!deleteService && allDeleteServices && allDeleteServices.length > 0 && (
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
              Service Name *
            </label>
            <input
              type="text"
              name="name"
              required
              className="input-field"
              placeholder="e.g., Temp Files Cleanup"
              value={formData.name}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Directory Path *
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                name="path"
                required
                className="input-field flex-1"
                placeholder="/tmp or /var/log/old"
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
              Path to the directory where files should be deleted
            </p>
            
            <div className="mt-3 flex items-center gap-8">
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="recursive"
                  name="recursive"
                  checked={formData.recursive}
                  onChange={handleChange}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-surface-300 rounded"
                />
                <label htmlFor="recursive" className="ml-2 block text-sm text-surface-700">
                  Delete files in subdirectories recursively
                </label>
              </div>
              
              {formData.recursive && (
                <div className="flex items-center animate-pulse">
                  <input
                    type="checkbox"
                    name="deleteEmptyDirectories"
                    id="deleteEmptyDirectories"
                    className="h-4 w-4 text-red-600 focus:ring-red-500 border-surface-300 rounded"
                    checked={formData.deleteEmptyDirectories}
                    onChange={handleChange}
                  />
                  <label htmlFor="deleteEmptyDirectories" className="ml-2 block text-sm text-red-600 font-medium">
                    Delete Empty Directories
                  </label>
                </div>
              )}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              File Types (optional)
            </label>
            <input
              type="text"
              name="fileTypes"
              className="input-field"
              placeholder=".log,.tmp,.bak (leave empty for all files)"
              value={formData.fileTypes}
              onChange={handleChange}
            />
            <p className="mt-1 text-sm text-surface-500">
              Comma-separated file extensions. Leave empty to delete all files.
            </p>
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              name="cleanupEnabled"
              id="cleanupEnabled"
              className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-surface-300 rounded"
              checked={formData.cleanupEnabled}
              onChange={handleChange}
            />
            <label htmlFor="cleanupEnabled" className="ml-2 block text-sm text-surface-900">
              Cleanup Enabled
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Cleanup Interval *
            </label>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="number"
                name="cleanupIntervalValue"
                required
                min="1"
                className="input-field"
                placeholder="1"
                value={formData.cleanupIntervalValue}
                onChange={handleChange}
              />
              <select
                name="cleanupIntervalUnit"
                className="input-field"
                value={formData.cleanupIntervalUnit}
                onChange={handleChange}
              >
                <option value="MINUTES">Minutes</option>
                <option value="HOURS">Hours</option>
                <option value="DAYS">Days</option>
              </select>
            </div>
            <p className="mt-1 text-sm text-surface-500">
              How often to run the cleanup process
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-700 mb-2">
              Delete Files Older Than *
            </label>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="number"
                name="deleteAgeValue"
                required
                min="1"
                className="input-field"
                placeholder="30"
                value={formData.deleteAgeValue}
                onChange={handleChange}
              />
              <select
                name="deleteAgeUnit"
                className="input-field"
                value={formData.deleteAgeUnit}
                onChange={handleChange}
              >
                <option value="MINUTES">Minutes</option>
                <option value="HOURS">Hours</option>
                <option value="DAYS">Days</option>
                <option value="MONTHS">Months</option>
              </select>
            </div>
            <p className="mt-1 text-sm text-surface-500">
              Files older than this will be deleted
            </p>
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
              {deleteService ? 'Update' : 'Create'}
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
        <div className="modal-overlay z-[60]">
          <div className="modal-panel max-w-md p-6">
            <h3 className="text-lg font-semibold text-surface-900 mb-4">Copy Existing Delete Service</h3>
            <p className="text-sm text-surface-600 mb-4">Select a delete service to copy its settings:</p>
            
            <select
              value={selectedToCopy}
              onChange={(e) => setSelectedToCopy(e.target.value)}
              className="input-field mb-4"
            >
              <option value="">-- Select Delete Service --</option>
              {allDeleteServices.map((item) => (
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

export default DeleteServiceForm;
