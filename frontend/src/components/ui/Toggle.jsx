import React from 'react';

const Toggle = ({ checked, onChange, disabled, label }) => (
  <label
    className={`inline-flex items-center gap-2.5 ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
    onClick={(e) => e.stopPropagation()}
  >
    <span className="relative inline-flex items-center">
      <input
        type="checkbox"
        className="sr-only peer"
        checked={!!checked}
        disabled={disabled}
        onChange={(e) => onChange?.(e.target.checked)}
      />
      <span className="block w-10 h-[22px] bg-surface-300 peer-focus-visible:ring-2 peer-focus-visible:ring-primary-500 peer-focus-visible:ring-offset-1
        rounded-full peer transition-colors
        peer-checked:bg-primary-600
        after:content-[''] after:absolute after:top-[3px] after:left-[3px]
        after:bg-white after:rounded-full after:h-4 after:w-4 after:shadow-sm after:transition-transform
        peer-checked:after:translate-x-[18px]" />
    </span>
    {label && <span className="text-sm font-medium text-surface-700">{label}</span>}
  </label>
);

export default Toggle;
