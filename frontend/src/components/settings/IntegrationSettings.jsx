import React, { useState } from 'react';
import { Plus, Edit, Trash2, Folder } from 'lucide-react';
import IntegrationForm from './IntegrationForm';

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
        <button
          onClick={() => setShowForm(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="h-4 w-4 mr-2" />
          Add
        </button>
      </div>

      {integrations.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <Folder className="h-12 w-12 mx-auto mb-2 text-gray-400" />
          <p>No integrations configured</p>
        </div>
      ) : (
        <div className="space-y-3">
          {integrations.map((integration) => (
            <div key={integration.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100">
              <div className="flex-1">
                <h3 className="font-medium text-gray-900">{integration.name}</h3>
                <p className="text-sm text-gray-500 truncate">{integration.path}</p>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleEdit(integration)}
                  className="p-2 text-gray-600 hover:text-primary-600"
                >
                  <Edit className="h-4 w-4" />
                </button>
                <button
                  onClick={() => onDelete(integration.id)}
                  className="p-2 text-gray-600 hover:text-red-600"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
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
