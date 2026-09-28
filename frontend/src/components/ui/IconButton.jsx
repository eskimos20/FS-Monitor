import React from 'react';

const VARIANTS = {
  default: 'text-surface-500 hover:text-surface-800 hover:bg-surface-100',
  primary: 'text-surface-500 hover:text-primary-600 hover:bg-primary-50',
  danger: 'text-surface-500 hover:text-red-600 hover:bg-red-50',
};

const IconButton = ({ icon: Icon, onClick, title, variant = 'default' }) => (
  <button
    type="button"
    onClick={onClick}
    title={title}
    className={`p-2 rounded-lg transition-colors ${VARIANTS[variant]}`}
  >
    <Icon className="h-4 w-4" />
  </button>
);

export default IconButton;
