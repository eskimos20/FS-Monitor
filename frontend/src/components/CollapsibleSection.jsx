import React, { useState, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';

const CollapsibleSection = ({ title, icon: Icon, children, defaultOpen = true, storageKey }) => {
  const getInitialState = () => {
    if (!storageKey) return defaultOpen;
    const saved = localStorage.getItem(`collapsible_${storageKey}`);
    return saved !== null ? saved === 'true' : defaultOpen;
  };

  const [isOpen, setIsOpen] = useState(getInitialState);

  useEffect(() => {
    if (storageKey) {
      localStorage.setItem(`collapsible_${storageKey}`, isOpen.toString());
    }
  }, [isOpen, storageKey]);

  return (
    <div className="card !p-0 overflow-hidden">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between px-5 py-4 hover:bg-surface-50 transition-colors"
      >
        <div className="flex items-center gap-3">
          {Icon && (
            <span className="h-8 w-8 rounded-lg bg-primary-50 text-primary-600 flex items-center justify-center">
              <Icon className="h-4 w-4" />
            </span>
          )}
          <h2 className="section-title">{title}</h2>
        </div>
        <ChevronDown className={`h-4 w-4 text-surface-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div className="px-5 pb-5 pt-4 border-t border-surface-100">
          {children}
        </div>
      )}
    </div>
  );
};

export default CollapsibleSection;
