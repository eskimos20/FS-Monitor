import { useState } from 'react';
import { Plus, Edit2, Trash2, HardDrive } from 'lucide-react';
import StorageConfigForm from './StorageConfigForm';

const StorageConfigSettings = ({ storageConfigs, onSave, onDelete }) => {
  const [showForm, setShowForm] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);

  const handleAdd = () => {
    setEditingConfig(null);
    setShowForm(true);
  };

  const handleEdit = (config) => {
    setEditingConfig(config);
    setShowForm(true);
  };

  const handleSave = (data) => {
    onSave(data, editingConfig?.id);
    setShowForm(false);
    setEditingConfig(null);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingConfig(null);
  };

  const handleDelete = (id) => {
    if (window.confirm('Are you sure you want to delete this storage configuration?')) {
      onDelete(id);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <p className="text-sm text-gray-600">
          Monitor disk space usage in directories
        </p>
        <button onClick={handleAdd} className="btn-primary flex items-center space-x-2">
          <Plus className="h-4 w-4" />
          <span>Add</span>
        </button>
      </div>

      {storageConfigs.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <HardDrive className="h-12 w-12 mx-auto mb-2 text-gray-400" />
          <p>No storage configurations yet</p>
        </div>
      ) : (
        <div className="space-y-3">
          {storageConfigs.map((config) => (
            <div key={config.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100">
              <div className="flex-1">
                <div className="flex items-center space-x-2 mb-2">
                  <h4 className="font-medium text-gray-900">{config.name}</h4>
                  {!config.active && (
                    <span className="px-2 py-1 text-xs font-medium bg-gray-100 text-gray-600 rounded">
                      Inactive
                    </span>
                  )}
                </div>
                <p className="text-sm text-gray-600 mb-2">{config.path}</p>
                <div className="flex items-center space-x-4 text-xs text-gray-500">
                  <span>Recursive: {config.recursive ? 'Yes' : 'No'}</span>
                  <span>Check every: {config.checkIntervalMinutes} {(config.intervalUnit || 'MINUTES').toLowerCase()}</span>
                </div>
              </div>
              <div className="flex items-center space-x-2 ml-4">
                <button
                  onClick={() => handleEdit(config)}
                  className="p-2 text-gray-400 hover:text-blue-600 transition-colors"
                  title="Edit"
                >
                  <Edit2 className="h-4 w-4" />
                </button>
                <button
                  onClick={() => handleDelete(config.id)}
                  className="p-2 text-gray-400 hover:text-red-600 transition-colors"
                  title="Delete"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <StorageConfigForm
          storageConfig={editingConfig}
          allStorageConfigs={storageConfigs}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      )}
    </div>
  );
};

export default StorageConfigSettings;
