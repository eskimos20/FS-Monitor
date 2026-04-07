import React, { useState, useEffect } from 'react';
import { FileText, CheckCircle, XCircle, ChevronDown, ChevronRight } from 'lucide-react';
import { logConfigAPI } from '../../api/axios';
import StatsCard from '../StatsCard';
import CollapsibleCard from '../CollapsibleCard';

const LogControlTab = ({ logConfigs }) => {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedConfigs, setExpandedConfigs] = useState(() => {
    const saved = localStorage.getItem('dashboard_logs_expanded');
    return saved ? JSON.parse(saved) : {};
  });
  const [expandedMatches, setExpandedMatches] = useState(() => {
    const saved = localStorage.getItem('dashboard_log_matches_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleConfigExpand = (id) => {
    setExpandedConfigs(prev => {
      const newExpanded = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_logs_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  const toggleMatchExpand = (key) => {
    setExpandedMatches(prev => {
      const newExpanded = { ...prev, [key]: !prev[key] };
      localStorage.setItem('dashboard_log_matches_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  useEffect(() => {
    fetchMatches();
    const interval = setInterval(fetchMatches, 10000);
    return () => clearInterval(interval);
  }, []);

  const fetchMatches = async () => {
    try {
      const response = await logConfigAPI.getRecentMatches(24);
      setMatches(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  // Group matches by log config
  const matchesByConfig = {};
  matches.forEach(match => {
    const configId = match.logConfig?.id || match.logConfigId;
    if (!matchesByConfig[configId]) {
      matchesByConfig[configId] = [];
    }
    matchesByConfig[configId].push(match);
  });

  const activeCount = logConfigs.filter(c => c.active).length;
  const inactiveCount = logConfigs.filter(c => !c.active).length;
  const totalMatchCount = matches.length;

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className={`grid grid-cols-1 ${totalMatchCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-6`}>
        <StatsCard
          icon={FileText}
          label="Total Log Monitors"
          value={logConfigs.length}
          iconColor="text-yellow-600"
        />
        <StatsCard
          icon={CheckCircle}
          label="Active"
          value={activeCount}
          iconColor="text-green-600"
        />
        {totalMatchCount > 0 && (
          <StatsCard
            icon={XCircle}
            label="Total Matches"
            value={totalMatchCount}
            iconColor="text-red-600"
          />
        )}
      </div>

      {/* Log Monitors Table */}
      <CollapsibleCard title="Log Monitoring" icon={FileText} defaultOpen={true} storageKey="dashboard_logs">
        <div className="p-6">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md text-sm mb-4">
              {error}
            </div>
          )}

          {logConfigs.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No log monitors configured</h3>
              <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first log monitor.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {logConfigs.map((config) => {
                const isExpanded = expandedConfigs[config.id] !== false;
                const configMatches = matchesByConfig[config.id] || [];
                
                return (
                  <div key={config.id} className="border border-gray-200 rounded-lg">
                    <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50" onClick={() => toggleConfigExpand(config.id)}>
                      <div className="flex items-center space-x-3 flex-1 min-w-0">
                        {isExpanded ? (
                          <ChevronDown className="h-5 w-5 text-gray-500 flex-shrink-0" />
                        ) : (
                          <ChevronRight className="h-5 w-5 text-gray-500 flex-shrink-0" />
                        )}
                        <div className="flex-1 min-w-0">
                          <h4 className="font-medium text-gray-900">{config.name}</h4>
                          <p className="text-sm text-gray-500 truncate">{config.path}</p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2 flex-shrink-0 ml-4">
                        {configMatches.length > 0 && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            {configMatches.length}
                          </span>
                        )}
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          config.active
                            ? 'bg-green-100 text-green-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}>
                          {config.active ? 'Active' : 'Inactive'}
                        </span>
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="border-t border-gray-100">
                        {/* Config details */}
                        <div className="px-4 py-3 bg-gray-50">
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            <div>
                              <p className="text-xs text-gray-500 mb-1">File Types</p>
                              <p className="text-sm text-gray-900 break-words">{config.fileTypes}</p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500 mb-1">Keywords</p>
                              <p className="text-sm text-gray-900 break-words">{config.keywords || 'All'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500 mb-1">Recursive</p>
                              <p className="text-sm text-gray-900">{config.recursive ? 'Yes' : 'No'}</p>
                            </div>
                          </div>
                        </div>

                        {/* Matches table */}
                        {configMatches.length > 0 && (
                          <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200">
                              <thead className="bg-gray-50">
                                <tr>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">File</th>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Keyword</th>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Line</th>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Match</th>
                                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Found</th>
                                  <th className="px-4 py-3"></th>
                                </tr>
                              </thead>
                              <tbody className="bg-white divide-y divide-gray-200">
                                {configMatches.map((match, index) => {
                                  const matchKey = `${config.id}-${index}`;
                                  const isMatchExpanded = expandedMatches[matchKey];
                                  const contextBefore = match.contextBefore ? match.contextBefore.split('\n').filter(l => l) : [];
                                  const contextAfter = match.contextAfter ? match.contextAfter.split('\n').filter(l => l) : [];
                                  const foundAt = new Date(match.foundAt);
                                  
                                  return (
                                    <React.Fragment key={matchKey}>
                                      <tr className="hover:bg-gray-50">
                                        <td className="px-4 py-3 text-sm text-gray-900">
                                          <div className="max-w-xs truncate" title={match.fileName}>{match.fileName}</div>
                                        </td>
                                        <td className="px-4 py-3">
                                          <span className="inline-flex items-center px-2 py-1 rounded text-xs font-mono bg-yellow-100 text-yellow-800">
                                            {match.keyword}
                                          </span>
                                        </td>
                                        <td className="px-4 py-3 text-sm text-gray-600">{match.lineNumber}</td>
                                        <td className="px-4 py-3 text-sm font-mono text-gray-900">
                                          <div className="max-w-md truncate" title={match.matchedLine}>{match.matchedLine}</div>
                                        </td>
                                        <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">
                                          {foundAt.toLocaleString()}
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                          <button
                                            onClick={(e) => {
                                              e.stopPropagation();
                                              toggleMatchExpand(matchKey);
                                            }}
                                            className="text-gray-400 hover:text-gray-600"
                                          >
                                            {isMatchExpanded ? (
                                              <ChevronDown className="h-4 w-4" />
                                            ) : (
                                              <ChevronRight className="h-4 w-4" />
                                            )}
                                          </button>
                                        </td>
                                      </tr>
                                      {isMatchExpanded && (
                                        <tr>
                                          <td colSpan="6" className="px-4 py-3 bg-gray-50">
                                            <div className="font-mono text-xs space-y-1 overflow-x-auto">
                                              {contextBefore.map((line, i) => (
                                                <div key={`before-${i}`} className="text-gray-500">
                                                  <span className="text-gray-400 mr-2 inline-block w-12 text-right">
                                                    {match.lineNumber - contextBefore.length + i}
                                                  </span>
                                                  <span className="whitespace-pre-wrap break-all">{line}</span>
                                                </div>
                                              ))}
                                              <div className="bg-yellow-100 text-yellow-900 font-semibold px-2 py-1 rounded">
                                                <span className="text-yellow-600 mr-2 inline-block w-12 text-right">{match.lineNumber}</span>
                                                <span className="whitespace-pre-wrap break-all">{match.matchedLine}</span>
                                              </div>
                                              {contextAfter.map((line, i) => (
                                                <div key={`after-${i}`} className="text-gray-500">
                                                  <span className="text-gray-400 mr-2 inline-block w-12 text-right">
                                                    {match.lineNumber + i + 1}
                                                  </span>
                                                  <span className="whitespace-pre-wrap break-all">{line}</span>
                                                </div>
                                              ))}
                                            </div>
                                          </td>
                                        </tr>
                                      )}
                                    </React.Fragment>
                                  );
                                })}
                              </tbody>
                            </table>
                          </div>
                        )}

                        {configMatches.length === 0 && (
                          <div className="p-4 text-center text-sm text-gray-500">
                            No matches found
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </CollapsibleCard>
    </div>
  );
};

export default LogControlTab;
