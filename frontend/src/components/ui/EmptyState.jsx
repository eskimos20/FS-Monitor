import React from 'react';
import { Inbox } from 'lucide-react';

const EmptyState = ({ icon: Icon = Inbox, title, description, action, children }) => (
  <div className="flex flex-col items-center justify-center py-12 px-6 text-center">
    <div className="flex items-center justify-center h-12 w-12 rounded-2xl bg-surface-100 mb-4">
      <Icon className="h-6 w-6 text-surface-400" />
    </div>
    <h3 className="text-sm font-semibold text-surface-900">{title}</h3>
    {description && <p className="mt-1 text-sm text-surface-500 max-w-sm">{description}</p>}
    {(action || children) && <div className="mt-5">{action || children}</div>}
  </div>
);

export default EmptyState;
