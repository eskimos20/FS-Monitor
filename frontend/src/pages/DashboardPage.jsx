import React, { useState } from 'react';
import { Cpu, FolderOpen, Server, FileSearch, HardDrive, Trash2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useIntegrations } from '../hooks/useIntegrations';
import { useServices } from '../hooks/useServices';
import { useSystemStats } from '../hooks/useSystemStats';
import { useMonitoringStatus } from '../hooks/useMonitoringStatus';
import { useLogConfigs } from '../hooks/useLogConfigs';
import { useDeleteServices } from '../hooks/useDeleteServices';
import { useAppSettings } from '../hooks/useAppSettings';
import TabButton from '../components/TabButton';
import PageHeader from '../components/ui/PageHeader';
import SystemTab from '../components/dashboard/SystemTab';
import FileIntegrationsTab from '../components/dashboard/FileIntegrationsTab';
import ServiceIntegrationsTab from '../components/dashboard/ServiceIntegrationsTab';
import LogControlTab from '../components/dashboard/LogControlTab';
import StorageTable from '../components/dashboard/StorageTable';
import DeleteServiceTab from '../components/dashboard/DeleteServiceTab';

const TABS = [
  { id: 'system', icon: Cpu, label: 'System' },
  { id: 'files', icon: FolderOpen, label: 'File Integrations' },
  { id: 'services', icon: Server, label: 'Services' },
  { id: 'storage', icon: HardDrive, label: 'Storage' },
  { id: 'delete', icon: Trash2, label: 'Delete Service' },
  { id: 'logs', icon: FileSearch, label: 'Log Control' },
];

const DashboardPage = () => {
  const [activeTab, setActiveTab] = useState('system');
  const { user } = useAuth();

  const { refreshIntervalMs } = useAppSettings();
  const { integrations, loading: integrationsLoading, error: integrationsError, toggleIntegration } = useIntegrations(60000);
  const { services, loading: servicesLoading, error: servicesError, toggleService } = useServices(60000);
  const { systemStats } = useSystemStats(refreshIntervalMs);
  const { secondsUntilNextRun, integrationTimers } = useMonitoringStatus(60000, refreshIntervalMs);
  const { logConfigs } = useLogConfigs(60000);
  const { deleteServices, loading: deleteServicesLoading, error: deleteServicesError, toggleDeleteService } = useDeleteServices(60000);

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Dashboard"
        subtitle={`Welcome${user?.username ? `, ${user.username}` : ''}`}
      />

      {/* Tabs */}
      <div className="border-b border-surface-200">
        <nav className="-mb-px flex gap-6 overflow-x-auto scroll-slim">
          {TABS.map((tab) => (
            <TabButton
              key={tab.id}
              active={activeTab === tab.id}
              onClick={() => setActiveTab(tab.id)}
              icon={tab.icon}
              label={tab.label}
            />
          ))}
        </nav>
      </div>

      {activeTab === 'system' && <SystemTab systemStats={systemStats} />}

      {activeTab === 'files' && (
        <FileIntegrationsTab
          integrations={integrations}
          loading={integrationsLoading}
          error={integrationsError}
          secondsUntilNextRun={secondsUntilNextRun}
          integrationTimers={integrationTimers}
          onToggle={toggleIntegration}
        />
      )}

      {activeTab === 'services' && (
        <ServiceIntegrationsTab
          services={services}
          loading={servicesLoading}
          error={servicesError}
          onToggle={toggleService}
        />
      )}

      {activeTab === 'storage' && <StorageTable />}

      {activeTab === 'delete' && (
        <DeleteServiceTab
          deleteServices={deleteServices}
          loading={deleteServicesLoading}
          error={deleteServicesError}
          onToggle={toggleDeleteService}
        />
      )}

      {activeTab === 'logs' && <LogControlTab logConfigs={logConfigs} />}
    </div>
  );
};

export default DashboardPage;
