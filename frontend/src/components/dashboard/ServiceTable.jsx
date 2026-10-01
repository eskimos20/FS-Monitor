import React, { useState } from 'react';
import { Calendar } from 'lucide-react';
import ExpandableRow from '../ui/ExpandableRow';
import Toggle from '../ui/Toggle';
import DetailItem from '../ui/DetailItem';
import StatusBadge from '../ui/StatusBadge';
import { formatSchedule } from '../../utils/formatters';

const statusVariant = (status) =>
  status === 'ONLINE' ? 'success' : status === 'OFFLINE' ? 'danger' : 'neutral';

const ServiceTable = ({ services, onToggle }) => {
  const [expandedItems, setExpandedItems] = useState(() => {
    const saved = localStorage.getItem('dashboard_services_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleExpand = (id) => {
    setExpandedItems(prev => {
      const next = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_services_expanded', JSON.stringify(next));
      return next;
    });
  };

  return (
    <div className="space-y-2.5">
      {services.map((service) => {
        const isExpanded = expandedItems[service.id] !== false;

        return (
          <ExpandableRow
            key={service.id}
            expanded={isExpanded}
            onToggle={() => toggleExpand(service.id)}
            title={service.name}
            subtitle={`${service.host}${service.port ? ':' + service.port : ''}`}
            aside={
              <>
                <StatusBadge
                  variant={statusVariant(service.status)}
                  label={service.status || 'UNKNOWN'}
                />
                <Toggle
                  checked={service.isActive}
                  onChange={() => onToggle(service.id, service.isActive)}
                />
              </>
            }
          >
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-6 gap-y-4">
              <DetailItem label="Type">
                <span className="badge-info">{service.type}</span>
              </DetailItem>
              <DetailItem label="Last Check">
                {service.lastCheckedAt ? new Date(service.lastCheckedAt).toLocaleString() : 'Never'}
              </DetailItem>
              <DetailItem label="Check Interval">
                {service.checkIntervalMinutes} minutes
              </DetailItem>
              <DetailItem label="Schedule">
                <Calendar className="h-3.5 w-3.5 mr-1.5 text-surface-400" />
                <span className={service.scheduleEnabled ? '' : 'text-surface-400'}>
                  {formatSchedule(service) || 'Always active'}
                </span>
              </DetailItem>
            </div>
          </ExpandableRow>
        );
      })}
    </div>
  );
};

export default ServiceTable;
