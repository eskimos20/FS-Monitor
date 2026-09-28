import React from 'react';
import { Folder, Server, Mail, FileSearch, HardDrive, Settings, Trash, Loader2, CheckCircle2, AlertCircle } from 'lucide-react';
import { useSettings } from '../hooks/useSettings';
import CollapsibleSection from '../components/CollapsibleSection';
import PageHeader from '../components/ui/PageHeader';
import IntegrationSettings from '../components/settings/IntegrationSettings';
import MailConfigSettings from '../components/settings/MailConfigSettings';
import ServiceSettings from '../components/settings/ServiceSettings';
import LogConfigSettings from '../components/settings/LogConfigSettings';
import StorageConfigSettings from '../components/settings/StorageConfigSettings';
import DeleteServiceSettings from '../components/settings/DeleteServiceSettings';
import AppSettingsSection from '../components/settings/AppSettingsSection';

const SettingsPage = () => {
  const {
    integrations,
    fileTypes,
    mailConfig,
    services,
    logConfigs,
    storageConfigs,
    deleteServices,
    appSettings,
    loading,
    error,
    success,
    saveIntegration,
    deleteIntegration,
    saveMailConfig,
    saveService,
    deleteService,
    saveLogConfig,
    deleteLogConfig,
    saveStorageConfig,
    deleteStorageConfig,
    saveDeleteService,
    deleteDeleteService,
    saveAppSettings
  } = useSettings();

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Settings"
        subtitle="Configure monitoring integrations, notifications and application behaviour"
      />

      {success && (
        <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-xl text-sm">
          <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
          {success}
        </div>
      )}
      {error && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          {error}
        </div>
      )}

      <div className="space-y-4">
        <CollapsibleSection title="Email Configuration" icon={Mail} defaultOpen={false} storageKey="settings_email">
          <MailConfigSettings mailConfig={mailConfig} onSave={saveMailConfig} />
        </CollapsibleSection>

        <CollapsibleSection title="File Integrations" icon={Folder} defaultOpen={true} storageKey="settings_integrations">
          <IntegrationSettings
            integrations={integrations}
            fileTypes={fileTypes}
            onSave={saveIntegration}
            onDelete={deleteIntegration}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Service Monitoring" icon={Server} defaultOpen={true} storageKey="settings_services">
          <ServiceSettings services={services} onSave={saveService} onDelete={deleteService} />
        </CollapsibleSection>

        <CollapsibleSection title="Storage Monitoring" icon={HardDrive} defaultOpen={false} storageKey="settings_storage">
          <StorageConfigSettings
            storageConfigs={storageConfigs}
            onSave={saveStorageConfig}
            onDelete={deleteStorageConfig}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Delete Services" icon={Trash} defaultOpen={false} storageKey="settings_delete">
          <DeleteServiceSettings
            deleteServices={deleteServices}
            onSave={saveDeleteService}
            onDelete={deleteDeleteService}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Log Monitoring" icon={FileSearch} defaultOpen={false} storageKey="settings_logs">
          <LogConfigSettings
            logConfigs={logConfigs}
            onSave={saveLogConfig}
            onDelete={deleteLogConfig}
          />
        </CollapsibleSection>

        <CollapsibleSection title="Application Settings" icon={Settings} defaultOpen={true} storageKey="settings_application">
          <AppSettingsSection appSettings={appSettings} onSave={saveAppSettings} />
        </CollapsibleSection>
      </div>
    </div>
  );
};

export default SettingsPage;
