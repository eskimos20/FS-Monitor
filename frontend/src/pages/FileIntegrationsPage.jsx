import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import FileIntegrationsTab from '../components/dashboard/FileIntegrationsTab';
import { useIntegrations } from '../hooks/useIntegrations';
import { useMonitoringStatus } from '../hooks/useMonitoringStatus';
import { useAppSettings } from '../hooks/useAppSettings';

const FileIntegrationsPage = () => {
  const { refreshIntervalMs } = useAppSettings();
  const { integrations, loading, error, toggleIntegration } = useIntegrations(refreshIntervalMs);
  const { secondsUntilNextRun, integrationTimers } = useMonitoringStatus(60000, refreshIntervalMs);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="File Integrations" subtitle="Monitored directories and latest file activity" />
      <FileIntegrationsTab
        integrations={integrations}
        loading={loading}
        error={error}
        secondsUntilNextRun={secondsUntilNextRun}
        integrationTimers={integrationTimers}
        onToggle={toggleIntegration}
      />
    </div>
  );
};

export default FileIntegrationsPage;
