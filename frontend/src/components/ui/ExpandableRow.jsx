import React from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';

/**
 * Expandable list-row card used across the dashboard.
 * header: main line content, aside: right-side content, children: expanded details.
 */
const ExpandableRow = ({ expanded, onToggle, title, subtitle, aside, children }) => (
  <div className="bg-white border border-surface-200 rounded-xl shadow-card overflow-hidden transition-shadow hover:shadow-card-hover">
    <div
      className="flex items-center justify-between gap-4 px-4 py-3.5 cursor-pointer select-none"
      onClick={onToggle}
      role="button"
      aria-expanded={expanded}
    >
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <span className="flex items-center justify-center h-6 w-6 rounded-md text-surface-400 bg-surface-100 flex-shrink-0">
          {expanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
        </span>
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-semibold text-surface-900 truncate">{title}</h4>
          {subtitle && <p className="text-xs text-surface-500 truncate mt-0.5">{subtitle}</p>}
        </div>
      </div>
      <div className="flex items-center gap-4 flex-shrink-0" onClick={(e) => e.stopPropagation()}>
        {aside}
      </div>
    </div>
    {expanded && (
      <div className="px-4 pb-4 pt-3 border-t border-surface-100 animate-fade-in">
        {children}
      </div>
    )}
  </div>
);

export default ExpandableRow;
