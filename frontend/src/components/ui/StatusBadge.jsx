import React from 'react';

const VARIANTS = {
  success: { badge: 'badge-success', dot: 'bg-emerald-500', pulse: true },
  danger: { badge: 'badge-danger', dot: 'bg-red-500', pulse: false },
  warning: { badge: 'badge-warning', dot: 'bg-amber-500', pulse: false },
  neutral: { badge: 'badge-neutral', dot: 'bg-surface-400', pulse: false },
  info: { badge: 'badge-info', dot: 'bg-primary-500', pulse: false },
};

const StatusBadge = ({ variant = 'neutral', label, pulse }) => {
  const v = VARIANTS[variant] || VARIANTS.neutral;
  const shouldPulse = pulse ?? v.pulse;
  return (
    <span className={v.badge}>
      <span className={`status-dot ${v.dot} ${shouldPulse ? 'animate-pulse' : ''}`} />
      {label}
    </span>
  );
};

export default StatusBadge;
