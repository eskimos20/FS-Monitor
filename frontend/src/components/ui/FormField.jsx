import React from 'react';

const FormField = ({ label, hint, error, required, children, htmlFor }) => (
  <div>
    {label && (
      <label htmlFor={htmlFor} className="input-label">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
    )}
    {children}
    {error
      ? <p className="mt-1.5 text-xs text-red-600">{error}</p>
      : hint && <p className="input-hint">{hint}</p>}
  </div>
);

export default FormField;
