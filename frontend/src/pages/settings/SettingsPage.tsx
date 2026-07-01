import { useState } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { nexusApi } from '../../api/nexus';
import { useAuth } from '../../context/AuthContext';
import type { Goal, ExperienceLevel } from '../../types';

const TABS = ['Profile', 'Security', 'Connections', 'About'];

const GOALS: { value: Goal; label: string }[] = [
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'SDE_1', label: 'SDE-1' },
  { value: 'FAANG', label: 'FAANG' },
  { value: 'PRODUCT_COMPANY', label: 'Product Company' },
  { value: 'SERVICE_COMPANY', label: 'Service Company' },
];

const LEVELS: { value: ExperienceLevel; label: string }[] = [
  { value: 'FRESHER', label: 'Fresher' },
  { value: 'JUNIOR', label: 'Junior' },
  { value: 'MID', label: 'Mid-level' },
  { value: 'SENIOR', label: 'Senior' },
];

export function SettingsPage() {
  const { user, updateUser, logout } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState(0);
  const [saving, setSaving] = useState(false);
  const [disconnecting, setDisconnecting] = useState({ github: false, leetcode: false });

  // GitHub PAT states
  const [githubToken, setGithubToken] = useState('');
  const [connectingGithub, setConnectingGithub] = useState(false);

  const [profile, setProfile] = useState({
    name: user?.name || '',
    bio: user?.bio || '',
    goal: user?.goal || 'SDE_1' as Goal,
    experienceLevel: user?.experienceLevel || 'FRESHER' as ExperienceLevel,
    targetCompany: user?.targetCompany || '',
    currentRole: user?.currentRole || '',
    yearsOfExperience: user?.yearsOfExperience || 0,
  });

  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwErrors, setPwErrors] = useState<Record<string, string>>({});

  const saveProfile = async () => {
    setSaving(true);
    try {
      const res = await authApi.updateProfile(profile);
      if (res) { updateUser(res); toast.success('Profile updated!'); }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to update profile');
    } finally { setSaving(false); }
  };

  const changePassword = async () => {
    const errs: Record<string, string> = {};
    if (!passwords.currentPassword) errs.currentPassword = 'Required';
    if (!passwords.newPassword || passwords.newPassword.length < 8) errs.newPassword = 'Min 8 characters';
    if (passwords.newPassword !== passwords.confirmPassword) errs.confirmPassword = 'Passwords do not match';
    setPwErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSaving(true);
    try {
      await authApi.changePassword({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      toast.success('Password changed successfully!');
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to change password');
    } finally { setSaving(false); }
  };

  // GitHub connect with PAT
  const connectGitHubWithToken = async () => {
    if (!githubToken.trim()) {
      toast.error('Please enter GitHub token');
      return;
    }
    setConnectingGithub(true);
    try {
      await nexusApi.connectGitHubWithToken(githubToken.trim());
      toast.success('GitHub connected!');
      setGithubToken('');
      const res = await authApi.getMe();
      if (res) updateUser(res);
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to connect GitHub');
    } finally {
      setConnectingGithub(false);
    }
  };

  const disconnectGitHub = async () => {
    setDisconnecting(d => ({ ...d, github: true }));
    try {
      await nexusApi.disconnectGitHub();
      toast.success('GitHub disconnected');
      const res = await authApi.getMe();
      if (res) updateUser(res);
    } catch { toast.error('Failed to disconnect'); }
    finally { setDisconnecting(d => ({ ...d, github: false })); }
  };

  const disconnectLeetCode = async () => {
    setDisconnecting(d => ({ ...d, leetcode: true }));
    try {
      await nexusApi.disconnectLeetCode();
      toast.success('LeetCode disconnected');
      const res = await authApi.getMe();
      if (res) updateUser(res);
    } catch { toast.error('Failed to disconnect'); }
    finally { setDisconnecting(d => ({ ...d, leetcode: false })); }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 24 }}>
        <h1>⚙️ Settings</h1>
        <p>Manage your profile and account settings</p>
      </div>

      <div className="tabs">
        {TABS.map((t, i) => (
          <button key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</button>
        ))}
      </div>

      {/* PROFILE */}
      {tab === 0 && (
        <div className="fade-in" style={{ maxWidth: 600 }}>
          <div className="card">
            <h3 className="card-title" style={{ marginBottom: 20 }}>Profile Information</h3>

            <div className="input-group">
              <label className="input-label">Full Name</label>
              <input className="input" value={profile.name} onChange={e => setProfile(p => ({ ...p, name: e.target.value }))} />
            </div>

            <div className="input-group">
              <label className="input-label">Bio</label>
              <textarea className="input" rows={3} placeholder="Tell us about yourself..." value={profile.bio} onChange={e => setProfile(p => ({ ...p, bio: e.target.value }))} />
            </div>

            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Primary Goal</label>
                <select className="input" value={profile.goal} onChange={e => setProfile(p => ({ ...p, goal: e.target.value as Goal }))}>
                  {GOALS.map(g => <option key={g.value} value={g.value}>{g.label}</option>)}
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">Experience Level</label>
                <select className="input" value={profile.experienceLevel} onChange={e => setProfile(p => ({ ...p, experienceLevel: e.target.value as ExperienceLevel }))}>
                  {LEVELS.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
                </select>
              </div>
            </div>

            <div className="grid-2">
              <div className="input-group">
                <label className="input-label">Target Company</label>
                <input className="input" placeholder="Google, Amazon, Flipkart..." value={profile.targetCompany} onChange={e => setProfile(p => ({ ...p, targetCompany: e.target.value }))} />
              </div>
              <div className="input-group">
                <label className="input-label">Current Role</label>
                <input className="input" placeholder="Student, Junior Dev..." value={profile.currentRole} onChange={e => setProfile(p => ({ ...p, currentRole: e.target.value }))} />
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Years of Experience</label>
              <input className="input" type="number" min={0} max={30} value={profile.yearsOfExperience} onChange={e => setProfile(p => ({ ...p, yearsOfExperience: parseInt(e.target.value) || 0 }))} />
            </div>

            <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
              <button className="btn btn-primary" onClick={saveProfile} disabled={saving}>
                {saving ? <><span className="spinner" /> Saving...</> : 'Save Changes'}
              </button>
              <button className="btn btn-secondary" onClick={() => setProfile({ name: user?.name || '', bio: user?.bio || '', goal: user?.goal || 'SDE_1', experienceLevel: user?.experienceLevel || 'FRESHER', targetCompany: user?.targetCompany || '', currentRole: user?.currentRole || '', yearsOfExperience: user?.yearsOfExperience || 0 })}>
                Reset
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SECURITY */}
      {tab === 1 && (
        <div className="fade-in" style={{ maxWidth: 480 }}>
          <div className="card">
            <h3 className="card-title" style={{ marginBottom: 20 }}>Change Password</h3>

            <div className="input-group">
              <label className="input-label">Current Password</label>
              <input className={`input ${pwErrors.currentPassword ? 'input-error' : ''}`} type="password" value={passwords.currentPassword} onChange={e => setPasswords(p => ({ ...p, currentPassword: e.target.value }))} />
              {pwErrors.currentPassword && <span className="error-msg">{pwErrors.currentPassword}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">New Password</label>
              <input className={`input ${pwErrors.newPassword ? 'input-error' : ''}`} type="password" placeholder="At least 8 characters" value={passwords.newPassword} onChange={e => setPasswords(p => ({ ...p, newPassword: e.target.value }))} />
              {pwErrors.newPassword && <span className="error-msg">{pwErrors.newPassword}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Confirm New Password</label>
              <input className={`input ${pwErrors.confirmPassword ? 'input-error' : ''}`} type="password" value={passwords.confirmPassword} onChange={e => setPasswords(p => ({ ...p, confirmPassword: e.target.value }))} />
              {pwErrors.confirmPassword && <span className="error-msg">{pwErrors.confirmPassword}</span>}
            </div>

            <button className="btn btn-primary" onClick={changePassword} disabled={saving}>
              {saving ? <><span className="spinner" /> Changing...</> : 'Change Password'}
            </button>
          </div>

          <div className="card" style={{ marginTop: 16, borderColor: 'rgba(239,68,68,0.3)' }}>
            <h3 className="card-title" style={{ marginBottom: 12, color: 'var(--error)' }}>Danger Zone</h3>
            <p style={{ fontSize: 13, marginBottom: 16 }}>Sign out of your CultivationX account</p>
            <button className="btn btn-danger" onClick={handleLogout}>Sign Out</button>
          </div>
        </div>
      )}

      {/* CONNECTIONS */}
      {tab === 2 && (
        <div className="fade-in" style={{ maxWidth: 560 }}>
          <div className="card">
            <h3 className="card-title" style={{ marginBottom: 20 }}>Connected Platforms</h3>

            {/* GitHub with PAT */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '16px 0', borderBottom: '1px solid var(--border)' }}>
              <span style={{ fontSize: 28 }}>⚙️</span>
              <div style={{ flex: 1 }}>
                <h4 style={{ color: 'var(--text-primary)', marginBottom: 4 }}>GitHub</h4>
                {user?.githubConnected
                  ? <p style={{ margin: 0, fontSize: 13 }}>Connected as @{user.githubUsername}</p>
                  : <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted)' }}>Not connected</p>}
              </div>
              {user?.githubConnected ? (
                <button className="btn btn-danger btn-sm" onClick={disconnectGitHub} disabled={disconnecting.github}>
                  {disconnecting.github ? <span className="spinner" /> : 'Disconnect'}
                </button>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, minWidth: 250 }}>
                  <input
                    type="password"
                    className="input input-sm"
                    placeholder="GitHub Personal Access Token"
                    value={githubToken}
                    onChange={e => setGithubToken(e.target.value)}
                    style={{ fontSize: 12 }}
                  />
                  <p style={{ margin: 0, fontSize: 11, color: 'var(--text-muted)' }}>
                    Create at <a href="https://github.com/settings/tokens" target="_blank" rel="noopener">github.com/settings/tokens</a>
                    <br/>Scopes: <code>repo</code>, <code>read:user</code>
                  </p>
                  <button 
                    className="btn btn-primary btn-sm" 
                    onClick={connectGitHubWithToken}
                    disabled={connectingGithub}
                  >
                    {connectingGithub ? <><span className="spinner" /> Connecting...</> : 'Connect'}
                  </button>
                </div>
              )}
            </div>

            {/* LeetCode */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '16px 0' }}>
              <span style={{ fontSize: 28 }}>💻</span>
              <div style={{ flex: 1 }}>
                <h4 style={{ color: 'var(--text-primary)', marginBottom: 4 }}>LeetCode</h4>
                {user?.leetcodeConnected
                  ? <p style={{ margin: 0, fontSize: 13 }}>Connected as @{user.leetcodeUsername}</p>
                  : <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted)' }}>Not connected</p>}
              </div>
              {user?.leetcodeConnected ? (
                <button className="btn btn-danger btn-sm" onClick={disconnectLeetCode} disabled={disconnecting.leetcode}>
                  {disconnecting.leetcode ? <span className="spinner" /> : 'Disconnect'}
                </button>
              ) : (
                <button className="btn btn-primary btn-sm" onClick={() => navigate('/nexus')}>Connect</button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ABOUT */}
      {tab === 3 && (
        <div className="fade-in" style={{ maxWidth: 480 }}>
          <div className="card" style={{ textAlign: 'center' }}>
            <div style={{ width: 64, height: 64, background: 'linear-gradient(135deg, var(--purple-primary), var(--purple-light))', borderRadius: 18, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, fontWeight: 800, color: '#fff', margin: '0 auto 16px' }}>
              CX
            </div>
            <h2 style={{ marginBottom: 8 }}>CultivationX</h2>
            <p style={{ marginBottom: 4 }}>AI-Powered Developer Growth Platform</p>
            <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 24 }}>Version 1.0.0</p>

            <div style={{ textAlign: 'left', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                ['🚀 Rise', 'Resume analysis & ATS scoring'],
                ['🤖 Mentor', 'AI coding coach & code review'],
                ['🌐 Nexus', 'GitHub, LeetCode & LeetGit+ integration'],
                ['🧬 Dev DNA', 'AI developer intelligence engine'],
              ].map(([name, desc]) => (
                <div key={name} style={{ display: 'flex', gap: 12, padding: '10px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                  <span style={{ color: 'var(--text-primary)', fontWeight: 600, minWidth: 100 }}>{name}</span>
                  <span style={{ color: 'var(--text-secondary)' }}>{desc}</span>
                </div>
              ))}
            </div>

            <p style={{ marginTop: 24, fontSize: 12, color: 'var(--text-muted)' }}>
              Built with Spring Boot 3.5 + React 18 + Spring AI (Groq)
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
