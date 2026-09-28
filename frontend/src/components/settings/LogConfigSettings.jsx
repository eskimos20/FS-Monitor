import React, { useState } from 'react';
import { Plus, FileSearch } from 'lucide-react';
import LogConfigForm from './LogConfigForm';
import EmptyState from '../ui/EmptyState';
import SettingsItemRow from '../ui/SettingsItemRow';
import StatusBadge from '../ui/StatusBadge';

const LogConfigSettings = ({ logConfigs, onSave, onDelete }) => {
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const handleEdit = (logConfig) => {
    setEditingItem(logConfig);
    setShowForm(true);
  };

  const handleClose = () => {
    setShowForm(false);
    setEditingItem(null);
  };

  const handleSave = async (data) => {
    await onSave(data, editingItem?.id);
    handleClose();
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button onClick={() => setShowForm(true)} className="btn-primary">
          <Plus className="h-4 w-4 mr-1.5" />
          Add Log Monitor
        </button>
      </div>

      {logConfigs.length === 0 ? (
        <EmptyState
          icon={FileSearch}
          title="No log monitors configured"
          description="Add a log monitor to search files for keywords."
        />
      ) : (
        <div className="space-y-2.5">
          {logConfigs.map((logConfig) => (
            <SettingsItemRow
              key={logConfig.id}
              title={logConfig.name}
              subtitle={
                <>
                  <span className="font-mono">{logConfig.path}</span>
                  {logConfig.fileTypes ? ` · ${logConfig.fileTypes}` : ''}
                  {logConfig.keywords ? ` · keywords: ${logConfig.keywords}` : ''}
                </>
              }
              meta={
                <StatusBadge
                  variant={logConfig.active ? 'success' : 'neutral'}
                  label={logConfig.active ? 'Active' : 'Inactive'}
                />
              }
              onEdit={() => handleEdit(logConfig)}
              onDelete={() => onDelete(logConfig.id)}
            />
          ))}
        </div>
      )}

      {showForm && (
        <LogConfigForm
          logConfig={editingItem}
          allLogConfigs={logConfigs}
          onSave={handleSave}
          onCancel={handleClose}
        />
      )}
    </div>
  );
};

export default LogConfigSettings;
