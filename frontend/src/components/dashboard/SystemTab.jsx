import React, { useState, useEffect, useMemo } from 'react';
import { Cpu, HardDrive, Activity, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
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
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* System Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatsCard
          icon={Cpu}
          label="CPU Usage (Total)"
          value={`${systemStats.cpuUsage.toFixed(1)}%`}
          iconColor="text-purple-600"
        />
        <StatsCard
          icon={HardDrive}
          label="Memory Usage"
          value={`${systemStats.memoryUsagePercent.toFixed(1)}%`}
          iconColor="text-orange-600"
        />
        <StatsCard
          icon={Activity}
          label="Processes"
          value={systemStats.processCount}
          iconColor="text-teal-600"
        />
      </div>

      {/* CPU per Core Visualization - htop style */}
      {systemStats.cpuPerCoreList && systemStats.cpuPerCoreList.length > 0 && (
        <div className="card">
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-lg font-semibold text-gray-900">CPU Usage per Core</h2>
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-1 text-xs text-gray-500">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <span>Live</span>
              </div>
              <div className="text-sm text-gray-500">
                {systemStats.availableProcessors} cores
              </div>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {systemStats.cpuPerCoreList.map((usage, index) => (
              <div key={index} className="flex items-center space-x-2">
                <div className="text-xs font-mono text-gray-600 w-8">
                  {index}:
                </div>
                <div className="flex-1 bg-gray-200 rounded-sm h-4 relative overflow-hidden">
                  <div 
                    className={`h-full transition-all duration-300 ${
                      usage > 80 ? 'bg-red-500' : usage > 50 ? 'bg-yellow-500' : 'bg-green-500'
                    }`}
                    style={{width: `${Math.min(usage, 100)}%`}}
                  ></div>
                </div>
                <div className="text-xs font-mono text-gray-900 w-12 text-right">
                  {usage.toFixed(1)}%
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Top Processes Table - Scrollable */}
      {systemStats.topProcesses && systemStats.topProcesses.length > 0 && (
        <div className="card">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-semibold text-gray-900">All Processes by CPU</h2>
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-1 text-xs text-gray-500">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <span>Live</span>
              </div>
              <div className="flex items-center space-x-2 text-sm text-gray-500">
                <Cpu className="h-4 w-4" />
                <span>{lastUpdate.toLocaleTimeString()}</span>
              </div>
            </div>
          </div>

          <div className="overflow-x-auto max-h-96 overflow-y-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50 sticky top-0">
                <tr>
                  <th 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('pid')}
                  >
                    <div className="flex items-center space-x-1">
                      <span>PID</span>
                      {sortColumn === 'pid' ? (
                        sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </th>
                  <th 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('user')}
                  >
                    <div className="flex items-center space-x-1">
                      <span>User</span>
                      {sortColumn === 'user' ? (
                        sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </th>
                  <th 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('name')}
                  >
                    <div className="flex items-center space-x-1">
                      <span>Process</span>
                      {sortColumn === 'name' ? (
                        sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </th>
                  <th 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('cpuUsage')}
                  >
                    <div className="flex items-center space-x-1">
                      <span>CPU %</span>
                      {sortColumn === 'cpuUsage' ? (
                        sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </th>
                  <th 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('memoryMB')}
                  >
                    <div className="flex items-center space-x-1">
                      <span>Memory</span>
                      {sortColumn === 'memoryMB' ? (
                        sortDirection === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Command</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {sortedProcesses.map((process, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {process.pid}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {process.user}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {process.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex items-center">
                        <div className="flex-1 bg-gray-200 rounded-full h-2 mr-2" style={{width: '60px'}}>
                          <div 
                            className="bg-purple-600 h-2 rounded-full" 
                            style={{width: `${Math.min(process.cpuUsage, 100)}%`}}
                          ></div>
                        </div>
                        <span>{process.cpuUsage.toFixed(1)}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex flex-col">
                        <span className="font-medium">{process.memoryMB} MB</span>
                        <span className="text-xs text-gray-500">{process.memoryUsage.toFixed(1)}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500 max-w-md truncate">
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
