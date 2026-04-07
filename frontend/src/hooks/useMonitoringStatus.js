import { useState, useEffect, useCallback, useRef } from 'react';
import api from '../api/axios';

export const useMonitoringStatus = (refreshInterval = 5000) => {
  const [secondsUntilNextRun, setSecondsUntilNextRun] = useState(60);
  const [integrationTimers, setIntegrationTimers] = useState({});
  const lastFetchRef = useRef(0);

  const fetchMonitoringStatus = useCallback(async () => {
    try {
      const response = await api.get('/monitoring/status');
      setSecondsUntilNextRun(response.data.secondsUntilNextRun);
      
      const backendTimers = response.data.integrationTimers || {};
      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, timerData] of Object.entries(backendTimers)) {
          newTimers[id] = timerData.secondsUntilNextRun;
        }
        return newTimers;
      });
      lastFetchRef.current = Date.now();
    } catch (error) {
      console.error('Failed to fetch monitoring status:', error);
    }
  }, []);

  useEffect(() => {
    fetchMonitoringStatus();
    
    // Countdown timer runs every second for smooth UI
    const countdownTimer = setInterval(() => {
      setSecondsUntilNextRun(prev => Math.max(0, prev - 1));
      
      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, seconds] of Object.entries(prev)) {
          newTimers[id] = Math.max(0, seconds - 1);
        }
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
