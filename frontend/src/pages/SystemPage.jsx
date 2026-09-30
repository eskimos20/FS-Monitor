import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import SystemTab from '../components/dashboard/SystemTab';
import { useSystemStats } from '../hooks/useSystemStats';
import { useAppSettings } from '../hooks/useAppSettings';

const SystemPage = () => {
  const { refreshIntervalMs } = useAppSettings();
  const { systemStats } = useSystemStats(refreshIntervalMs);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="System" subtitle="Live CPU, memory and process stats" />
      <SystemTab systemStats={systemStats} />
    </div>
  );
};

export default SystemPage;
