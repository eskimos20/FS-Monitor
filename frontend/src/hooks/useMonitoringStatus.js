import { useState, useEffect, useCallback, useRef } from 'react';
import api from '../api/axios';

// Global state to persist timers across component mounts
let globalTimerState = {
  secondsUntilNextRun: null,
  integrationTimers: {},
  lastUpdate: Date.now()
};

export const useMonitoringStatus = (refreshInterval) => {
  const [secondsUntilNextRun, setSecondsUntilNextRun] = useState(globalTimerState.secondsUntilNextRun);
  const [integrationTimers, setIntegrationTimers] = useState(globalTimerState.integrationTimers);
  const lastFetchRef = useRef(0);
  const mountTimeRef = useRef(Date.now());

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
      
      lastFetchRef.current = Date.now();
    } catch (error) {
      // Silently fail - backend will retry
    }
  }, []);

  useEffect(() => {
    // On mount, check if we have recent data in global state
    const timeSinceLastUpdate = Date.now() - globalTimerState.lastUpdate;
    const elapsedSeconds = Math.floor(timeSinceLastUpdate / 1000);
    
    if (globalTimerState.secondsUntilNextRun !== null && timeSinceLastUpdate < refreshInterval) {
      // Use cached data adjusted for elapsed time
      const adjustedSeconds = Math.max(0, globalTimerState.secondsUntilNextRun - elapsedSeconds);
      setSecondsUntilNextRun(adjustedSeconds);
      
      const adjustedTimers = {};
      for (const [id, seconds] of Object.entries(globalTimerState.integrationTimers)) {
        adjustedTimers[id] = Math.max(0, seconds - elapsedSeconds);
      }
      setIntegrationTimers(adjustedTimers);
      
      // Update global state with adjusted values
      globalTimerState.secondsUntilNextRun = adjustedSeconds;
      globalTimerState.integrationTimers = adjustedTimers;
      globalTimerState.lastUpdate = Date.now();
    } else {
      // Fetch fresh data
      fetchMonitoringStatus();
    }
    
    // Countdown timer runs every second for smooth UI
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

    // Fetch from backend at configured interval
    const fetchTimer = setInterval(fetchMonitoringStatus, refreshInterval);

    return () => {
      clearInterval(countdownTimer);
      clearInterval(fetchTimer);
    };
  }, [fetchMonitoringStatus, refreshInterval]);

  return {
    secondsUntilNextRun,
    integrationTimers,
    fetchMonitoringStatus
  };
};
