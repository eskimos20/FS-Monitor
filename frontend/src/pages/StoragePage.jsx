import React from 'react';
import PageHeader from '../components/ui/PageHeader';
import StorageTable from '../components/dashboard/StorageTable';

const StoragePage = () => (
  <div className="space-y-6 animate-fade-in">
    <PageHeader title="Storage" subtitle="Disk usage and largest directories" />
    <StorageTable />
  </div>
);

export default StoragePage;
