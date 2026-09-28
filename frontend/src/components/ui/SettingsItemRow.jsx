import React from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import IconButton from './IconButton';

const SettingsItemRow = ({ title, subtitle, meta, onEdit, onDelete }) => (
  <div className="flex items-center justify-between gap-3 px-4 py-3 bg-surface-50/70 border border-surface-200 rounded-xl hover:border-surface-300 hover:bg-surface-50 transition-colors">
    <div className="flex-1 min-w-0">
      <div className="flex items-center gap-2">
        <h3 className="text-sm font-semibold text-surface-900 truncate">{title}</h3>
        {meta}
      </div>
      {subtitle && <p className="text-xs text-surface-500 truncate mt-0.5">{subtitle}</p>}
    </div>
    <div className="flex items-center gap-1 flex-shrink-0">
      {onEdit && <IconButton icon={Pencil} onClick={onEdit} title="Edit" variant="primary" />}
      {onDelete && <IconButton icon={Trash2} onClick={onDelete} title="Delete" variant="danger" />}
    </div>
  </div>
);

export default SettingsItemRow;
