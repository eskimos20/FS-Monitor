import React from 'react';

const PageHeader = ({ title, subtitle, children }) => (
  <div className="bg-gradient-to-r from-primary-50 to-blue-50 rounded-xl shadow-sm border border-surface-200 p-3 sm:p-6">
    <div className="flex items-start justify-between gap-4 flex-wrap">
      <div className="min-w-0">
        <h1 className="text-2xl sm:text-3xl font-bold text-surface-900">{title}</h1>
        {subtitle && <p className="text-surface-600 mt-1">{subtitle}</p>}
      </div>
      {children && <div className="flex items-center gap-3 flex-shrink-0 self-center">{children}</div>}
    </div>
  </div>
);

export default PageHeader;
