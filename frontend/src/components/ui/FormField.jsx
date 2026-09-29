import React from 'react';

// Label + hint/error wrapper. Renders a styled <input> when no children are
// provided; pass children for custom controls (select, textarea, etc.).
const FormField = ({ label, hint, error, required, htmlFor, children, className, ...inputProps }) => (
  <div>
    {label && (
      <label htmlFor={htmlFor} className="input-label">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
    )}
    {children ?? (
      <input
        {...inputProps}
        id={htmlFor}
        required={required}
        className={`input-field${className ? ` ${className}` : ''}`}
      />
    )}
    {error
      ? <p className="mt-1.5 text-xs text-red-600">{error}</p>
      : hint && <p className="input-hint">{hint}</p>}
  </div>
);

export default FormField;
