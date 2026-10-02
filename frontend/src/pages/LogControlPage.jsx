import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import LogControlTab from '../components/dashboard/LogControlTab';
import { useLogConfigs } from '../hooks/useLogConfigs';
import { useAppSettings } from '../hooks/useAppSettings';

const LogControlPage = () => {
  const { refreshIntervalMs } = useAppSettings();
  const { logConfigs, toggleLogConfig } = useLogConfigs(refreshIntervalMs);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="Log Control" subtitle="Log file monitors and keyword matches" />
      <LogControlTab logConfigs={logConfigs} onToggle={toggleLogConfig} />
    </div>
  );
};

export default LogControlPage;
