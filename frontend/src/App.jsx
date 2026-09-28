import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';

// Route-level code splitting: each page loads on demand
const Login = lazy(() => import('./pages/Login'));
const ChangePassword = lazy(() => import('./pages/ChangePassword'));
const Dashboard = lazy(() => import('./pages/DashboardPage'));
const Settings = lazy(() => import('./pages/SettingsPage'));
const LogConfigsPage = lazy(() => import('./pages/settings/LogConfigsPage'));

const FullScreenLoader = () => (
  <div className="min-h-screen flex items-center justify-center bg-surface-50">
    <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
  </div>
);

const RouteLoader = () => (
  <div className="flex items-center justify-center py-24">
    <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
  </div>
);

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.mustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  return children;
};

const AppRoutes = () => {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }

  return (
    <Suspense fallback={<FullScreenLoader />}>
      <Routes>
        <Route path="/login" element={
          !user ? <Login /> :
          user.mustChangePassword ? <Navigate to="/change-password" replace /> :
          <Navigate to="/dashboard" replace />
        } />
        <Route path="/change-password" element={
          !user ? <Navigate to="/login" replace /> :
          <ChangePassword />
        } />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={
            <Suspense fallback={<RouteLoader />}><Dashboard /></Suspense>
          } />
          <Route path="settings" element={
            <Suspense fallback={<RouteLoader />}><Settings /></Suspense>
          } />
          <Route path="settings/log-configs" element={
            <Suspense fallback={<RouteLoader />}><LogConfigsPage /></Suspense>
          } />
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
