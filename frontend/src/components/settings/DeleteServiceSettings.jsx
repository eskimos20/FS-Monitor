import React, { useState } from 'react';
import { Plus, Trash } from 'lucide-react';
import DeleteServiceForm from './DeleteServiceForm';
import EmptyState from '../ui/EmptyState';
import SettingsItemRow from '../ui/SettingsItemRow';
import StatusBadge from '../ui/StatusBadge';

const UNIT_MAP = { MINUTES: 'min', HOURS: 'h', DAYS: 'd', WEEKS: 'w', MONTHS: 'mo' };

const formatInterval = (value, unit) => `${value} ${UNIT_MAP[unit] || (unit || '').toLowerCase()}`;

const DeleteServiceSettings = ({ deleteServices, onSave, onDelete }) => {
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const handleEdit = (deleteService) => {
    setEditingItem(deleteService);
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
          Add Delete Service
        </button>
      </div>

      {deleteServices.length === 0 ? (
        <EmptyState
          icon={Trash}
          title="No delete services configured"
          description="Add a delete service to automatically clean up old files."
        />
      ) : (
        <div className="space-y-2.5">
          {deleteServices.map((service) => (
            <SettingsItemRow
              key={service.id}
              title={service.name}
              subtitle={
                <>
                  <span className="font-mono">{service.path}</span>
                  {service.fileTypes ? ` · ${service.fileTypes}` : ''}
                  {` · every ${formatInterval(service.cleanupIntervalValue, service.cleanupIntervalUnit)}`}
                  {` · older than ${formatInterval(service.deleteAgeValue, service.deleteAgeUnit)}`}
                  {service.recursive ? ' · recursive' : ''}
                  {service.filesDeletedLastScan > 0 ? ` · ${service.filesDeletedLastScan} files deleted last scan` : ''}
                </>
              }
              meta={
                <StatusBadge
                  variant={service.cleanupEnabled ? 'success' : 'neutral'}
                  label={service.cleanupEnabled ? 'Enabled' : 'Disabled'}
                />
              }
              onEdit={() => handleEdit(service)}
              onDelete={() => onDelete(service.id)}
            />
          ))}
        </div>
      )}

      {showForm && (
        <DeleteServiceForm
          deleteService={editingItem}
          allDeleteServices={deleteServices}
          onSave={handleSave}
          onCancel={handleClose}
        />
      )}
    </div>
  );
};

export default DeleteServiceSettings;
