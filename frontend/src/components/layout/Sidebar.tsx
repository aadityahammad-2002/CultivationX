import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import styles from './Sidebar.module.css';

const NAV = [
  { to: '/dashboard', icon: '🏠', label: 'Dashboard' },
  { to: '/rise',      icon: '🚀', label: 'Rise' },
  { to: '/mentor',    icon: '🤖', label: 'Mentor' },
  { to: '/nexus',     icon: '🌐', label: 'Nexus' },
  { to: '/devdna',    icon: '🧬', label: 'Dev DNA' },
  { to: '/settings',  icon: '⚙️',  label: 'Settings' },
];

export function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'CX';

  return (
    <aside className={styles.sidebar}>
      {/* Logo */}
      <div className={styles.logo}>
        <span className={styles.logoIcon}>CX</span>
        <span className={styles.logoText}>CultivationX</span>
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        {NAV.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIcon}>{icon}</span>
            <span className={styles.navLabel}>{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User */}
      <div className={styles.userSection}>
        <div className={styles.userAvatar}>{initials}</div>
        <div className={styles.userInfo}>
          <div className={styles.userName}>{user?.name}</div>
          <div className={styles.userScore}>Dev DNA: {user?.devDnaScore ?? 0}</div>
        </div>
        <button className={styles.logoutBtn} onClick={handleLogout} title="Logout">
          🚪
        </button>
      </div>
    </aside>
  );
}