import { useState } from 'react';
import { Plus, HardDrive } from 'lucide-react';
import StorageConfigForm from './StorageConfigForm';
import EmptyState from '../ui/EmptyState';
import SettingsItemRow from '../ui/SettingsItemRow';
import StatusBadge from '../ui/StatusBadge';

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
      <div className="flex justify-between items-center gap-4">
        <p className="text-sm text-surface-500">
          Monitor disk space usage in directories
        </p>
        <button onClick={handleAdd} className="btn-primary flex-shrink-0">
          <Plus className="h-4 w-4 mr-1.5" />
          Add Storage Config
        </button>
      </div>

      {storageConfigs.length === 0 ? (
        <EmptyState
          icon={HardDrive}
          title="No storage configurations"
          description="Add a storage config to track disk usage for a directory."
        />
      ) : (
        <div className="space-y-2.5">
          {storageConfigs.map((config) => (
            <SettingsItemRow
              key={config.id}
              title={config.name}
              subtitle={
                <>
                  <span className="font-mono">{config.path}</span>
                  {` · ${config.recursive ? 'Recursive' : 'Top level'} · every ${config.checkIntervalMinutes} ${(config.intervalUnit || 'MINUTES').toLowerCase()}`}
                </>
              }
              meta={
                <StatusBadge
                  variant={config.active ? 'success' : 'neutral'}
                  label={config.active ? 'Active' : 'Inactive'}
                />
              }
              onEdit={() => handleEdit(config)}
              onDelete={() => handleDelete(config.id)}
            />
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
