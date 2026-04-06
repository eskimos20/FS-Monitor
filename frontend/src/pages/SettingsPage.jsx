import React from 'react';
import { Folder, Server, Mail, FileSearch } from 'lucide-react';
import { useSettings } from '../hooks/useSettings';
import CollapsibleSection from '../components/CollapsibleSection';
import IntegrationSettings from '../components/settings/IntegrationSettings';
import MailConfigSettings from '../components/settings/MailConfigSettings';
import ServiceSettings from '../components/settings/ServiceSettings';
import LogConfigSettings from '../components/settings/LogConfigSettings';

const SettingsPage = () => {
  const {
    integrations,
    fileTypes,
    mailConfig,
    services,
    logConfigs,
    loading,
    error,
    success,
    saveIntegration,
    deleteIntegration,
    saveMailConfig,
    saveService,
    deleteService,
    saveLogConfig,
    deleteLogConfig
  } = useSettings();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600">Configure your monitoring integrations and notifications</p>
      </div>

      {/* Success/Error Messages */}
      {success && (
        <div className="bg-green-50 border border-green-200 text-green-600 px-4 py-3 rounded-md max-w-3xl">
          {success}
        </div>
      )}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md max-w-3xl">
          {error}
        </div>
      )}

      {/* Settings Sections - Centrerad layout */}
      <div className="max-w-4xl space-y-6">
        <CollapsibleSection title="Email Configuration" icon={Mail} defaultOpen={false}>
          <MailConfigSettings
            mailConfig={mailConfig}
            onSave={saveMailConfig}
          />
        </CollapsibleSection>

        <CollapsibleSection title="File Integrations" icon={Folder} defaultOpen={true}>
          <IntegrationSettings
            integrations={integrations}
            fileTypes={fileTypes}
            onSave={saveIntegration}
            onDelete={deleteIntegration}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Service Monitoring" icon={Server} defaultOpen={true}>
          <ServiceSettings
            services={services}
            onSave={saveService}
            onDelete={deleteService}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Log Monitoring" icon={FileSearch} defaultOpen={false}>
          <LogConfigSettings
            logConfigs={logConfigs}
            onSave={saveLogConfig}
            onDelete={deleteLogConfig}
          />
        </CollapsibleSection>
      </div>
    </div>
  );
};

export default SettingsPage;
