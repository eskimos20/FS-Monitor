import React, { useState, useCallback } from 'react';
import { FileText, CheckCircle2, AlertTriangle, ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import { logConfigAPI } from '../../api/axios';
import { usePolling } from '../../hooks/usePolling';
import StatsCard from '../StatsCard';
import CollapsibleCard from '../CollapsibleCard';
import EmptyState from '../ui/EmptyState';
import StatusBadge from '../ui/StatusBadge';
import DetailItem from '../ui/DetailItem';
import ExpandableRow from '../ui/ExpandableRow';

const toLines = (value) =>
  Array.isArray(value) ? value.filter(Boolean) : (value || '').split('\n').filter(Boolean);

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
      const next = { ...prev, [id]: !prev[id] };
      localStorage.setItem('dashboard_logs_expanded', JSON.stringify(next));
      return next;
    });
  };

  const toggleMatchExpand = (key) => {
    setExpandedMatches(prev => {
      const next = { ...prev, [key]: !prev[key] };
      localStorage.setItem('dashboard_log_matches_expanded', JSON.stringify(next));
      return next;
    });
  };

  const fetchMatches = useCallback(async () => {
    try {
      const response = await logConfigAPI.getRecentMatches(24);
      setMatches(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchMatches, 10000);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  // Group matches by log config
  const matchesByConfig = {};
  matches.forEach(match => {
    const configId = match.logConfig?.id || match.logConfigId;
    (matchesByConfig[configId] = matchesByConfig[configId] || []).push(match);
  });

  const activeCount = logConfigs.filter(c => c.active).length;
  const totalMatchCount = matches.length;

  return (
    <div className="space-y-6">
      <div className={`grid grid-cols-1 ${totalMatchCount > 0 ? 'md:grid-cols-3' : 'md:grid-cols-2'} gap-5`}>
        <StatsCard icon={FileText} label="Total Log Monitors" value={logConfigs.length}
          iconColor="text-amber-600" tint="bg-amber-50" />
        <StatsCard icon={CheckCircle2} label="Active" value={activeCount}
          iconColor="text-emerald-600" tint="bg-emerald-50" />
        {totalMatchCount > 0 && (
          <StatsCard icon={AlertTriangle} label="Matches (24h)" value={totalMatchCount}
            iconColor="text-red-600" tint="bg-red-50" />
        )}
      </div>

      <CollapsibleCard title="Log Monitoring" icon={FileText} defaultOpen={true} storageKey="dashboard_logs">
        <div className="p-5">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm mb-4">
              {error}
            </div>
          )}

          {logConfigs.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No log monitors configured"
              description="Go to Settings to add your first log monitor."
            />
          ) : (
            <div className="space-y-2.5">
              {logConfigs.map((config) => {
                const isExpanded = expandedConfigs[config.id] !== false;
                const configMatches = matchesByConfig[config.id] || [];

                return (
                  <ExpandableRow
                    key={config.id}
                    expanded={isExpanded}
                    onToggle={() => toggleConfigExpand(config.id)}
                    title={config.name}
                    subtitle={config.path}
                    aside={
                      <>
                        {configMatches.length > 0 && (
                          <span className="badge-danger tnum">{configMatches.length}</span>
                        )}
                        <StatusBadge
                          variant={config.active ? 'success' : 'neutral'}
                          label={config.active ? 'Active' : 'Inactive'}
                          pulse={config.active}
                        />
                      </>
                    }
                  >
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-x-6 gap-y-4 mb-4">
                      <DetailItem label="File Types">
                        <span className="font-mono text-xs">{config.fileTypes || 'All files'}</span>
                      </DetailItem>
                      <DetailItem label="Keywords">
                        <span className="font-mono text-xs break-all">{config.keywords || 'All'}</span>
                      </DetailItem>
                      <DetailItem label="Recursive">
                        {config.recursive ? 'Yes' : 'No'}
                      </DetailItem>
                    </div>

                    {configMatches.length === 0 ? (
                      <p className="text-center text-sm text-surface-400 py-4">No matches in the last 24h</p>
                    ) : (
                      <div className="overflow-x-auto border border-surface-200 rounded-lg">
                        <table className="table-shell">
                          <thead className="table-head">
                            <tr>
                              <th className="table-th">File</th>
                              <th className="table-th">Keyword</th>
                              <th className="table-th">Line</th>
                              <th className="table-th">Match</th>
                              <th className="table-th">Found</th>
                              <th className="table-th w-10"></th>
                            </tr>
                          </thead>
                          <tbody className="bg-white divide-y divide-surface-100">
                            {configMatches.map((match, index) => {
                              const matchKey = `${config.id}-${index}`;
                              const isMatchExpanded = expandedMatches[matchKey];
                              const contextBefore = toLines(match.contextBefore);
                              const contextAfter = toLines(match.contextAfter);

                              return (
                                <React.Fragment key={matchKey}>
                                  <tr className="hover:bg-surface-50 transition-colors">
                                    <td className="table-td">
                                      <div className="max-w-[220px] truncate font-mono text-xs" title={match.fileName}>
                                        {match.fileName}
                                      </div>
                                    </td>
                                    <td className="table-td">
                                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-mono bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-600/20">
                                        {match.keyword}
                                      </span>
                                    </td>
                                    <td className="table-td tnum">{match.lineNumber}</td>
                                    <td className="table-td font-mono text-xs">
                                      <div className="max-w-md truncate" title={match.matchedLine}>{match.matchedLine}</div>
                                    </td>
                                    <td className="table-td text-xs text-surface-500 whitespace-nowrap">
                                      {match.foundAt ? new Date(match.foundAt).toLocaleString() : '—'}
                                    </td>
                                    <td className="table-td text-right">
                                      <button
                                        onClick={(e) => { e.stopPropagation(); toggleMatchExpand(matchKey); }}
                                        className="p-1 rounded-md text-surface-400 hover:text-surface-700 hover:bg-surface-100 transition-colors"
                                        aria-label="Toggle context"
                                      >
                                        {isMatchExpanded
                                          ? <ChevronDown className="h-4 w-4" />
                                          : <ChevronRight className="h-4 w-4" />}
                                      </button>
                                    </td>
                                  </tr>
                                  {isMatchExpanded && (
                                    <tr>
                                      <td colSpan="6" className="px-4 py-3 bg-surface-50">
                                        <div className="font-mono text-xs space-y-0.5 overflow-x-auto scroll-slim max-h-72">
                                          {contextBefore.map((line, i) => (
                                            <div key={`b-${i}`} className="text-surface-500 flex">
                                              <span className="text-surface-400 mr-3 inline-block w-12 text-right flex-shrink-0 tnum">
                                                {match.lineNumber - contextBefore.length + i}
                                              </span>
                                              <span className="whitespace-pre-wrap break-all">{line}</span>
                                            </div>
                                          ))}
                                          <div className="bg-amber-50 text-amber-900 font-medium px-2 py-1 rounded flex border border-amber-200/60">
                                            <span className="text-amber-600 mr-3 inline-block w-12 text-right flex-shrink-0 tnum">
                                              {match.lineNumber}
                                            </span>
                                            <span className="whitespace-pre-wrap break-all">{match.matchedLine}</span>
                                          </div>
                                          {contextAfter.map((line, i) => (
                                            <div key={`a-${i}`} className="text-surface-500 flex">
                                              <span className="text-surface-400 mr-3 inline-block w-12 text-right flex-shrink-0 tnum">
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
                  </ExpandableRow>
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
