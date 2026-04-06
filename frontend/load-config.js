import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// Get __dirname equivalent in ES modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load config.json
function loadConfig() {
  try {
    const configPath = path.join(__dirname, '..', 'config.json');
    if (fs.existsSync(configPath)) {
      const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      const profile = process.env.NODE_ENV || 'development';
      return config[profile] || config.development;
    }
  } catch (error) {
    console.warn('Could not load config.json, using defaults:', error.message);
  }
  
  // Default values
  return {
    frontend: { port: 3000, host: 'localhost' },
    backend: { port: 8080, host: 'localhost' }
  };
}

// Get configuration
const config = loadConfig();

// Export configuration as ES module
export default {
  frontendPort: config.frontend.port,
  frontendHost: config.frontend.host,
  backendPort: config.backend.port,
  backendHost: config.backend.host
};
