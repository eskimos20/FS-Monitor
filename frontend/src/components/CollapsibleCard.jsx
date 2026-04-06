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
    <div className="bg-white rounded-lg shadow">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between p-6 border-b border-gray-200 hover:bg-gray-50 transition-colors"
      >
        <div className="flex items-center space-x-2">
          {Icon && <Icon className="h-5 w-5 text-gray-600" />}
          <h2 className="text-lg font-semibold text-gray-800">{title}</h2>
        </div>
        {isOpen ? (
          <ChevronUp className="h-5 w-5 text-gray-400" />
        ) : (
          <ChevronDown className="h-5 w-5 text-gray-400" />
        )}
      </button>
      
      {isOpen && (
        <div>
          {children}
        </div>
      )}
    </div>
  );
};

export default CollapsibleCard;
