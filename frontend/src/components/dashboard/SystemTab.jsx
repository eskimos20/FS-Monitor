import React, { useState, useEffect, useMemo } from 'react';
import { Cpu, HardDrive, Activity, ArrowUpDown, ArrowUp, ArrowDown, Loader2 } from 'lucide-react';
import StatsCard from '../StatsCard';

const SystemTab = ({ systemStats }) => {
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [sortColumn, setSortColumn] = useState('cpuUsage');
  const [sortDirection, setSortDirection] = useState('desc');

  useEffect(() => {
    if (systemStats) {
      setLastUpdate(new Date());
    }
  }, [systemStats]);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('desc');
    }
  };

  const sortedProcesses = useMemo(() => {
    if (!systemStats?.topProcesses) return [];
    
    const processes = [...systemStats.topProcesses];
    
    processes.sort((a, b) => {
      let aVal, bVal;
      
      switch (sortColumn) {
        case 'pid':
          aVal = parseInt(a.pid);
          bVal = parseInt(b.pid);
          break;
        case 'user':
          aVal = a.user.toLowerCase();
          bVal = b.user.toLowerCase();
          break;
        case 'name':
          aVal = a.name.toLowerCase();
          bVal = b.name.toLowerCase();
          break;
        case 'cpuUsage':
          aVal = a.cpuUsage;
          bVal = b.cpuUsage;
          break;
        case 'memoryMB':
          aVal = a.memoryMB;
          bVal = b.memoryMB;
          break;
        default:
          return 0;
      }
      
      if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
    
    return processes;
  }, [systemStats?.topProcesses, sortColumn, sortDirection]);

  if (!systemStats) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* System Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <StatsCard
          icon={Cpu}
          label="CPU Usage (Total)"
          value={`${systemStats.cpuUsage.toFixed(1)}%`}
          iconColor="text-purple-600"
          tint="bg-purple-50"
          percent={systemStats.cpuUsage}
        />
        <StatsCard
          icon={HardDrive}
          label="Memory Usage"
          value={`${systemStats.memoryUsagePercent.toFixed(1)}%`}
          iconColor="text-orange-600"
          tint="bg-orange-50"
          percent={systemStats.memoryUsagePercent}
        />
        <StatsCard
          icon={Activity}
          label="Processes"
          value={systemStats.processCount}
          iconColor="text-teal-600"
          tint="bg-teal-50"
        />
      </div>

      {/* CPU per Core Visualization - htop style */}
      {systemStats.cpuPerCoreList && systemStats.cpuPerCoreList.length > 0 && (
        <div className="card">
          <div className="flex justify-between items-center mb-4">
            <h2 className="section-title">CPU Usage per Core</h2>
            <div className="flex items-center gap-3">
              <span className="badge-neutral">
                <span className="status-dot bg-emerald-500 animate-pulse" />
                Live
              </span>
              <span className="text-xs text-surface-500 tnum">
                {systemStats.availableProcessors} cores
              </span>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2">
            {systemStats.cpuPerCoreList.map((usage, index) => (
              <div key={index} className="flex items-center gap-2">
                <div className="text-xs font-mono text-surface-500 w-7 tnum">
                  {index}
                </div>
                <div className="flex-1 bg-surface-100 rounded-full h-2 relative overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      usage > 80 ? 'bg-red-500' : usage > 50 ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}
                    style={{width: `${Math.min(usage, 100)}%`}}
                  />
                </div>
                <div className="text-xs font-mono text-surface-700 w-12 text-right tnum">
                  {usage.toFixed(1)}%
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Top Processes Table - Scrollable */}
      {systemStats.topProcesses && systemStats.topProcesses.length > 0 && (
        <div className="card !p-0 overflow-hidden">
          <div className="flex justify-between items-center px-6 py-4 border-b border-surface-100">
            <h2 className="section-title">Processes by CPU</h2>
            <div className="flex items-center gap-3">
              <span className="badge-neutral">
                <span className="status-dot bg-emerald-500 animate-pulse" />
                Live
              </span>
              <span className="flex items-center gap-1.5 text-xs text-surface-500 tnum">
                <Cpu className="h-3.5 w-3.5" />
                {lastUpdate.toLocaleTimeString()}
              </span>
            </div>
          </div>

          <div className="overflow-x-auto scroll-slim max-h-96 overflow-y-auto">
            <table className="table-shell">
              <thead className="table-head sticky top-0">
                <tr>
                  {[
                    { key: 'pid', label: 'PID' },
                    { key: 'user', label: 'User' },
                    { key: 'name', label: 'Process' },
                    { key: 'cpuUsage', label: 'CPU %' },
                    { key: 'memoryMB', label: 'Memory' },
                  ].map((col) => (
                    <th
                      key={col.key}
                      className="table-th cursor-pointer select-none hover:text-surface-700"
                      onClick={() => handleSort(col.key)}
                    >
                      <div className="flex items-center gap-1">
                        <span>{col.label}</span>
                        {sortColumn === col.key ? (
                          sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                        ) : (
                          <ArrowUpDown className="h-3 w-3 opacity-30" />
                        )}
                      </div>
                    </th>
                  ))}
                  <th className="table-th">Command</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-surface-100">
                {sortedProcesses.map((process, index) => (
                  <tr key={index} className="hover:bg-surface-50 transition-colors">
                    <td className="table-td whitespace-nowrap font-mono text-xs tnum">{process.pid}</td>
                    <td className="table-td whitespace-nowrap">{process.user}</td>
                    <td className="table-td whitespace-nowrap font-medium text-surface-900">{process.name}</td>
                    <td className="table-td whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div className="w-14 bg-surface-100 rounded-full h-1.5 overflow-hidden">
                          <div
                            className={`h-1.5 rounded-full ${
                              process.cpuUsage > 80 ? 'bg-red-500' : process.cpuUsage > 50 ? 'bg-amber-500' : 'bg-primary-500'
                            }`}
                            style={{width: `${Math.min(process.cpuUsage, 100)}%`}}
                          />
                        </div>
                        <span className="tnum">{process.cpuUsage.toFixed(1)}%</span>
                      </div>
                    </td>
                    <td className="table-td whitespace-nowrap">
                      <div className="flex flex-col">
                        <span className="font-medium tnum">{process.memoryMB} MB</span>
                        <span className="text-xs text-surface-400 tnum">{process.memoryUsage.toFixed(1)}%</span>
                      </div>
                    </td>
                    <td className="table-td text-surface-500 max-w-md truncate font-mono text-xs">
                      {process.command}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default SystemTab;
