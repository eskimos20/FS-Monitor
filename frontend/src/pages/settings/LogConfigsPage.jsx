import React, { useState } from 'react';
import { Plus, FileText, AlertCircle, Loader2, Pencil, Trash2 } from 'lucide-react';
import { useLogConfigs } from '../../hooks/useLogConfigs';
import LogConfigForm from '../../components/settings/LogConfigForm';
import PageHeader from '../../components/ui/PageHeader';
import EmptyState from '../../components/ui/EmptyState';
import IconButton from '../../components/ui/IconButton';
import Toggle from '../../components/ui/Toggle';
import DetailItem from '../../components/ui/DetailItem';

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
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Log Configurations"
        subtitle="Configure log file monitoring and keyword searches"
        actions={
          <button onClick={handleCreate} className="btn-primary">
            <Plus className="h-4 w-4 mr-1.5" />
            Add Log Configuration
          </button>
        }
      />

      {error && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          {error}
        </div>
      )}

      {logConfigs.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={FileText}
            title="No log configurations"
            description="Get started by adding your first log configuration."
            action={
              <button onClick={handleCreate} className="btn-primary">
                <Plus className="h-4 w-4 mr-1.5" />
                Add Log Configuration
              </button>
            }
          />
        </div>
      ) : (
        <div className="space-y-3">
          {logConfigs.map((config) => (
            <div key={config.id} className="card card-hover !p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <h3 className="section-title mb-3">{config.name}</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6 gap-y-3">
                    <DetailItem label="Path">
                      <span className="font-mono text-xs break-all">{config.path}</span>
                    </DetailItem>
                    <DetailItem label="File Types">
                      <span className="font-mono text-xs">{config.fileTypes}</span>
                    </DetailItem>
                    <DetailItem label="Keywords">
                      <span className="font-mono text-xs bg-surface-100 px-1.5 py-0.5 rounded break-all">
                        {config.keywords}
                      </span>
                    </DetailItem>
                  </div>
                  {config.lastCheck && (
                    <p className="mt-3 text-xs text-surface-400">
                      Last checked: {new Date(config.lastCheck).toLocaleString()}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-2 flex-shrink-0">
                  <Toggle checked={config.active} onChange={() => handleToggle(config.id)} />
                  <IconButton icon={Pencil} onClick={() => handleEdit(config)} title="Edit" variant="primary" />
                  <IconButton icon={Trash2} onClick={() => setDeleteConfirm(config.id)} title="Delete" variant="danger" />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

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

      {deleteConfirm && (
        <div className="modal-overlay">
          <div className="modal-panel max-w-md p-6">
            <h3 className="modal-title mb-3">Confirm Delete</h3>
            <p className="text-sm text-surface-600 mb-6">
              Are you sure you want to delete this log configuration? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <button onClick={() => setDeleteConfirm(null)} className="btn-secondary">
                Cancel
              </button>
              <button onClick={() => handleDelete(deleteConfirm)} className="btn-danger">
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
