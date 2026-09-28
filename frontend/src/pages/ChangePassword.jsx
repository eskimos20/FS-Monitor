import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Eye, EyeOff, KeyRound, CheckCircle2, AlertCircle } from 'lucide-react';

const PasswordInput = ({ id, label, value, onChange, show, onToggle, placeholder }) => (
  <div>
    <label htmlFor={id} className="input-label">{label}</label>
    <div className="relative">
      <input
        id={id}
        name={id}
        type={show ? 'text' : 'password'}
        required
        className="input-field pr-10"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
      />
      <button
        type="button"
        className="absolute inset-y-0 right-0 pr-3 flex items-center"
        onClick={onToggle}
        tabIndex={-1}
      >
        {show
          ? <EyeOff className="h-4 w-4 text-surface-400 hover:text-surface-600" />
          : <Eye className="h-4 w-4 text-surface-400 hover:text-surface-600" />}
      </button>
    </div>
  </div>
);

const ChangePassword = () => {
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [showPasswords, setShowPasswords] = useState({ current: false, new: false, confirm: false });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const { user, changePassword } = useAuth();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const togglePasswordVisibility = (field) => {
    setShowPasswords({ ...showPasswords, [field]: !showPasswords[field] });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password and confirm password do not match.');
      setLoading(false);
      return;
    }

    try {
      await changePassword(formData.currentPassword, formData.newPassword);
      setSuccess('Password changed successfully! You will be redirected to login...');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-50 px-4">
      <div className="relative max-w-[400px] w-full">
        <div className="text-center mb-7">
          <div className="inline-flex items-center justify-center h-14 w-14 rounded-2xl bg-primary-600 shadow-pop mb-4">
            <KeyRound className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-surface-900">Change Password</h1>
          <p className="mt-1 text-sm text-surface-500">
            {user?.mustChangePassword
              ? 'You must change your password before continuing'
              : 'Update your account password'}
          </p>
        </div>

        <div className="bg-white rounded-2xl border border-surface-200 shadow-pop p-8">
          <form className="space-y-5" onSubmit={handleSubmit}>
            <PasswordInput
              id="currentPassword"
              label="Current Password"
              value={formData.currentPassword}
              onChange={handleChange}
              show={showPasswords.current}
              onToggle={() => togglePasswordVisibility('current')}
              placeholder="Enter current password"
            />
            <PasswordInput
              id="newPassword"
              label="New Password"
              value={formData.newPassword}
              onChange={handleChange}
              show={showPasswords.new}
              onToggle={() => togglePasswordVisibility('new')}
              placeholder="Enter new password"
            />
            <p className="text-xs text-surface-500 -mt-1">
              At least 8 characters with both letters and numbers.
            </p>
            <PasswordInput
              id="confirmPassword"
              label="Confirm New Password"
              value={formData.confirmPassword}
              onChange={handleChange}
              show={showPasswords.confirm}
              onToggle={() => togglePasswordVisibility('confirm')}
              placeholder="Confirm new password"
            />

            {error && (
              <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 px-3.5 py-2.5 rounded-lg text-sm">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                {error}
              </div>
            )}
            {success && (
              <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 text-emerald-700 px-3.5 py-2.5 rounded-lg text-sm">
                <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
                {success}
              </div>
            )}

            <button type="submit" disabled={loading} className="btn-primary w-full py-2.5">
              {loading
                ? <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                : 'Change Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChangePassword;
