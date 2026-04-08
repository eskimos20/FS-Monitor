export const formatTimeAgo = (minutes) => {
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${Math.round(minutes)} min ago`;
  
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = Math.round(minutes % 60);
  
  if (hours < 24) {
    if (remainingMinutes === 0) return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
    return `${hours}h ${remainingMinutes}m ago`;
  }
  
  const days = Math.floor(hours / 24);
  const remainingHours = hours % 24;
  
  if (days < 365) {
    if (remainingHours === 0 && remainingMinutes === 0) return `${days} day${days !== 1 ? 's' : ''} ago`;
    return `${days}d ${remainingHours}h ago`;
  }
  
  const years = Math.floor(days / 365);
  const remainingDays = days % 365;
  
  if (remainingDays === 0) return `${years} year${years !== 1 ? 's' : ''} ago`;
  return `${years}y ${remainingDays}d ago`;
};

export const getStatusColor = (isActive, lastFileFound, thresholdMinutes) => {
  if (!isActive) return 'status-inactive';
  if (!lastFileFound) return 'status-inactive';
  
  const now = new Date();
  const lastFileTime = new Date(lastFileFound);
  const minutesSinceLastFile = (now - lastFileTime) / (1000 * 60);
  
  return minutesSinceLastFile <= thresholdMinutes ? 'status-active' : 'status-inactive';
};

export const getStatusText = (isActive, lastFileFound, thresholdMinutes) => {
  if (!isActive) return 'Disabled';
  if (!lastFileFound) return 'No files found';
  
  const now = new Date();
  const lastFileTime = new Date(lastFileFound);
  const minutesSinceLastFile = (now - lastFileTime) / (1000 * 60);
  
  if (minutesSinceLastFile <= thresholdMinutes) {
    return 'Active';
  } else {
    return `Inactive (${formatTimeAgo(minutesSinceLastFile)})`;
  }
};

export const formatCheckInterval = (integration) => {
  const value = integration.checkIntervalValue || 5;
  const unit = integration.checkIntervalUnit || 'MINUTES';
  return `${value} ${unit.toLowerCase()}`;
};

export const formatIntegrationTimer = (integration, integrationTimers) => {
  if (!integration || !integration.isActive || !integration.monitoringEnabled) {
    return '--';
  }
  
  const totalSeconds = integrationTimers[integration.id];
  if (totalSeconds === null || totalSeconds === undefined) return 'Loading...';
  if (totalSeconds === 0) return '...';
  
  // Calculate time units
  const months = Math.floor(totalSeconds / (30 * 24 * 60 * 60));
  const days = Math.floor((totalSeconds % (30 * 24 * 60 * 60)) / (24 * 60 * 60));
  const hours = Math.floor((totalSeconds % (24 * 60 * 60)) / (60 * 60));
  const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);
  const seconds = totalSeconds % 60;
  
  // Build formatted string with largest non-zero units
  const parts = [];
  if (months > 0) parts.push(`${months}mo`);
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  if (seconds > 0 || parts.length === 0) parts.push(`${seconds}s`);
  
  // Return up to 2 most significant units
  return parts.slice(0, 2).join(' ');
};

export const formatDateTime = (dateString) => {
  if (!dateString) return 'Never';
  const date = new Date(dateString);
  return date.toLocaleString();
};
