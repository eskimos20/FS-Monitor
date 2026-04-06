import React, { useState, useEffect } from 'react';
import { FileText, AlertCircle, CheckCircle, Loader, Clock } from 'lucide-react';
import { logConfigAPI } from '../../api/axios';

const LogControlTab = ({ logConfigs }) => {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedMatch, setExpandedMatch] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(new Date());

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

      {/* Results */}
      {!loading && matches.length > 0 && (
        <div className="card">
          <div className="space-y-4">
            {matches.map((match, index) => {
              const contextBefore = match.contextBefore ? match.contextBefore.split('\n').filter(l => l) : [];
              const contextAfter = match.contextAfter ? match.contextAfter.split('\n').filter(l => l) : [];
              const foundAt = new Date(match.foundAt);
              
              return (
                <div
                  key={match.id || index}
                  className="border border-gray-200 rounded-lg overflow-hidden"
                >
                  <div
                    className="bg-gray-50 p-4 cursor-pointer hover:bg-gray-100"
                    onClick={() => toggleMatchExpand(index)}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-2">
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
                        <p className="text-sm text-gray-600 truncate">{match.fileName}</p>
                        <p className="text-sm font-mono text-gray-900 mt-2 truncate">
                          {match.matchedLine}
                        </p>
                      </div>
                      <FileText className="h-5 w-5 text-gray-400 ml-4" />
                    </div>
                  </div>

                  {expandedMatch === index && (
                    <div className="bg-white p-4 border-t border-gray-200">
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
