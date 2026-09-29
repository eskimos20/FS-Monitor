import React, { useState, useEffect } from 'react';
import { Pencil, Save, X } from 'lucide-react';
import FormField from '../ui/FormField';
import Toggle from '../ui/Toggle';
import DetailItem from '../ui/DetailItem';

const MailConfigSettings = ({ mailConfig, onSave }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [useAuthentication, setUseAuthentication] = useState(false);
  const [formData, setFormData] = useState({
    host: '',
    port: 587,
    fromEmail: '',
    toEmail: '',
    username: '',
    password: ''
  });

  // The API never returns the password; keep it blank so an empty field
  // preserves the stored password on save.
  const toFormData = (config) => ({
    host: config?.host ?? '',
    port: config?.port ?? 587,
    fromEmail: config?.fromEmail ?? '',
    toEmail: config?.toEmail ?? '',
    username: config?.username ?? '',
    password: ''
  });

  useEffect(() => {
    if (mailConfig) {
      setFormData(toFormData(mailConfig));
      setUseAuthentication(!!mailConfig.username);
    }
  }, [mailConfig]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const dataToSave = {
      ...formData,
      username: useAuthentication ? formData.username : '',
      password: useAuthentication ? formData.password : ''
    };

    await onSave(dataToSave);
    setIsEditing(false);
  };

  const handleCancel = () => {
    if (mailConfig) {
      setFormData(toFormData(mailConfig));
      setUseAuthentication(!!mailConfig.username);
    }
    setIsEditing(false);
  };

  const update = (key, value) => setFormData({ ...formData, [key]: value });

  if (!isEditing && mailConfig) {
    return (
      <div className="space-y-4">
        <div className="flex justify-end">
          <button onClick={() => setIsEditing(true)} className="btn-secondary">
            <Pencil className="h-4 w-4 mr-1.5" />
            Edit Configuration
          </button>
        </div>

        <div className="bg-surface-50/70 border border-surface-200 rounded-xl p-5">
          <div className="grid grid-cols-2 gap-x-6 gap-y-4">
            <DetailItem label="SMTP Host">
              <span className="font-mono text-xs">{mailConfig.host}</span>
            </DetailItem>
            <DetailItem label="Port">
              <span className="tnum">{mailConfig.port}</span>
            </DetailItem>
            <DetailItem label="From Email">{mailConfig.fromEmail}</DetailItem>
            <DetailItem label="To Email">{mailConfig.toEmail}</DetailItem>
            {mailConfig.username && (
              <DetailItem label="Authentication">
                Enabled ({mailConfig.username})
              </DetailItem>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="grid grid-cols-2 gap-4">
        <FormField
          label="SMTP Host"
          value={formData.host}
          onChange={(e) => update('host', e.target.value)}
          placeholder="smtp.gmail.com"
          required
        />
        <FormField
          label="Port"
          type="number"
          value={formData.port}
          onChange={(e) => update('port', parseInt(e.target.value))}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <FormField
          label="From Email (Sender)"
          type="email"
          value={formData.fromEmail}
          onChange={(e) => update('fromEmail', e.target.value)}
          placeholder="noreply@company.com"
          required
        />
        <FormField
          label="To Email (Recipient)"
          type="email"
          value={formData.toEmail}
          onChange={(e) => update('toEmail', e.target.value)}
          placeholder="admin@company.com"
          required
        />
      </div>

      <div className="pt-4 border-t border-surface-200">
        <Toggle
          checked={useAuthentication}
          onChange={setUseAuthentication}
          label="Use SMTP Authentication"
        />
      </div>

      {useAuthentication && (
        <div className="grid grid-cols-2 gap-4">
          <FormField
            label="Username"
            value={formData.username}
            onChange={(e) => update('username', e.target.value)}
            placeholder="your-email@gmail.com"
          />
          <FormField
            label="Password"
            type="password"
            value={formData.password}
            onChange={(e) => update('password', e.target.value)}
            placeholder="Your app password"
          />
        </div>
      )}

      <div className="flex justify-end gap-2 pt-4 border-t border-surface-200">
        {mailConfig && (
          <button type="button" onClick={handleCancel} className="btn-secondary">
            <X className="h-4 w-4 mr-1.5" />
            Cancel
          </button>
        )}
        <button type="submit" className="btn-primary">
          <Save className="h-4 w-4 mr-1.5" />
          Save Configuration
        </button>
      </div>
    </form>
  );
};

export default MailConfigSettings;
