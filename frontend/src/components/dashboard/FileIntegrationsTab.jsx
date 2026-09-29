import React from 'react';
import { FolderOpen, CheckCircle2, XCircle, Activity, Loader2 } from 'lucide-react';
import StatsCard from '../StatsCard';
import IntegrationTable from './IntegrationTable';
import CollapsibleCard from '../CollapsibleCard';
import EmptyState from '../ui/EmptyState';

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
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  const activeCount = integrations.filter(i => i.isActive && i.monitoringEnabled).length;
  const inactiveCount = integrations.length - activeCount;

  return (
    <div className="space-y-6">
      <div className={`grid grid-cols-1 ${inactiveCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-5`}>
        <StatsCard icon={FolderOpen} label="Total Integrations" value={integrations.length}
          iconColor="text-primary-600" tint="bg-primary-50" />
        <StatsCard icon={CheckCircle2} label="Active" value={activeCount}
          iconColor="text-emerald-600" tint="bg-emerald-50" />
        {inactiveCount > 0 && (
          <StatsCard icon={XCircle} label="Inactive" value={inactiveCount}
            iconColor="text-red-600" tint="bg-red-50" />
        )}
      </div>

      <CollapsibleCard title="File Integrations" icon={FolderOpen} defaultOpen={true} storageKey="dashboard_file_integrations">
        <div className="p-5">
          <div className="flex justify-end items-center mb-4">
            <div className="badge-neutral">
              <Activity className="h-3.5 w-3.5" />
              {secondsUntilNextRun === null ? 'Loading…'
                : secondsUntilNextRun === 0 ? 'Checking now…'
                : `Next check in ${secondsUntilNextRun}s`}
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm mb-4">
              {error}
            </div>
          )}

          {integrations.length === 0 ? (
            <EmptyState
              icon={FolderOpen}
              title="No integrations"
              description="Go to Settings to add your first file integration."
            />
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
