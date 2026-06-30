import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

export function AppLayout() {
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <Sidebar />
      <main style={{
        marginLeft: 'var(--sidebar-width)',
        flex: 1,
        overflowY: 'auto',
        background: 'var(--bg-primary)',
        padding: '32px',
      }}>
        <Outlet />
      </main>
    </div>
  );
}