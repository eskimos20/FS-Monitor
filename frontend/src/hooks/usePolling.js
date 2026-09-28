import { useEffect } from 'react';

/**
 * Runs `callback` once on mount and then every `intervalMs` milliseconds.
 * A non-positive or missing interval disables polling.
 */
export const usePolling = (callback, intervalMs) => {
  useEffect(() => {
    callback();
    if (intervalMs && intervalMs > 0) {
      const interval = setInterval(callback, intervalMs);
      return () => clearInterval(interval);
    }
  }, [callback, intervalMs]);
};
