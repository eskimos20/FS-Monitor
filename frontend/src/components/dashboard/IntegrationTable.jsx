import React, { useState } from 'react';
import { Clock, Activity, Calendar } from 'lucide-react';
import { formatCheckInterval, formatIntegrationTimer, formatDateTime, formatSchedule, isIntegrationHealthy, getStatusText } from '../../utils/formatters';
import ExpandableRow from '../ui/ExpandableRow';
import Toggle from '../ui/Toggle';
import DetailItem from '../ui/DetailItem';

const IntegrationTable = ({ integrations, integrationTimers, onToggle }) => {
  const [expandedItems, setExpandedItems] = useState(() => {
    const saved = localStorage.getItem('dashboard_integrations_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleExpand = (id) => {
    setExpandedItems(prev => {
      const next = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_integrations_expanded', JSON.stringify(next));
      return next;
    });
  };

  return (
    <div className="space-y-2.5">
      {integrations.map((integration) => {
        const isExpanded = expandedItems[integration.id] !== false;
        const healthy = isIntegrationHealthy(
          integration.isActive, integration.lastFileFound, integration.thresholdMinutes);

        return (
          <ExpandableRow
            key={integration.id}
            expanded={isExpanded}
            onToggle={() => toggleExpand(integration.id)}
            title={integration.name}
            subtitle={integration.path}
            aside={
              <>
                <div className="flex items-center gap-2">
                  <span className={`status-dot ${healthy ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`} />
                  <span className={`text-xs font-medium ${healthy ? 'text-emerald-600' : 'text-red-600'}`}>
                    {getStatusText(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}
                  </span>
                </div>
                <Toggle
                  checked={integration.isActive}
                  onChange={() => onToggle(integration.id, integration.isActive)}
                />
              </>
            }
          >
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-6 gap-y-4">
              <DetailItem label="Check Interval">
                <Clock className="h-3.5 w-3.5 mr-1.5 text-surface-400" />
                {formatCheckInterval(integration)}
              </DetailItem>
              <DetailItem label="Next Run">
                <Activity className="h-3.5 w-3.5 mr-1.5 text-primary-500" />
                <span className="font-mono text-xs tnum">
                  {formatIntegrationTimer(integration, integrationTimers)}
                </span>
              </DetailItem>
              <DetailItem label="Last File Found">
                {formatDateTime(integration.lastFileFound)}
              </DetailItem>
              <DetailItem label="Schedule">
                <Calendar className="h-3.5 w-3.5 mr-1.5 text-surface-400" />
                <span className={integration.scheduleEnabled ? '' : 'text-surface-400'}>
                  {formatSchedule(integration) || 'Always active'}
                </span>
              </DetailItem>
            </div>
          </ExpandableRow>
        );
      })}
    </div>
  );
};

export default IntegrationTable;
