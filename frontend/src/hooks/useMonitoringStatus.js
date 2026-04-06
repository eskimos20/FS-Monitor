import { useState, useEffect } from 'react';
import api from '../api/axios';

export const useMonitoringStatus = () => {
  const [secondsUntilNextRun, setSecondsUntilNextRun] = useState(60);
  const [integrationTimers, setIntegrationTimers] = useState({});

  const fetchMonitoringStatus = async () => {
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
    } catch (error) {
      console.error('Failed to fetch monitoring status:', error);
    }
  };

  useEffect(() => {
    fetchMonitoringStatus();
    
    const timer = setInterval(() => {
      setSecondsUntilNextRun(prev => {
        if (prev <= 1) {
          setTimeout(() => {
            fetchMonitoringStatus();
          }, 2000);
          return 60;
        }
        return prev - 1;
      });
      
      setIntegrationTimers(prev => {
        const newTimers = {};
        for (const [id, seconds] of Object.entries(prev)) {
          newTimers[id] = Math.max(0, seconds - 1);
        }
        return newTimers;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  return {
    secondsUntilNextRun,
    integrationTimers,
    fetchMonitoringStatus
  };
};
