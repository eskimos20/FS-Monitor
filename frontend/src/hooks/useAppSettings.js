import { useState, useEffect, useCallback } from 'react';
import { appSettingsAPI } from '../api/axios';

const DEFAULT_REFRESH_INTERVAL_SECONDS = 5;

export const useAppSettings = () => {
  const [appSettings, setAppSettings] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchAppSettings = useCallback(async () => {
    try {
      const response = await appSettingsAPI.getCurrent();
      setAppSettings(response.data);
    } catch (error) {
      // Failed to load - using defaults
      setAppSettings({ refreshIntervalSeconds: DEFAULT_REFRESH_INTERVAL_SECONDS });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAppSettings();
  }, [fetchAppSettings]);

  const refreshIntervalSeconds = appSettings?.refreshIntervalSeconds || DEFAULT_REFRESH_INTERVAL_SECONDS;

  return {
    appSettings,
    loading,
    refreshIntervalMs: refreshIntervalSeconds * 1000,
    refetch: fetchAppSettings
  };
};
