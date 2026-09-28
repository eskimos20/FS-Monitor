import React from 'react';
import { Trash2, Clock, Loader2 } from 'lucide-react';
import StatusBadge from '../ui/StatusBadge';
import EmptyState from '../ui/EmptyState';
import Toggle from '../ui/Toggle';
import DetailItem from '../ui/DetailItem';

const UNIT_MAP = { MINUTES: 'min', HOURS: 'h', DAYS: 'd', WEEKS: 'w', MONTHS: 'mo' };

const formatInterval = (value, unit) => `${value} ${UNIT_MAP[unit] || (unit || '').toLowerCase()}`;

const DeleteServiceTab = ({ deleteServices, loading, error, onToggle }) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3">
        <p className="text-sm text-red-700">Error loading delete services: {error}</p>
      </div>
    );
  }

  if (deleteServices.length === 0) {
    return (
      <div className="card">
        <EmptyState
          icon={Trash2}
          title="No delete services"
          description="Configure delete services in Settings to automatically clean up old files."
        />
      </div>
    );
  }

  return (
    <div className="space-y-2.5">
      {deleteServices.map((service) => (
        <div key={service.id} className="bg-white border border-surface-200 rounded-xl shadow-card overflow-hidden">
          <div className="flex items-center justify-between gap-4 px-4 py-3.5">
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-semibold text-surface-900 truncate">{service.name}</h4>
              <p className="text-xs text-surface-500 truncate mt-0.5 font-mono">{service.path}</p>
            </div>
            <div className="flex items-center gap-4 flex-shrink-0">
              <StatusBadge
                variant={service.cleanupEnabled ? 'success' : 'neutral'}
                label={service.cleanupEnabled ? 'Enabled' : 'Disabled'}
                pulse={!!service.cleanupEnabled}
              />
              <Toggle
                checked={service.cleanupEnabled}
                onChange={() => onToggle(service.id)}
              />
            </div>
          </div>

          <div className="px-4 pb-4 pt-3 border-t border-surface-100">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-6 gap-y-4">
              <DetailItem label="Cleanup Interval">
                <Clock className="h-3.5 w-3.5 mr-1.5 text-surface-400" />
                Every {formatInterval(service.cleanupIntervalValue, service.cleanupIntervalUnit)}
              </DetailItem>
              <DetailItem label="Delete Age">
                {formatInterval(service.deleteAgeValue, service.deleteAgeUnit)}
              </DetailItem>
              <DetailItem label="Scan Mode">
                {service.recursive ? 'Recursive' : 'Top level only'}
              </DetailItem>
              <DetailItem label="Deleted Last Scan">
                <span className="tnum">
                  {service.filesDeletedLastScan || 0} files / {service.foldersDeletedLastScan || 0} dirs
                </span>
              </DetailItem>
            </div>
            {service.fileTypes && (
              <p className="mt-3 text-xs text-surface-500">
                File types: <span className="font-mono">{service.fileTypes}</span>
              </p>
            )}
            {service.lastCleanup && (
              <p className="mt-2 text-xs text-surface-400">
                Last cleanup: {new Date(service.lastCleanup).toLocaleString()}
              </p>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default DeleteServiceTab;
