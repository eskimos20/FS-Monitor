import React, { useState, useEffect } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

const CollapsibleCard = ({ title, icon: Icon, children, defaultOpen = true, storageKey }) => {
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
        className="w-full flex items-center justify-between px-6 py-4 hover:bg-surface-50/60 transition-colors"
      >
        <div className="flex items-center space-x-2.5">
          {Icon && (
            <div className="flex items-center justify-center h-8 w-8 rounded-lg bg-surface-100">
              <Icon className="h-4 w-4 text-surface-600" />
            </div>
          )}
          <h2 className="text-base font-semibold text-surface-800">{title}</h2>
        </div>
        {isOpen ? (
          <ChevronUp className="h-4 w-4 text-surface-400" />
        ) : (
          <ChevronDown className="h-4 w-4 text-surface-400" />
        )}
      </button>
      {isOpen && <div className="border-t border-surface-100" />}
      
      {isOpen && (
        <div>
          {children}
        </div>
      )}
    </div>
  );
};

export default CollapsibleCard;
