import React from 'react';
import { Server, CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import StatsCard from '../StatsCard';
import ServiceTable from './ServiceTable';
import CollapsibleCard from '../CollapsibleCard';
import EmptyState from '../ui/EmptyState';

const ServiceIntegrationsTab = ({ services, loading, error, onToggle }) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  const activeCount = services.filter(s => s.isActive).length;
  const inactiveCount = services.length - activeCount;

  return (
    <div className="space-y-6">
      <div className={`grid grid-cols-1 ${inactiveCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-5`}>
        <StatsCard icon={Server} label="Total Services" value={services.length}
          iconColor="text-primary-600" tint="bg-primary-50" />
        <StatsCard icon={CheckCircle2} label="Active" value={activeCount}
          iconColor="text-emerald-600" tint="bg-emerald-50" />
        {inactiveCount > 0 && (
          <StatsCard icon={XCircle} label="Inactive" value={inactiveCount}
            iconColor="text-surface-500" tint="bg-surface-100" />
        )}
      </div>

      <CollapsibleCard title="Service Integrations" icon={Server} defaultOpen={true} storageKey="dashboard_services">
        <div className="p-5">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm mb-4">
              {error}
            </div>
          )}

          {services.length === 0 ? (
            <EmptyState
              icon={Server}
              title="No services configured"
              description="Go to Settings to add your first service integration."
            />
          ) : (
            <ServiceTable services={services} onToggle={onToggle} />
          )}
        </div>
      </CollapsibleCard>
    </div>
  );
};

export default ServiceIntegrationsTab;
