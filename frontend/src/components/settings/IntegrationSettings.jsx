import React, { useState } from 'react';
import { Plus, Folder } from 'lucide-react';
import IntegrationForm from './IntegrationForm';
import EmptyState from '../ui/EmptyState';
import SettingsItemRow from '../ui/SettingsItemRow';
import StatusBadge from '../ui/StatusBadge';

const IntegrationSettings = ({ integrations, fileTypes, onSave, onDelete }) => {
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const handleEdit = (integration) => {
    setEditingItem(integration);
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
          Add Integration
        </button>
      </div>

      {integrations.length === 0 ? (
        <EmptyState
          icon={Folder}
          title="No integrations configured"
          description="Add an integration to monitor a directory for new files."
        />
      ) : (
        <div className="space-y-2.5">
          {integrations.map((integration) => (
            <SettingsItemRow
              key={integration.id}
              title={integration.name}
              subtitle={<span className="font-mono">{integration.path}</span>}
              meta={
                <StatusBadge
                  variant={integration.monitoringEnabled ? 'success' : 'neutral'}
                  label={integration.monitoringEnabled ? 'Enabled' : 'Disabled'}
                />
              }
              onEdit={() => handleEdit(integration)}
              onDelete={() => onDelete(integration.id)}
            />
          ))}
        </div>
      )}

      {showForm && (
        <IntegrationForm
          integration={editingItem}
          allIntegrations={integrations}
          fileTypes={fileTypes}
          onSave={handleSave}
          onClose={handleClose}
        />
      )}
    </div>
  );
};

export default IntegrationSettings;
