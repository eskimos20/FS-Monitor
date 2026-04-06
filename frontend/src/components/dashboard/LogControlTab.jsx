import React, { useState, useEffect } from 'react';
import { FileText, AlertCircle, CheckCircle, Loader, Clock, ChevronDown, ChevronRight } from 'lucide-react';
import { logConfigAPI } from '../../api/axios';
import CollapsibleCard from '../CollapsibleCard';

const LogControlTab = ({ logConfigs }) => {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedMatch, setExpandedMatch] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [expandedConfigs, setExpandedConfigs] = useState(() => {
    const saved = localStorage.getItem('dashboard_logs_expanded');
    return saved ? JSON.parse(saved) : {};
  });

  const toggleConfigExpand = (id) => {
    setExpandedConfigs(prev => {
      const newExpanded = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_logs_expanded', JSON.stringify(newExpanded));
      return newExpanded;
    });
  };

  useEffect(() => {
    fetchMatches();
    const interval = setInterval(fetchMatches, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchMatches = async () => {
    try {
      const response = await logConfigAPI.getRecentMatches(24);
      setMatches(response.data);
      setLastUpdate(new Date());
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleMatchExpand = (index) => {
    setExpandedMatch(expandedMatch === index ? null : index);
  };

  // Group matches by log config
  const matchesByConfig = {};
  matches.forEach(match => {
    const configId = match.logConfig?.id || match.logConfigId;
    if (!matchesByConfig[configId]) {
      matchesByConfig[configId] = [];
    }
    matchesByConfig[configId].push(match);
  });

  const activeConfigs = logConfigs.filter(c => c.active);

  return (
    <div className="space-y-6">
      {/* Header with stats */}
      <div className="card">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Log Monitoring Results</h2>
            <p className="text-sm text-gray-500 mt-1">
              Showing matches from the last 24 hours
            </p>
          </div>
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span>Auto-refresh</span>
            </div>
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <Clock className="h-4 w-4" />
              <span>{lastUpdate.toLocaleTimeString()}</span>
            </div>
            <div className="text-sm font-medium text-gray-900">
              {matches.length} {matches.length === 1 ? 'match' : 'matches'}
            </div>
          </div>
        </div>
      </div>

      {/* Log Configs with their matches */}
      <CollapsibleCard title="Log Monitoring" icon={FileText} defaultOpen={true} storageKey="dashboard_logs">
        <div className="p-6">
          {logConfigs.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No log monitors configured</h3>
              <p className="mt-1 text-sm text-gray-500">Go to Settings to add your first log monitor.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {logConfigs.map((config) => {
                const isExpanded = expandedConfigs[config.id] !== false;
                const configMatches = matchesByConfig[config.id] || [];
                
                return (
                  <div key={config.id} className="border border-gray-200 rounded-lg">
                    <div className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50" onClick={() => toggleConfigExpand(config.id)}>
                      <div className="flex items-center space-x-3 flex-1">
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
                      <div className="flex items-center space-x-2">
                        {configMatches.length > 0 && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            {configMatches.length} {configMatches.length === 1 ? 'match' : 'matches'}
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
                              <p className="text-sm text-gray-900">{config.fileTypes}</p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500 mb-1">Keywords</p>
                              <p className="text-sm text-gray-900">{config.keywords || 'All'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500 mb-1">Recursive</p>
                              <p className="text-sm text-gray-900">{config.recursive ? 'Yes' : 'No'}</p>
                            </div>
                          </div>
                        </div>

                        {/* Matches for this config */}
                        {configMatches.length > 0 && (
                          <div className="p-4 space-y-3">
                            <h5 className="text-sm font-medium text-gray-700">Recent Matches:</h5>
                            {configMatches.map((match, index) => {
                              const contextBefore = match.contextBefore ? match.contextBefore.split('\n').filter(l => l) : [];
                              const contextAfter = match.contextAfter ? match.contextAfter.split('\n').filter(l => l) : [];
                              const foundAt = new Date(match.foundAt);
                              const matchKey = `${config.id}-${index}`;
                              
                              return (
                                <div
                                  key={match.id || matchKey}
                                  className="border border-gray-200 rounded-lg overflow-hidden"
                                >
                                  <div
                                    className="bg-gray-50 p-3 cursor-pointer hover:bg-gray-100"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleMatchExpand(matchKey);
                                    }}
                                  >
                                    <div className="flex items-start justify-between">
                                      <div className="flex-1">
                                        <div className="flex items-center space-x-2 mb-1">
                                          <span className="text-xs font-mono bg-yellow-100 text-yellow-800 px-2 py-1 rounded">
                                            {match.keyword}
                                          </span>
                                          <span className="text-xs text-gray-500">
                                            Line {match.lineNumber}
                                          </span>
                                          <span className="text-xs text-gray-400">
                                            {foundAt.toLocaleString()}
                                          </span>
                                        </div>
                                        <p className="text-xs text-gray-600 truncate">{match.fileName}</p>
                                        <p className="text-xs font-mono text-gray-900 mt-1 truncate">
                                          {match.matchedLine}
                                        </p>
                                      </div>
                                      <FileText className="h-4 w-4 text-gray-400 ml-2 flex-shrink-0" />
                                    </div>
                                  </div>

                                  {expandedMatch === matchKey && (
                                    <div className="bg-white p-3 border-t border-gray-200">
                                      <div className="font-mono text-xs space-y-1">
                                        {/* Context Before */}
                                        {contextBefore.map((line, i) => (
                                          <div key={`before-${i}`} className="text-gray-500">
                                            <span className="text-gray-400 mr-2">
                                              {match.lineNumber - contextBefore.length + i}
                                            </span>
                                            {line}
                                          </div>
                                        ))}

                                        {/* Matched Line */}
                                        <div className="bg-yellow-50 text-yellow-900 font-semibold px-2 py-1 rounded">
                                          <span className="text-yellow-600 mr-2">{match.lineNumber}</span>
                                          {match.matchedLine}
                                        </div>

                                        {/* Context After */}
                                        {contextAfter.map((line, i) => (
                                          <div key={`after-${i}`} className="text-gray-500">
                                            <span className="text-gray-400 mr-2">
                                              {match.lineNumber + i + 1}
                                            </span>
                                            {line}
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}

                        {configMatches.length === 0 && (
                          <div className="p-4 text-center text-sm text-gray-500">
                            No matches found for this log configuration
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

      {/* Loading State */}
      {loading && (
        <div className="card">
          <div className="flex items-center justify-center py-12">
            <Loader className="h-8 w-8 text-primary-600 animate-spin" />
            <span className="ml-3 text-gray-600">Searching logs...</span>
          </div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="card bg-red-50 border border-red-200">
          <div className="flex items-center">
            <AlertCircle className="h-5 w-5 text-red-600 mr-3" />
            <p className="text-red-800">{error}</p>
          </div>
        </div>
      )}

      {/* No Results */}
      {!loading && matches.length === 0 && !error && (
        <div className="card">
          <div className="text-center py-12">
            <CheckCircle className="h-12 w-12 text-green-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No Matches Found</h3>
            <p className="text-gray-500">
              No errors or keywords found in the monitored log files
            </p>
            {activeConfigs.length === 0 && (
              <p className="text-gray-500 mt-2">
                Go to Settings → Log Monitoring to configure log monitoring
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default LogControlTab;
