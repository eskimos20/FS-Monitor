import React, { useState } from 'react';
import { Cpu, FolderOpen, Server, FileSearch } from 'lucide-react';
import { useIntegrations } from '../hooks/useIntegrations';
import { useServices } from '../hooks/useServices';
import { useSystemStats } from '../hooks/useSystemStats';
import { useMonitoringStatus } from '../hooks/useMonitoringStatus';
import { useLogConfigs } from '../hooks/useLogConfigs';
import TabButton from '../components/TabButton';
import SystemTab from '../components/dashboard/SystemTab';
import FileIntegrationsTab from '../components/dashboard/FileIntegrationsTab';
import ServiceIntegrationsTab from '../components/dashboard/ServiceIntegrationsTab';
import LogControlTab from '../components/dashboard/LogControlTab';

const DashboardPage = () => {
  const [activeTab, setActiveTab] = useState('system');
  
  const { integrations, loading: integrationsLoading, error: integrationsError, toggleIntegration } = useIntegrations();
  const { services, loading: servicesLoading, error: servicesError, toggleService } = useServices();
  const { systemStats } = useSystemStats(1000);
  const { secondsUntilNextRun, integrationTimers } = useMonitoringStatus();
  const { logConfigs } = useLogConfigs();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600">Monitor your file integrations and system resources</p>
      </div>

      {/* Tabs Navigation */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <TabButton
            active={activeTab === 'system'}
            onClick={() => setActiveTab('system')}
            icon={Cpu}
            label="CPU & Memory"
          />
          <TabButton
            active={activeTab === 'files'}
            onClick={() => setActiveTab('files')}
            icon={FolderOpen}
            label="File Integrations"
          />
          <TabButton
            active={activeTab === 'services'}
            onClick={() => setActiveTab('services')}
            icon={Server}
            label="Service Integrations"
          />
          <TabButton
            active={activeTab === 'logs'}
            onClick={() => setActiveTab('logs')}
            icon={FileSearch}
            label="Log Control"
          />
        </nav>
      </div>

      {/* Tab Content */}
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
      
      {activeTab === 'logs' && (
        <LogControlTab
          logConfigs={logConfigs}
        />
      )}
    </div>
  );
};

export default DashboardPage;
