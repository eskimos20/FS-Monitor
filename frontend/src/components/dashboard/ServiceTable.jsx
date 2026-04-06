import React, { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';

const ServiceTable = ({ services, onToggle }) => {
  const [expandedItems, setExpandedItems] = useState(() => {
    const saved = localStorage.getItem('dashboard_services_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleExpand = (id) => {
    setExpandedItems(prev => {
      const newExpanded = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_services_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  return (
    <div className="space-y-3">
      {services.map((service) => {
        const isExpanded = expandedItems[service.id] !== false;
        
        return (
          <div key={service.id} className="border border-gray-200 rounded-lg">
            <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50" onClick={() => toggleExpand(service.id)}>
              <div className="flex items-center space-x-3 flex-1">
                {isExpanded ? (
                  <ChevronDown className="h-5 w-5 text-gray-500 flex-shrink-0" />
                ) : (
                  <ChevronRight className="h-5 w-5 text-gray-500 flex-shrink-0" />
                )}
                <div className="flex-1 min-w-0">
                  <h4 className="font-medium text-gray-900">{service.name}</h4>
                  <p className="text-sm text-gray-500">{service.host}:{service.port}</p>
                </div>
              </div>
              <div className="flex items-center space-x-3 ml-4">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  service.status === 'ONLINE' 
                    ? 'bg-green-100 text-green-800'
                    : service.status === 'OFFLINE'
                    ? 'bg-red-100 text-red-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {service.status || 'UNKNOWN'}
                </span>
                <label className="relative inline-flex items-center cursor-pointer" onClick={(e) => e.stopPropagation()}>
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={service.isActive}
                    onChange={() => onToggle(service.id, service.isActive)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                </label>
              </div>
            </div>

            {isExpanded && (
              <div className="px-4 pb-4 pt-0 border-t border-gray-100">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Type</p>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {service.type}
                    </span>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Last Check</p>
                    <p className="text-sm text-gray-900">
                      {service.lastCheckedAt ? new Date(service.lastCheckedAt).toLocaleString() : 'Never'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Check Interval</p>
                    <p className="text-sm text-gray-900">{service.checkIntervalMinutes} minutes</p>
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

export default ServiceTable;
