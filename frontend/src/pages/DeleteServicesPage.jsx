import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import DeleteServiceTab from '../components/dashboard/DeleteServiceTab';
import { useDeleteServices } from '../hooks/useDeleteServices';
import { useAppSettings } from '../hooks/useAppSettings';

const DeleteServicesPage = () => {
  const { refreshIntervalMs } = useAppSettings();
  const { deleteServices, loading, error, toggleDeleteService } = useDeleteServices(refreshIntervalMs);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="Delete Services" subtitle="Automatic cleanup of old files" />
      <DeleteServiceTab
        deleteServices={deleteServices}
        loading={loading}
        error={error}
        onToggle={toggleDeleteService}
      />
    </div>
  );
};

export default DeleteServicesPage;
