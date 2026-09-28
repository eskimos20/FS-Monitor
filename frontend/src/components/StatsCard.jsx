import React from 'react';

const barColor = (pct) =>
  pct >= 90 ? 'bg-red-500' : pct >= 75 ? 'bg-amber-500' : 'bg-emerald-500';

const StatsCard = ({ icon: Icon, label, value, iconColor = 'text-primary-600', tint = 'bg-primary-50', percent }) => {
  return (
    <div className="card card-hover">
      <div className="flex items-center">
        <div className={`flex items-center justify-center h-11 w-11 rounded-xl ${tint} flex-shrink-0`}>
          <Icon className={`h-5 w-5 ${iconColor}`} />
        </div>
        <div className="ml-4 min-w-0 flex-1">
          <p className="text-xs font-medium uppercase tracking-wide text-surface-500 truncate">{label}</p>
          <p className="text-2xl font-semibold text-surface-900 tnum truncate">{value}</p>
        </div>
      </div>
      {typeof percent === 'number' && (
        <div className="mt-3 h-1.5 w-full rounded-full bg-surface-100 overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-500 ${barColor(percent)}`}
            style={{ width: `${Math.min(Math.max(percent, 0), 100)}%` }}
          />
        </div>
      )}
    </div>
  );
};

export default StatsCard;
