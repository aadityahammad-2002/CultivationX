import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ui/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { SetupPage } from './pages/auth/SetupPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { RisePage } from './pages/rise/RisePage';
import { MentorPage } from './pages/mentor/MentorPage';
import { NexusPage } from './pages/nexus/NexusPage';
import { DevDnaPage } from './pages/devdna/DevDnaPage';
import { SettingsPage } from './pages/settings/SettingsPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: '#16161f',
              color: '#f0effe',
              border: '1px solid #2a2a3a',
              borderRadius: 10,
              fontSize: 14,
            },
            success: { iconTheme: { primary: '#10b981', secondary: '#16161f' } },
            error:   { iconTheme: { primary: '#ef4444', secondary: '#16161f' } },
          }}
        />
        <Routes>
          {/* Public */}
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/setup"    element={<SetupPage />} />

          {/* Protected */}
          <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/rise"      element={<RisePage />} />
            <Route path="/mentor"    element={<MentorPage />} />
            <Route path="/nexus"     element={<NexusPage />} />
            <Route path="/devdna"    element={<DevDnaPage />} />
            <Route path="/settings"  element={<SettingsPage />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}