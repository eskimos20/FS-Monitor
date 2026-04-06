import React from 'react';
import { Clock, Activity } from 'lucide-react';
import { formatTimeAgo, formatCheckInterval, formatIntegrationTimer, formatDateTime, getStatusColor, getStatusText } from '../../utils/formatters';

const IntegrationTable = ({ integrations, integrationTimers, onToggle }) => {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Integration
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Check Interval
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Next Run
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Last File Found
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              On/Off
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {integrations.map((integration) => (
            <tr key={integration.id} className="hover:bg-gray-50">
              <td className="px-6 py-4 whitespace-nowrap">
                <div>
                  <div className="text-sm font-medium text-gray-900">{integration.name}</div>
                  <div className="text-sm text-gray-500 truncate max-w-xs">{integration.path}</div>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <div className={`w-3 h-3 rounded-full mr-2 ${
                    getStatusColor(integration.isActive, integration.lastFileFound, integration.thresholdMinutes) === 'status-active' 
                      ? 'bg-green-500 animate-pulse' 
                      : 'bg-red-500'
                  }`}></div>
                  <span className={getStatusColor(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}>
                    {getStatusText(integration.isActive, integration.lastFileFound, integration.thresholdMinutes)}
                  </span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                <div className="flex items-center">
                  <Clock className="h-4 w-4 mr-1" />
                  {formatCheckInterval(integration)}
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                <div className="flex items-center">
                  <Activity className="h-4 w-4 mr-1 text-primary-500" />
                  <span className="font-mono text-xs">
                    {formatIntegrationTimer(integration, integrationTimers)}
                  </span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {formatDateTime(integration.lastFileFound)}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={integration.isActive}
                    onChange={() => onToggle(integration.id, integration.isActive)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                  <span className="ml-3 text-sm font-medium text-gray-700">
                    {integration.isActive ? 'Active' : 'Inactive'}
                  </span>
                </label>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default IntegrationTable;
