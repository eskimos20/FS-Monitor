import React, { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle, CheckCircle2, Server, FolderOpen, HardDrive,
  Trash2, FileSearch, ChevronRight, Loader2
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api, { logConfigAPI } from '../api/axios';
import { usePolling } from '../hooks/usePolling';
import { useIntegrations } from '../hooks/useIntegrations';
import { useServices } from '../hooks/useServices';
import { useDeleteServices } from '../hooks/useDeleteServices';
import { useLogConfigs } from '../hooks/useLogConfigs';
import { useAppSettings } from '../hooks/useAppSettings';
import PageHeader from '../components/ui/PageHeader';
import EmptyState from '../components/ui/EmptyState';
import StatusBadge from '../components/ui/StatusBadge';
import { isIntegrationHealthy } from '../utils/formatters';

const INTERVAL_MS = {
  MINUTES: 60 * 1000,
  HOURS: 60 * 60 * 1000,
  DAYS: 24 * 60 * 60 * 1000,
  WEEKS: 7 * 24 * 60 * 60 * 1000,
  MONTHS: 30 * 24 * 60 * 60 * 1000,
};

const DISK_WARN_PCT = 90;

const ProblemSection = ({ title, count, to, children }) => (
  <div className="bg-white border border-surface-200 rounded-xl shadow-card overflow-hidden">
    <div className="flex items-center justify-between gap-3 px-4 py-3 border-b border-surface-100">
      <div className="flex items-center gap-2.5 min-w-0">
        <AlertTriangle className="h-4 w-4 text-red-600 flex-shrink-0" />
        <h3 className="text-sm font-semibold text-surface-900 truncate">{title}</h3>
        <span className="badge-danger tnum">{count}</span>
      </div>
      <Link
        to={to}
        className="flex items-center gap-1 text-xs font-medium text-primary-600 hover:text-primary-700 flex-shrink-0"
      >
        View all <ChevronRight className="h-3.5 w-3.5" />
      </Link>
    </div>
    <div className="divide-y divide-surface-100">{children}</div>
  </div>
);

const ProblemRow = ({ icon: Icon, title, subtitle, detail }) => (
  <div className="flex items-center justify-between gap-4 px-4 py-2.5">
    <div className="flex items-center gap-2.5 min-w-0">
      <Icon className="h-4 w-4 text-surface-400 flex-shrink-0" />
      <div className="min-w-0">
        <p className="text-sm font-medium text-surface-900 truncate">{title}</p>
        {subtitle && <p className="text-xs text-surface-500 truncate font-mono mt-0.5">{subtitle}</p>}
      </div>
    </div>
    {detail && <div className="flex-shrink-0">{detail}</div>}
  </div>
);

const DashboardPage = () => {
  const { user } = useAuth();
  const { refreshIntervalMs } = useAppSettings();
  const { integrations, loading: integrationsLoading } = useIntegrations(refreshIntervalMs);
  const { services, loading: servicesLoading } = useServices(refreshIntervalMs);
  const { deleteServices, loading: deleteServicesLoading } = useDeleteServices(refreshIntervalMs);
  const { logConfigs } = useLogConfigs(refreshIntervalMs);
  const [storageData, setStorageData] = useState({});
  const [storageLoaded, setStorageLoaded] = useState(false);
  const [matches, setMatches] = useState([]);
  const [matchesLoaded, setMatchesLoaded] = useState(false);

  const fetchStorage = useCallback(async () => {
    try {
      const response = await api.get('/storage-configs/dashboard');
      setStorageData(response.data);
    } catch (err) {
      // Storage section is optional on the overview - keep last known data
    } finally {
      setStorageLoaded(true);
    }
  }, []);
  usePolling(fetchStorage, 30000);

  const fetchMatches = useCallback(async () => {
    try {
      const response = await logConfigAPI.getRecentMatches(24);
      setMatches(response.data);
    } catch (err) {
      // Non-fatal for the overview
    } finally {
      setMatchesLoaded(true);
    }
  }, []);
  usePolling(fetchMatches, refreshIntervalMs || 60000);

  // --- Collect problems per category ---

  const offlineServices = services.filter(s => s.isActive && s.status === 'OFFLINE');

  const unhealthyIntegrations = integrations.filter(i =>
    i.isActive && i.monitoringEnabled &&
    !isIntegrationHealthy(i.isActive, i.lastFileFound, i.thresholdMinutes)
  );

  const storageWarnings = [];
  const systemStorage = storageData['-1'];
  if (systemStorage?.diskSpace?.totalBytes > 0) {
    const used = Math.max(systemStorage.diskSpace.totalBytes - (systemStorage.diskSpace.usableBytes || 0), 0);
    const pct = (used / systemStorage.diskSpace.totalBytes) * 100;
    if (pct >= DISK_WARN_PCT) {
      storageWarnings.push({ label: 'System storage', detail: `${pct.toFixed(1)}% used` });
    }
  }
  (systemStorage?.info || []).forEach(mount => {
    const total = mount.totalSizeBytes || 0;
    const pct = total > 0 ? ((total - (mount.freeSpaceBytes || 0)) / total) * 100 : 0;
    if (pct >= DISK_WARN_PCT) {
      storageWarnings.push({ label: mount.path, detail: `${pct.toFixed(1)}% used` });
    }
  });

  const now = Date.now();
  const overdueDeletes = deleteServices.filter(d => {
    if (!d.cleanupEnabled) return false;
    if (!d.lastCleanup) return true;
    const intervalMs = (INTERVAL_MS[d.cleanupIntervalUnit] || 0) * (d.cleanupIntervalValue || 0);
    return intervalMs > 0 && new Date(d.lastCleanup).getTime() + intervalMs < now;
  });

  const matchCountByConfig = {};
  matches.forEach(m => {
    const id = m.logConfig?.id || m.logConfigId;
    matchCountByConfig[id] = (matchCountByConfig[id] || 0) + 1;
  });
  const logIssues = logConfigs
    .map(c => ({ config: c, count: matchCountByConfig[c.id] || 0 }))
    .filter(x => x.count > 0);

  const totalProblems =
    offlineServices.length + unhealthyIntegrations.length +
    storageWarnings.length + overdueDeletes.length + logIssues.length;

  const loading = servicesLoading || integrationsLoading || deleteServicesLoading ||
    !storageLoaded || !matchesLoaded;

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Dashboard"
        subtitle={`Welcome${user?.username ? `, ${user.username}` : ''}`}
      />

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
        </div>
      ) : totalProblems === 0 ? (
        <div className="card">
          <EmptyState
            icon={CheckCircle2}
            title="All systems operational"
            description="No offline services, unhealthy integrations, storage warnings, overdue cleanups or log matches right now."
          />
        </div>
      ) : (
        <div className="space-y-4">
          {offlineServices.length > 0 && (
            <ProblemSection title="Services offline" count={offlineServices.length} to="/services">
              {offlineServices.map(s => (
                <ProblemRow
                  key={s.id}
                  icon={Server}
                  title={s.name}
                  subtitle={`${s.host}${s.port ? ':' + s.port : ''}`}
                  detail={<StatusBadge variant="danger" label="OFFLINE" />}
                />
              ))}
            </ProblemSection>
          )}

          {unhealthyIntegrations.length > 0 && (
            <ProblemSection title="File integrations with problems" count={unhealthyIntegrations.length} to="/integrations">
              {unhealthyIntegrations.map(i => (
                <ProblemRow
                  key={i.id}
                  icon={FolderOpen}
                  title={i.name}
                  subtitle={i.path}
                  detail={
                    <span className="text-xs text-red-600 whitespace-nowrap">
                      {i.lastFileFound
                        ? `Stale since ${new Date(i.lastFileFound).toLocaleString()}`
                        : 'No files found'}
                    </span>
                  }
                />
              ))}
            </ProblemSection>
          )}

          {storageWarnings.length > 0 && (
            <ProblemSection title="Storage warnings" count={storageWarnings.length} to="/storage">
              {storageWarnings.map((w, idx) => (
                <ProblemRow
                  key={idx}
                  icon={HardDrive}
                  title={w.label}
                  detail={<StatusBadge variant="danger" label={w.detail} />}
                />
              ))}
            </ProblemSection>
          )}

          {overdueDeletes.length > 0 && (
            <ProblemSection title="Delete services overdue" count={overdueDeletes.length} to="/delete-services">
              {overdueDeletes.map(d => (
                <ProblemRow
                  key={d.id}
                  icon={Trash2}
                  title={d.name}
                  subtitle={d.path}
                  detail={
                    <span className="text-xs text-red-600 whitespace-nowrap">
                      {d.lastCleanup
                        ? `Last ran ${new Date(d.lastCleanup).toLocaleString()}`
                        : 'Never ran'}
                    </span>
                  }
                />
              ))}
            </ProblemSection>
          )}

          {logIssues.length > 0 && (
            <ProblemSection title="Log matches (last 24h)" count={logIssues.reduce((a, x) => a + x.count, 0)} to="/log-control">
              {logIssues.map(({ config, count }) => (
                <ProblemRow
                  key={config.id}
                  icon={FileSearch}
                  title={config.name}
                  subtitle={config.path}
                  detail={<StatusBadge variant="danger" label={`${count} matches`} />}
                />
              ))}
            </ProblemSection>
          )}
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
