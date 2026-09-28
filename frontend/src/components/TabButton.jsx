import React from 'react';

const TabButton = ({ active, onClick, icon: Icon, label }) => {
  return (
    <button
      onClick={onClick}
      className={`${
        active
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-surface-500 hover:text-surface-700 hover:border-surface-300'
      } whitespace-nowrap py-3.5 px-1 border-b-2 font-medium text-sm flex items-center transition-colors`}
    >
      <Icon className="h-4 w-4 mr-2" />
      {label}
    </button>
  );
};

export default TabButton;
