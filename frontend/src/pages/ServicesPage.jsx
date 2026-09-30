import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import ServiceIntegrationsTab from '../components/dashboard/ServiceIntegrationsTab';
import { useServices } from '../hooks/useServices';
import { useAppSettings } from '../hooks/useAppSettings';

const ServicesPage = () => {
  const { refreshIntervalMs } = useAppSettings();
  const { services, loading, error, toggleService } = useServices(refreshIntervalMs);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="Services" subtitle="Service availability checks and status" />
      <ServiceIntegrationsTab
        services={services}
        loading={loading}
        error={error}
        onToggle={toggleService}
      />
    </div>
  );
};

export default ServicesPage;
