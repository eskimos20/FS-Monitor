import React, { useState } from 'react';
import { Plus, Server } from 'lucide-react';
import ServiceForm from './ServiceForm';
import EmptyState from '../ui/EmptyState';
import SettingsItemRow from '../ui/SettingsItemRow';
import StatusBadge from '../ui/StatusBadge';

const ServiceSettings = ({ services, onSave, onDelete }) => {
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const handleEdit = (service) => {
    setEditingItem(service);
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
          Add Service
        </button>
      </div>

      {services.length === 0 ? (
        <EmptyState
          icon={Server}
          title="No services configured"
          description="Add a service to monitor its availability."
        />
      ) : (
        <div className="space-y-2.5">
          {services.map((service) => (
            <SettingsItemRow
              key={service.id}
              title={service.name}
              subtitle={`${service.type} · ${service.host}${service.port ? ':' + service.port : ''}`}
              meta={
                <StatusBadge
                  variant={service.isActive ? 'success' : 'neutral'}
                  label={service.isActive ? 'Enabled' : 'Disabled'}
                />
              }
              onEdit={() => handleEdit(service)}
              onDelete={() => onDelete(service.id)}
            />
          ))}
        </div>
      )}

      {showForm && (
        <ServiceForm
          service={editingItem}
          allServices={services}
          onSave={handleSave}
          onClose={handleClose}
        />
      )}
    </div>
  );
};

export default ServiceSettings;
