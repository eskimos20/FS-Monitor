import { useState, useCallback } from 'react';
import { HardDrive, Folder, File, AlertCircle, ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import CollapsibleCard from '../CollapsibleCard';
import EmptyState from '../ui/EmptyState';
import api from '../../api/axios';
import { usePolling } from '../../hooks/usePolling';
import { sortByName } from '../../utils/formatters';

const formatBytes = (bytes) => {
  if (!bytes || bytes <= 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const usageColor = (usedPct) =>
  usedPct >= 90 ? 'bg-red-500' : usedPct >= 75 ? 'bg-amber-500' : 'bg-emerald-500';

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

  const fetchStorageData = useCallback(async () => {
    try {
      const response = await api.get('/storage-configs/dashboard');
      setStorageData(response.data);
      setError('');
    } catch (err) {
      setError('Error fetching storage data');
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchStorageData, 30000);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <div className="flex items-center gap-2 text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      </div>
    );
  }

  const storageConfigs = sortByName(Object.values(storageData), entry => entry.config?.name);

  // Get system-wide storage data (cache ID -1)
  const systemStorage = storageData['-1'];

  if (storageConfigs.length === 0 && !systemStorage) {
    return (
      <div className="card">
        <EmptyState
          icon={HardDrive}
          title="No storage configurations"
          description="Add a storage configuration in Settings to monitor disk usage."
        />
      </div>
    );
  }

  return (
    <CollapsibleCard title="Storage Monitoring" icon={HardDrive} defaultOpen={true} storageKey="dashboard_storage">
      <div className="p-6 space-y-6">
        
        {/* System-wide Storage Summary */}
        {systemStorage && systemStorage.diskSpace && (() => {
          const total = systemStorage.diskSpace.totalBytes || 0;
          const free = systemStorage.diskSpace.usableBytes || 0;
          const used = Math.max(total - free, 0);
          const usedPct = total > 0 ? (used / total) * 100 : 0;
          return (
          <div className="bg-gradient-to-r from-surface-50 to-primary-50/60 border border-surface-200 rounded-xl p-6 mb-6">
            <div className="flex items-center space-x-3 mb-4">
              <div className="flex items-center justify-center h-9 w-9 rounded-lg bg-primary-100">
                <HardDrive className="h-5 w-5 text-primary-600" />
              </div>
              <h3 className="section-title">System-wide Storage</h3>
              <span className="text-sm text-surface-500">(local filesystems)</span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-white/80 rounded-lg p-4 border border-white">
                <p className="text-xs font-medium uppercase tracking-wide text-surface-500 mb-1">Total Storage</p>
                <p className="text-xl font-semibold text-surface-900 tnum">
                  {formatBytes(total)}
                </p>
              </div>
              <div className="bg-white/80 rounded-lg p-4 border border-white">
                <p className="text-xs font-medium uppercase tracking-wide text-surface-500 mb-1">Free Space</p>
                <p className="text-xl font-semibold text-emerald-600 tnum">
                  {formatBytes(free)}
                </p>
              </div>
              <div className="bg-white/80 rounded-lg p-4 border border-white">
                <p className="text-xs font-medium uppercase tracking-wide text-surface-500 mb-1">Used Space</p>
                <p className="text-xl font-semibold text-amber-600 tnum">
                  {formatBytes(used)}
                </p>
              </div>
            </div>

            {/* Usage bar */}
            <div className="mt-4">
              <div className="flex justify-between text-xs text-surface-500 mb-1.5">
                <span>Used</span>
                <span className="font-medium tnum">{usedPct.toFixed(1)}%</span>
              </div>
              <div className="h-2 w-full rounded-full bg-white/80 border border-white overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${usageColor(usedPct)}`}
                  style={{ width: `${Math.min(usedPct, 100)}%` }}
                />
              </div>
            </div>

            {/* Mount Points Details */}
            {systemStorage.info && systemStorage.info.length > 0 && (
              <div className="mt-4 pt-4 border-t border-surface-200">
                <p className="text-xs font-semibold uppercase tracking-wide text-surface-500 mb-2">Mount Points</p>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                  {systemStorage.info.map((mount, index) => {
                    const mountTotal = mount.totalSizeBytes || 0;
                    const mountFree = mount.freeSpaceBytes || 0;
                    const mountPct = mountTotal > 0 ? ((mountTotal - mountFree) / mountTotal) * 100 : 0;
                    return (
                      <div key={index} className="text-sm bg-white/70 rounded-lg px-3 py-2 border border-white">
                        <div className="flex justify-between items-baseline">
                          <span className="font-medium text-surface-700 truncate" title={mount.path}>{mount.path}</span>
                          <span className="ml-2 text-surface-500 tnum whitespace-nowrap">{formatBytes(mountTotal)}</span>
                        </div>
                        <div className="mt-1.5 h-1 w-full rounded-full bg-surface-200 overflow-hidden">
                          <div
                            className={`h-full rounded-full ${usageColor(mountPct)}`}
                            style={{ width: `${Math.min(mountPct, 100)}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          );
        })()}
        
        {/* Separator between system-wide and individual configs */}
        {systemStorage && storageConfigs.filter(({ config }) => config.id !== -1).length > 0 && (
          <div className="border-t border-surface-200 pt-6">
            <h4 className="text-md font-semibold text-surface-700 mb-4">Individual Storage Configurations</h4>
          </div>
        )}
        
        {/* Individual Storage Configs */}
        {storageConfigs.filter(({ config }) => config.id !== -1).map(({ config, info, largestFiles, diskSpace }) => {
          // For Individual Storage, use the first (and only) item which contains total stats
          const totalSize = info && info.length > 0 ? info[0].totalSizeBytes : (diskSpace?.totalBytes || 0);
          const totalFiles = info && info.length > 0 ? info[0].fileCount : 0;
          const totalDirs = info && info.length > 0 ? info[0].directoryCount : 0;
          const isExpanded = expandedItems[config.id] !== false;

          return (
            <div key={config.id} className="border border-surface-200 rounded-lg">
              <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-surface-50" onClick={() => toggleExpand(config.id)}>
                <div className="flex items-center space-x-2">
                  {isExpanded ? (
                    <ChevronDown className="h-5 w-5 text-surface-500" />
                  ) : (
                    <ChevronRight className="h-5 w-5 text-surface-500" />
                  )}
                  <div>
                    <h3 className="font-semibold text-surface-800">{config.name}</h3>
                    <p className="text-sm text-surface-600">{config.path}</p>
                  </div>
                </div>
              </div>

              {isExpanded && (
                <div className="p-4 pt-0">

              {/* Directory Size Summary */}
              {diskSpace && diskSpace.totalBytes > 0 && (
                <div className="bg-primary-50 border border-primary-200 rounded-lg p-4 mb-4">
                  <div className="grid grid-cols-1 gap-4">
                    <div>
                      <p className="text-xs text-surface-600 mb-1">Total Directory Size</p>
                      <p className="text-lg font-semibold text-surface-900">{formatBytes(diskSpace.totalBytes)}</p>
                    </div>
                  </div>
                </div>
              )}

              {/* Summary Stats */}
              <div className="bg-surface-50 border border-surface-200 rounded-lg p-4 mb-4">
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <p className="text-xs text-surface-600 mb-1">Scanned Size</p>
                    <p className="text-lg font-semibold text-surface-900">{formatBytes(totalSize)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-surface-600 mb-1">Total Files</p>
                    <p className="text-lg font-semibold text-surface-900">{totalFiles.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-surface-600 mb-1">Directories</p>
                    <p className="text-lg font-semibold text-surface-900">{totalDirs.toLocaleString()}</p>
                  </div>
                </div>
              </div>

              {/* Largest Directories */}
              {info && info.length > 0 && (
                <div className="mb-4">
                  <h4 className="text-sm font-medium text-surface-700 mb-2">Largest Directories (Top 20)</h4>
                  <div className="overflow-x-auto scroll-slim border border-surface-200 rounded-lg">
                    <table className="table-shell">
                      <thead className="table-head">
                        <tr>
                          <th className="table-th">Directory</th>
                          <th className="table-th">Size</th>
                          <th className="table-th">Files</th>
                          <th className="table-th">Subdirs</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-surface-100">
                        {info.map((item) => (
                          <tr key={item.path} className="hover:bg-surface-50 transition-colors">
                            <td className="table-td">
                              <div className="flex items-center gap-2">
                                <Folder className="h-4 w-4 text-primary-500 flex-shrink-0" />
                                <span className="truncate max-w-md font-mono text-xs" title={item.path}>{item.path}</span>
                              </div>
                            </td>
                            <td className="table-td font-medium text-surface-900 tnum">{formatBytes(item.totalSizeBytes)}</td>
                            <td className="table-td text-surface-600 tnum">{item.fileCount.toLocaleString()}</td>
                            <td className="table-td text-surface-600 tnum">{item.directoryCount.toLocaleString()}</td>
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
                  <h4 className="text-sm font-medium text-surface-700 mb-2">Largest Files (Top 10)</h4>
                  <div className="overflow-x-auto scroll-slim border border-surface-200 rounded-lg">
                    <table className="table-shell">
                      <thead className="table-head">
                        <tr>
                          <th className="table-th">File</th>
                          <th className="table-th">Size</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-surface-100">
                        {largestFiles.slice(0, 10).map((file) => (
                          <tr key={file.filePath} className="hover:bg-surface-50 transition-colors">
                            <td className="table-td">
                              <div className="flex items-center gap-2">
                                <File className="h-4 w-4 text-surface-400 flex-shrink-0" />
                                <span className="truncate max-w-md font-mono text-xs" title={file.filePath}>{file.filePath}</span>
                              </div>
                            </td>
                            <td className="table-td font-medium text-surface-900 tnum">{formatBytes(file.sizeBytes)}</td>
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
