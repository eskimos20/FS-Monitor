import React from 'react';

const TabButton = ({ active, onClick, icon: Icon, label }) => {
  return (
    <button
      onClick={onClick}
      className={`${
        active
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
      } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center`}
    >
      <Icon className="h-5 w-5 mr-2" />
      {label}
    </button>
  );
};

export default TabButton;
