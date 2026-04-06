import React from 'react';
import { FolderOpen, CheckCircle, XCircle, Clock, Activity } from 'lucide-react';
import StatsCard from '../StatsCard';
import IntegrationTable from './IntegrationTable';
import CollapsibleCard from '../CollapsibleCard';

const FileIntegrationsTab = ({ 
  integrations, 
  loading, 
  error, 
  secondsUntilNextRun, 
  integrationTimers,
  onToggle 
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  const activeCount = integrations.filter(i => i.isActive && i.monitoringEnabled).length;
  const inactiveCount = integrations.filter(i => !i.isActive || !i.monitoringEnabled).length;

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className={`grid grid-cols-1 ${inactiveCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-6`}>
        <StatsCard
          icon={FolderOpen}
          label="Total Integrations"
          value={integrations.length}
          iconColor="text-primary-600"
        />
        <StatsCard
          icon={CheckCircle}
          label="Active"
          value={activeCount}
          iconColor="text-green-600"
        />
        {inactiveCount > 0 && (
          <StatsCard
            icon={XCircle}
            label="Inactive"
            value={inactiveCount}
            iconColor="text-red-600"
          />
        )}
      </div>

      {/* Integrations Table */}
      <CollapsibleCard title="File Integrations" icon={FolderOpen} defaultOpen={true} storageKey="dashboard_file_integrations">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <Activity className="h-4 w-4" />
              <span>Next check in {secondsUntilNextRun}s</span>
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md text-sm mb-4">
              {error}
            </div>
          )}

          {integrations.length === 0 ? (
            <div className="text-center py-12">
              <FolderOpen className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No integrations</h3>
              <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first integration.</p>
            </div>
          ) : (
            <IntegrationTable
              integrations={integrations}
              integrationTimers={integrationTimers}
              onToggle={onToggle}
            />
          )}
        </div>
      </CollapsibleCard>
    </div>
  );
};

export default FileIntegrationsTab;
