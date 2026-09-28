import React from 'react';

const DetailItem = ({ label, children }) => (
  <div className="min-w-0">
    <p className="text-[11px] font-medium uppercase tracking-wide text-surface-400 mb-1">{label}</p>
    <div className="text-sm text-surface-800 flex items-center min-w-0">{children}</div>
  </div>
);

export default DetailItem;
