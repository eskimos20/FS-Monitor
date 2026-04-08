import React, { useState } from 'react';
import { Plus, Edit, Trash2, Power, FileText, AlertCircle } from 'lucide-react';
import { useLogConfigs } from '../../hooks/useLogConfigs';
import LogConfigForm from '../../components/settings/LogConfigForm';

const LogConfigsPage = () => {
  const { logConfigs, loading, error, createLogConfig, updateLogConfig, deleteLogConfig, toggleLogConfig } = useLogConfigs(60000);
  const [showForm, setShowForm] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const handleCreate = () => {
    setEditingConfig(null);
    setShowForm(true);
  };

  const handleEdit = (config) => {
    setEditingConfig(config);
    setShowForm(true);
  };

  const handleSave = async (data) => {
    try {
      if (editingConfig) {
        await updateLogConfig(editingConfig.id, data);
      } else {
        await createLogConfig(data);
      }
      setShowForm(false);
      setEditingConfig(null);
    } catch (err) {
      console.error('Error saving log config:', err);
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteLogConfig(id);
      setDeleteConfirm(null);
    } catch (err) {
      console.error('Error deleting log config:', err);
    }
  };

  const handleToggle = async (id) => {
    try {
      await toggleLogConfig(id);
    } catch (err) {
      console.error('Error toggling log config:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Log Configurations</h1>
          <p className="text-gray-600">Configure log file monitoring and keyword searches</p>
        </div>
        <button onClick={handleCreate} className="btn-primary">
          <Plus className="h-4 w-4 mr-2" />
          Add Log Configuration
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md flex items-center">
          <AlertCircle className="h-5 w-5 mr-2" />
          {error}
        </div>
      )}

      {/* Log Configs List */}
      {logConfigs.length === 0 ? (
        <div className="card">
          <div className="text-center py-12">
            <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No Log Configurations</h3>
            <p className="text-gray-500 mb-4">
              Get started by adding your first log configuration
            </p>
            <button onClick={handleCreate} className="btn-primary">
              <Plus className="h-4 w-4 mr-2" />
              Add Log Configuration
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6">
          {logConfigs.map((config) => (
            <div key={config.id} className="card">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3 mb-2">
                    <h3 className="text-lg font-semibold text-gray-900">{config.name}</h3>
                  </div>

                  <div className="space-y-2 text-sm text-gray-600">
                    <div>
                      <span className="font-medium">Path:</span> {config.path}
                    </div>
                    <div>
                      <span className="font-medium">File Types:</span> {config.fileTypes}
                    </div>
                    <div>
                      <span className="font-medium">Keywords:</span>{' '}
                      <span className="font-mono text-xs bg-gray-100 px-2 py-1 rounded">
                        {config.keywords}
                      </span>
                    </div>
                    {config.lastCheck && (
                      <div className="text-xs text-gray-500">
                        Last checked: {new Date(config.lastCheck).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>

                <div className="flex items-center space-x-2 ml-4">
                  <button
                    onClick={() => handleToggle(config.id)}
                    className={`p-2 rounded-md ${
                      config.active
                        ? 'text-green-600 hover:bg-green-50'
                        : 'text-gray-400 hover:bg-gray-50'
                    }`}
                    title="Toggle Status"
                  >
                    <Power className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => handleEdit(config)}
                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-md"
                    title="Edit"
                  >
                    <Edit className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => setDeleteConfirm(config.id)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded-md"
                    title="Delete"
                  >
                    <Trash2 className="h-5 w-5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Form Modal */}
      {showForm && (
        <LogConfigForm
          logConfig={editingConfig}
          onSave={handleSave}
          onCancel={() => {
            setShowForm(false);
            setEditingConfig(null);
          }}
        />
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Confirm Delete</h3>
            <p className="text-gray-600 mb-6">
              Are you sure you want to delete this log configuration? This action cannot be undone.
            </p>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => setDeleteConfirm(null)}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(deleteConfirm)}
                className="bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LogConfigsPage;
