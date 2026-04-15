import React from 'react';
import { Trash, Clock } from 'lucide-react';

const DeleteServiceTab = ({ deleteServices, loading, error, onToggle }) => {
  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-800">Error loading delete services: {error}</p>
      </div>
    );
  }

  const formatInterval = (value, unit) => {
    const unitMap = {
      'MINUTES': 'min',
      'HOURS': 'h',
      'DAYS': 'd',
      'MONTHS': 'mo'
    };
    return `${value} ${unitMap[unit] || unit.toLowerCase()}`;
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'Never';
    return new Date(dateTime).toLocaleString();
  };

  return (
    <div className="space-y-4">
      {deleteServices.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <Trash className="h-16 w-16 mx-auto mb-4 text-gray-400" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No Delete Services</h3>
          <p className="text-gray-600">
            Configure delete services in Settings to automatically clean up old files
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {deleteServices.map((service) => (
            <div key={service.id} className="border border-gray-200 rounded-lg">
              <div className="flex items-center justify-between p-4">
                <div className="flex-1 min-w-0">
                  <h4 className="font-medium text-gray-900">{service.name}</h4>
                  <p className="text-sm text-gray-500 truncate">{service.path}</p>
                  {service.fileTypes && (
                    <p className="text-xs text-gray-400 mt-1">
                      File types: {service.fileTypes}
                    </p>
                  )}
                </div>
                <div className="flex items-center space-x-3 ml-4">
                  <div className="flex items-center">
                    <div className={`w-3 h-3 rounded-full mr-2 ${
                      service.cleanupEnabled 
                        ? 'bg-green-500 animate-pulse' 
                        : 'bg-gray-400'
                    }`}></div>
                    <span className={`text-sm ${
                      service.cleanupEnabled 
                        ? 'text-green-600' 
                        : 'text-gray-500'
                    }`}>
                      {service.cleanupEnabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={service.cleanupEnabled}
                      onChange={() => onToggle(service.id)}
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                  </label>
                </div>
              </div>

              <div className="px-4 pb-4 border-t border-gray-100">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Cleanup Interval</p>
                    <div className="flex items-center text-sm text-gray-900">
                      <Clock className="h-4 w-4 mr-1" />
                      Every {formatInterval(service.cleanupIntervalValue, service.cleanupIntervalUnit)}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Delete Age</p>
                    <p className="text-sm text-gray-900">
                      {formatInterval(service.deleteAgeValue, service.deleteAgeUnit)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Scan Mode</p>
                    <p className="text-sm text-gray-900">
                      {service.recursive ? 'Recursive' : 'Non-recursive'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Files/Folders deleted last scan</p>
                    <p className="text-sm text-gray-900">
                      {service.filesDeletedLastScan || 0}/{service.foldersDeletedLastScan || 0}
                    </p>
                  </div>
                </div>
                {service.lastCleanup && (
                  <div className="mt-4 pt-4 border-t border-gray-100">
                    <p className="text-xs text-gray-500">
                      Last cleanup: <span className="font-medium text-gray-700">{formatDateTime(service.lastCleanup)}</span>
                    </p>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default DeleteServiceTab;
