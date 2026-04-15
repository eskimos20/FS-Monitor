import React, { useState } from 'react';
import { Plus, Edit, Trash2, FileSearch } from 'lucide-react';
import LogConfigForm from './LogConfigForm';

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
        <button
          onClick={() => setShowForm(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="h-4 w-4 mr-2" />
          Add
        </button>
      </div>

      {logConfigs.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <FileSearch className="h-12 w-12 mx-auto mb-2 text-gray-400" />
          <p>No log configurations</p>
        </div>
      ) : (
        <div className="space-y-3">
          {logConfigs.map((logConfig) => (
            <div key={logConfig.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100">
              <div className="flex-1">
                <h3 className="font-medium text-gray-900">{logConfig.name}</h3>
                <p className="text-sm text-gray-500">
                  {logConfig.path} - {logConfig.fileTypes}
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  Keywords: {logConfig.keywords}
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleEdit(logConfig)}
                  className="p-2 text-gray-600 hover:text-primary-600"
                >
                  <Edit className="h-4 w-4" />
                </button>
                <button
                  onClick={() => onDelete(logConfig.id)}
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
