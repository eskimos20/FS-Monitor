import { useState, useEffect } from 'react';
import { HardDrive, Folder, File, AlertCircle, ChevronDown, ChevronRight } from 'lucide-react';
import CollapsibleCard from '../CollapsibleCard';

const StorageTable = () => {
  const [storageData, setStorageData] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedItems, setExpandedItems] = useState(() => {
    const saved = localStorage.getItem('dashboard_storage_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleExpand = (id) => {
    setExpandedItems(prev => {
      const newExpanded = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_storage_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  const fetchStorageData = async () => {
    try {
      const response = await fetch('/api/storage-configs/dashboard', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setStorageData(data);
        setError('');
      } else {
        setError('Failed to fetch storage data');
      }
    } catch (err) {
      setError('Error fetching storage data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStorageData();
    const interval = setInterval(fetchStorageData, 30000); // Refresh every 30 seconds
    return () => clearInterval(interval);
  }, []);

  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatPercentage = (used, total) => {
    if (!total) return '0%';
    return ((used / total) * 100).toFixed(1) + '%';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-2 mb-4">
          <HardDrive className="h-5 w-5 text-gray-600" />
          <h2 className="text-lg font-semibold text-gray-800">Storage Monitoring</h2>
        </div>
        <p className="text-gray-500">Loading storage data...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-2 mb-4">
          <HardDrive className="h-5 w-5 text-gray-600" />
          <h2 className="text-lg font-semibold text-gray-800">Storage Monitoring</h2>
        </div>
        <div className="flex items-center space-x-2 text-red-600">
          <AlertCircle className="h-5 w-5" />
          <span>{error}</span>
        </div>
      </div>
    );
  }

  const storageConfigs = Object.values(storageData);
  
  // Get system-wide storage data (cache ID -1)
  const systemStorage = storageData['-1'];
  
  if (storageConfigs.length === 0 && !systemStorage) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-2 mb-4">
          <HardDrive className="h-5 w-5 text-gray-600" />
          <h2 className="text-lg font-semibold text-gray-800">Storage Monitoring</h2>
        </div>
        <p className="text-gray-500">No storage configurations found. Add one in Settings.</p>
      </div>
    );
  }

  return (
    <CollapsibleCard title="Storage Monitoring" icon={HardDrive} defaultOpen={true} storageKey="dashboard_storage">
      <div className="p-6 space-y-6">
        
        {/* System-wide Storage Summary */}
        {systemStorage && systemStorage.diskSpace && (
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-6 mb-6">
            <div className="flex items-center space-x-3 mb-4">
              <HardDrive className="h-6 w-6 text-blue-600" />
              <h3 className="text-lg font-semibold text-gray-800">System-wide Storage</h3>
              <span className="text-sm text-gray-500">(excluding /mnt)</span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-white bg-opacity-70 rounded-lg p-4">
                <p className="text-sm text-gray-600 mb-1">Total Storage</p>
                <p className="text-xl font-bold text-blue-600">
                  {formatBytes(systemStorage.diskSpace.totalBytes)}
                </p>
              </div>
              <div className="bg-white bg-opacity-70 rounded-lg p-4">
                <p className="text-sm text-gray-600 mb-1">Free Space</p>
                <p className="text-xl font-bold text-green-600">
                  {formatBytes(systemStorage.diskSpace.usableBytes)}
                </p>
              </div>
              <div className="bg-white bg-opacity-70 rounded-lg p-4">
                <p className="text-sm text-gray-600 mb-1">Used Space</p>
                <p className="text-xl font-bold text-orange-600">
                  {formatBytes(systemStorage.diskSpace.totalBytes - systemStorage.diskSpace.usableBytes)}
                </p>
              </div>
            </div>
            
            {/* Mount Points Details */}
            {systemStorage.info && systemStorage.info.length > 0 && (
              <div className="mt-4 pt-4 border-t border-blue-200">
                <p className="text-sm font-semibold text-gray-700 mb-2">Mount Points:</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {systemStorage.info.map((mount, index) => (
                    <div key={index} className="text-sm bg-white bg-opacity-50 rounded px-3 py-2">
                      <span className="font-medium text-gray-700">{mount.path}:</span>
                      <span className="ml-2 text-gray-600">{formatBytes(mount.totalSizeBytes)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
        
        {/* Separator between system-wide and individual configs */}
        {systemStorage && storageConfigs.filter(({ config }) => config.id !== -1).length > 0 && (
          <div className="border-t border-gray-200 pt-6">
            <h4 className="text-md font-semibold text-gray-700 mb-4">Individual Storage Configurations</h4>
          </div>
        )}
        
        {/* Individual Storage Configs */}
        {storageConfigs.filter(({ config }) => config.id !== -1).map(({ config, info, largestFiles, diskSpace }) => {
          const totalSize = info?.reduce((sum, item) => sum + item.totalSizeBytes, 0) || 0;
          const totalFiles = info?.reduce((sum, item) => sum + item.fileCount, 0) || 0;
          const totalDirs = info?.reduce((sum, item) => sum + item.directoryCount, 0) || 0;
          const isExpanded = expandedItems[config.id] !== false;

          return (
            <div key={config.id} className="border border-gray-200 rounded-lg">
              <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50" onClick={() => toggleExpand(config.id)}>
                <div className="flex items-center space-x-2">
                  {isExpanded ? (
                    <ChevronDown className="h-5 w-5 text-gray-500" />
                  ) : (
                    <ChevronRight className="h-5 w-5 text-gray-500" />
                  )}
                  <div>
                    <h3 className="font-semibold text-gray-800">{config.name}</h3>
                    <p className="text-sm text-gray-600">{config.path}</p>
                  </div>
                </div>
              </div>

              {isExpanded && (
                <div className="p-4 pt-0">

              {/* Directory Size Summary */}
              {diskSpace && diskSpace.totalBytes > 0 && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                  <div className="grid grid-cols-1 gap-4">
                    <div>
                      <p className="text-xs text-gray-600 mb-1">Total Directory Size</p>
                      <p className="text-lg font-semibold text-gray-900">{formatBytes(diskSpace.totalBytes)}</p>
                    </div>
                  </div>
                </div>
              )}

              {/* Summary Stats */}
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-4">
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <p className="text-xs text-gray-600 mb-1">Scanned Size</p>
                    <p className="text-lg font-semibold text-gray-900">{formatBytes(totalSize)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600 mb-1">Total Files</p>
                    <p className="text-lg font-semibold text-gray-900">{totalFiles.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600 mb-1">Directories</p>
                    <p className="text-lg font-semibold text-gray-900">{totalDirs.toLocaleString()}</p>
                  </div>
                </div>
              </div>

              {/* Largest Directories */}
              {info && info.length > 0 && (
                <div className="mb-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">Largest Directories (Top 20)</h4>
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Directory</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Files</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Subdirs</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {info.map((item) => (
                          <tr key={item.id} className="hover:bg-gray-50">
                            <td className="px-4 py-2 text-sm text-gray-900">
                              <div className="flex items-center space-x-2">
                                <Folder className="h-4 w-4 text-blue-500 flex-shrink-0" />
                                <span className="truncate max-w-md" title={item.path}>{item.path}</span>
                              </div>
                            </td>
                            <td className="px-4 py-2 text-sm font-medium text-gray-900">{formatBytes(item.totalSizeBytes)}</td>
                            <td className="px-4 py-2 text-sm text-gray-600">{item.fileCount.toLocaleString()}</td>
                            <td className="px-4 py-2 text-sm text-gray-600">{item.directoryCount.toLocaleString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Largest Files */}
              {largestFiles && largestFiles.length > 0 && (
                <div>
                  <h4 className="text-sm font-medium text-gray-700 mb-2">Largest Files (Top 10)</h4>
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">File</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {largestFiles.slice(0, 10).map((file) => (
                          <tr key={file.id} className="hover:bg-gray-50">
                            <td className="px-4 py-2 text-sm text-gray-900">
                              <div className="flex items-center space-x-2">
                                <File className="h-4 w-4 text-gray-500 flex-shrink-0" />
                                <span className="truncate max-w-md" title={file.filePath}>{file.filePath}</span>
                              </div>
                            </td>
                            <td className="px-4 py-2 text-sm font-medium text-gray-900">{formatBytes(file.sizeBytes)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </CollapsibleCard>
  );
};

export default StorageTable;
