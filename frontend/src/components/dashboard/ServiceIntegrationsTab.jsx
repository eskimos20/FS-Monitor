import React from 'react';
import { Server, CheckCircle, XCircle } from 'lucide-react';
import StatsCard from '../StatsCard';
import ServiceTable from './ServiceTable';

const ServiceIntegrationsTab = ({ services, loading, error, onToggle }) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  const activeCount = services.filter(s => s.isActive).length;
  const inactiveCount = services.filter(s => !s.isActive).length;

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className={`grid grid-cols-1 ${inactiveCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-6`}>
        <StatsCard
          icon={Server}
          label="Total Services"
          value={services.length}
          iconColor="text-blue-600"
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
            iconColor="text-gray-600"
          />
        )}
      </div>

      {/* Services Table */}
      <div className="card">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Service Integrations</h2>
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Server className="h-4 w-4" />
            <span>Service Monitoring</span>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md text-sm mb-4">
            {error}
          </div>
        )}

        {services.length === 0 ? (
          <div className="text-center py-12">
            <Server className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No services configured</h3>
            <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first service integration.</p>
          </div>
        ) : (
          <ServiceTable services={services} onToggle={onToggle} />
        )}
      </div>
    </div>
  );
};

export default ServiceIntegrationsTab;
