import React, { useState } from 'react';
import { Clock, Activity, ChevronDown, ChevronRight } from 'lucide-react';
import { formatTimeAgo, formatCheckInterval, formatIntegrationTimer, formatDateTime, getStatusColor, getStatusText } from '../../utils/formatters';

const IntegrationTable = ({ integrations, integrationTimers, onToggle }) => {
  const [expandedItems, setExpandedItems] = useState(() => {
    const saved = localStorage.getItem('dashboard_integrations_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleExpand = (id) => {
    setExpandedItems(prev => {
      const newExpanded = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_integrations_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  return (
    <div className="space-y-3">
      {integrations.map((integration) => {
        const isExpanded = expandedItems[integration.id] !== false;
        const statusColor = getStatusColor(integration.isActive, integration.lastFileFound, integration.thresholdMinutes);
        
        return (
          <div key={integration.id} className="border border-gray-200 rounded-lg">
            <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50" onClick={() => toggleExpand(integration.id)}>
              <div className="flex items-center space-x-3 flex-1">
                {isExpanded ? (
                  <ChevronDown className="h-5 w-5 text-gray-500 flex-shrink-0" />
                ) : (
                  <ChevronRight className="h-5 w-5 text-gray-500 flex-shrink-0" />
                )}
                <div className="flex-1 min-w-0">
                  <h4 className="font-medium text-gray-900">{integration.name}</h4>
                  <p className="text-sm text-gray-500 truncate">{integration.path}</p>
                </div>
              </div>
              <div className="flex items-center space-x-3 ml-4">
                <div className="flex items-center">
                  <div className={`w-3 h-3 rounded-full mr-2 ${
                    statusColor === 'status-active' 
                      ? 'bg-green-500 animate-pulse' 
                      : 'bg-red-500'
                  }`}></div>
                  <span className={`text-sm ${statusColor}`}>
                    {getStatusText(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}
                  </span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer" onClick={(e) => e.stopPropagation()}>
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={integration.isActive}
                    onChange={() => onToggle(integration.id, integration.isActive)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                </label>
              </div>
            </div>

            {isExpanded && (
              <div className="px-4 pb-4 pt-0 border-t border-gray-100">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Check Interval</p>
                    <div className="flex items-center text-sm text-gray-900">
                      <Clock className="h-4 w-4 mr-1" />
                      {formatCheckInterval(integration)}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Next Run</p>
                    <div className="flex items-center text-sm text-gray-900">
                      <Activity className="h-4 w-4 mr-1 text-primary-500" />
                      <span className="font-mono text-xs">
                        {formatIntegrationTimer(integration, integrationTimers)}
                      </span>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Last File Found</p>
                    <p className="text-sm text-gray-900">{formatDateTime(integration.lastFileFound)}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default IntegrationTable;
