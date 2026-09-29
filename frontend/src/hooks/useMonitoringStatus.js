import { useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

// Global state to persist timers across component mounts
let globalTimerState = {
  secondsUntilNextRun: null,
  integrationTimers: {},
  lastUpdate: Date.now()
};

export const useMonitoringStatus = (refreshInterval, settingsIntervalMs = null) => {
  // Poll backend at the user's refresh interval; the 1s countdown below
  // interpolates between polls so the UI updates smoothly
  const pollIntervalMs = settingsIntervalMs || refreshInterval || 60000;

  const [secondsUntilNextRun, setSecondsUntilNextRun] = useState(globalTimerState.secondsUntilNextRun);
  const [integrationTimers, setIntegrationTimers] = useState(globalTimerState.integrationTimers);

  const fetchMonitoringStatus = useCallback(async () => {
    try {
      const response = await api.get('/monitoring/status');
      const backendSeconds = response.data.secondsUntilNextRun;
      const backendTimers = response.data.integrationTimers || {};

      // Update global state
      globalTimerState.secondsUntilNextRun = backendSeconds;
      globalTimerState.lastUpdate = Date.now();

      const newTimers = {};
      for (const [id, timerData] of Object.entries(backendTimers)) {
        newTimers[id] = timerData.secondsUntilNextRun;
      }
      globalTimerState.integrationTimers = newTimers;

      // Update local state
      setSecondsUntilNextRun(backendSeconds);
      setIntegrationTimers(newTimers);
    } catch (error) {
      // Silently fail - next poll retries
    }
  }, []);

  useEffect(() => {
    // Instant paint from cache (compensating for time elapsed while unmounted),
    // then always fetch fresh truth immediately
    const elapsedSeconds = Math.floor((Date.now() - globalTimerState.lastUpdate) / 1000);
    if (globalTimerState.secondsUntilNextRun !== null && elapsedSeconds > 0) {
      const adjustedSeconds = Math.max(0, globalTimerState.secondsUntilNextRun - elapsedSeconds);
      const adjustedTimers = {};
      for (const [id, seconds] of Object.entries(globalTimerState.integrationTimers)) {
        adjustedTimers[id] = Math.max(0, seconds - elapsedSeconds);
      }
      setSecondsUntilNextRun(adjustedSeconds);
      setIntegrationTimers(adjustedTimers);
      globalTimerState.secondsUntilNextRun = adjustedSeconds;
      globalTimerState.integrationTimers = adjustedTimers;
      globalTimerState.lastUpdate = Date.now();
    }
    fetchMonitoringStatus();

    // Local 1s countdown between polls - never goes below 0; the next poll
    // resynchronises (backend reports the real schedule after each scan)
    const countdownTimer = setInterval(() => {
      setSecondsUntilNextRun(prev => {
        const newValue = prev !== null ? Math.max(0, prev - 1) : null;
        globalTimerState.secondsUntilNextRun = newValue;
        globalTimerState.lastUpdate = Date.now();
        return newValue;
      });

      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, seconds] of Object.entries(prev)) {
          newTimers[id] = Math.max(0, seconds - 1);
        }
        globalTimerState.integrationTimers = newTimers;
        return newTimers;
      });
    }, 1000);

    const fetchTimer = setInterval(fetchMonitoringStatus, pollIntervalMs);

    return () => {
      clearInterval(countdownTimer);
      clearInterval(fetchTimer);
    };
  }, [fetchMonitoringStatus, pollIntervalMs]);

  return {
    secondsUntilNextRun,
    integrationTimers,
    fetchMonitoringStatus
  };
};
