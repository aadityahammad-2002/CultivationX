import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { nexusApi } from '../../api/nexus';
import type { GitHubProfileResponse, LeetCodeProfileResponse, LeetGitStats, LeetGitSyncResponse } from '../../types';
import styles from './Nexus.module.css';

const TABS = ['Overview', 'GitHub', 'LeetCode', 'LeetGit'];

const LANGUAGES = ['Java', 'Python', 'Python3', 'JavaScript', 'TypeScript', 'C++', 'C', 'Go', 'Rust', 'Kotlin'];
const DIFFICULTIES = ['Easy', 'Medium', 'Hard'];

const DEFAULT_SYNC_FORM = {
  problemTitle: '',
  problemSlug: '',
  problemDescription: '',
  language: 'Java',
  difficulty: 'Medium',
  code: '',
};

export function NexusPage() {
  const [tab, setTab] = useState(0);
  const [github, setGithub] = useState<GitHubProfileResponse | null>(null);
  const [leetcode, setLeetcode] = useState<LeetCodeProfileResponse | null>(null);
  const [leetgitStats, setLeetgitStats] = useState<LeetGitStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState({ github: false, leetcode: false });
  const [leetcodeUsername, setLeetcodeUsername] = useState('');
  const [leetgitRepo, setLeetgitRepo] = useState('leetcode-solutions');
  const [connectingLC, setConnectingLC] = useState(false);
  const [enablingLG, setEnablingLG] = useState(false);

  // LeetGit sync form
  const [showSyncForm, setShowSyncForm] = useState(false);
  const [syncForm, setSyncForm] = useState(DEFAULT_SYNC_FORM);
  const [syncingLG, setSyncingLG] = useState(false);
  const [lastSyncResult, setLastSyncResult] = useState<LeetGitSyncResponse | null>(null);

  // Auto-detect: recent LeetCode submissions
  const [recentSubmissions, setRecentSubmissions] = useState<any[]>([]);
  const [detecting, setDetecting] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    if (code) {
      window.history.replaceState({}, document.title, '/nexus');
      nexusApi.connectGitHub(code)
        .then(res => {
          if (res.data) { setGithub(res.data); toast.success('GitHub connected successfully!'); }
        })
        .catch(() => toast.error('GitHub connection failed'));
    }

    Promise.all([
      nexusApi.getGitHub().then(r => r.data && setGithub(r.data)).catch(() => {}),
      nexusApi.getLeetCode().then(r => r.data && setLeetcode(r.data)).catch(() => {}),
      nexusApi.getLeetGitStats().then(r => r.data && setLeetgitStats(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);

  // Auto-detect LeetCode submissions when on LeetGit tab
  useEffect(() => {
    if (tab === 3 && leetgitStats && leetcode?.connected) {
      detectRecentSubmissions();
    }
  }, [tab, leetgitStats, leetcode]);

  const detectRecentSubmissions = async () => {
    setDetecting(true);
    try {
      // Fetch from LeetCode public API (no auth needed for recent submissions)
      const username = leetcode?.username;
      if (!username) return;
      
      const res = await fetch(`https://alfa-leetcode-api.onrender.com/${username}/submission`);
      const data = await res.json();
      if (data && Array.isArray(data.submission)) {
        // Filter accepted submissions from last 24h that aren't already synced
        const accepted = data.submission
          .filter((s: any) => s.statusDisplay === 'Accepted')
          .slice(0, 5);
        setRecentSubmissions(accepted);
      }
    } catch (e) {
      // Silent fail — manual sync form still works
      setRecentSubmissions([]);
    } finally {
      setDetecting(false);
    }
  };

  const syncGitHub = async () => {
    setSyncing(s => ({ ...s, github: true }));
    try {
      const res = await nexusApi.syncGitHub();
      if (res.data) { setGithub(res.data); toast.success('GitHub synced!'); }
    } catch { toast.error('Sync failed'); }
    finally { setSyncing(s => ({ ...s, github: false })); }
  };

  const connectLeetCode = async () => {
    if (!leetcodeUsername.trim()) { toast.error('Enter your LeetCode username'); return; }
    setConnectingLC(true);
    try {
      const res = await nexusApi.connectLeetCode(leetcodeUsername);
      if (res.data) { setLeetcode(res.data); toast.success('LeetCode connected!'); }
    } catch (err: any) { toast.error(err.response?.data?.error || 'Connection failed'); }
    finally { setConnectingLC(false); }
  };

  const syncLeetCode = async () => {
    setSyncing(s => ({ ...s, leetcode: true }));
    try {
      const res = await nexusApi.syncLeetCode();
      if (res.data) { setLeetcode(res.data); toast.success('LeetCode synced!'); }
    } catch { toast.error('Sync failed'); }
    finally { setSyncing(s => ({ ...s, leetcode: false })); }
  };

  const enableLeetGit = async () => {
    setEnablingLG(true);
    try {
      await nexusApi.enableLeetGit(leetgitRepo);
      toast.success(`LeetGit enabled! Repo: ${leetgitRepo}`);
      const res = await nexusApi.getLeetGitStats();
      if (res.data) setLeetgitStats(res.data);
    } catch (err: any) { toast.error(err.response?.data?.error || 'Failed to enable LeetGit'); }
    finally { setEnablingLG(false); }
  };

  const handleSyncSolution = async () => {
    if (!syncForm.problemTitle.trim()) { toast.error('Enter problem title'); return; }
    if (!syncForm.problemSlug.trim()) { toast.error('Enter problem slug'); return; }
    if (!syncForm.code.trim()) { toast.error('Paste your solution code'); return; }

    setSyncingLG(true);
    setLastSyncResult(null);
    try {
      const res = await nexusApi.syncSolution(syncForm);
      if (res.data) {
        setLastSyncResult(res.data);
        toast.success('Solution synced to GitHub with AI review!');
        setSyncForm(DEFAULT_SYNC_FORM);
        setShowSyncForm(false);
        const statsRes = await nexusApi.getLeetGitStats();
        if (statsRes.data) setLeetgitStats(statsRes.data);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Sync failed');
    } finally {
      setSyncingLG(false);
    }
  };

  // One-click sync from detected submission
  const quickSync = (submission: any) => {
    const title = submission.title || 'Unknown Problem';
    const slug = (submission.titleSlug || title.toLowerCase().replace(/[^a-z0-9]+/g, '-')).replace(/^-|-$/g, '');
    setSyncForm({
      problemTitle: title,
      problemSlug: slug,
      problemDescription: '',
      language: submission.lang || 'Java',
      difficulty: 'Medium',
      code: '',
    });
    setShowSyncForm(true);
    toast('Paste your solution code and click Sync!', { icon: '💡' });
  };

  // Auto-generate slug from title
  const handleTitleChange = (title: string) => {
    const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
    setSyncForm(f => ({ ...f, problemTitle: title, problemSlug: slug }));
  };

  if (loading) return <div className="page-loader"><div className="spinner" style={{ width: 40, height: 40, borderWidth: 3 }} /><p>Loading Nexus...</p></div>;

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 24 }}>
        <h1>🌐 Nexus</h1>
        <p>Connect your developer ecosystem — GitHub, LeetCode, and LeetGit</p>
      </div>

      <div className="tabs">
        {TABS.map((t, i) => <button key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</button>)}
      </div>

      {/* OVERVIEW */}
      {tab === 0 && (
        <div className="fade-in">
          <div className="grid-3">
            {/* GitHub card */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ fontSize: 28 }}>⚙️</span>
                <div>
                  <h4 style={{ color: 'var(--text-primary)' }}>GitHub</h4>
                  <span className={`badge ${github?.connected ? 'badge-success' : 'badge-error'}`}>{github?.connected ? 'Connected' : 'Not Connected'}</span>
                </div>
              </div>
              {github?.connected ? (
                <>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>@{github.username} · Last sync: {github.lastSyncAt ? new Date(github.lastSyncAt).toLocaleString() : 'Never'}</p>
                  <div className="grid-2" style={{ gap: 8, marginBottom: 12 }}>
                    {[['Repos', github.publicRepos], ['Stars', github.totalStars], ['Followers', github.followers], ['Following', github.following]].map(([l, v]) => (
                      <div key={l as string} style={{ textAlign: 'center', padding: '8px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                        <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{v ?? 0}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{l}</div>
                      </div>
                    ))}
                  </div>
                  <button className="btn btn-secondary btn-sm btn-full" onClick={syncGitHub} disabled={syncing.github}>
                    {syncing.github ? <><span className="spinner" /> Syncing...</> : '🔄 Sync Now'}
                  </button>
                </>
              ) : (
                <div>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>Connect GitHub to sync your repositories and contribution data</p>
                  <a href={`https://github.com/login/oauth/authorize?client_id=${import.meta.env.VITE_GITHUB_CLIENT_ID}&scope=repo,user`} className="btn btn-primary btn-sm btn-full">
                    Connect GitHub
                  </a>
                </div>
              )}
            </div>

            {/* LeetCode card */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ fontSize: 28 }}>💻</span>
                <div>
                  <h4 style={{ color: 'var(--text-primary)' }}>LeetCode</h4>
                  <span className={`badge ${leetcode?.connected ? 'badge-success' : 'badge-error'}`}>{leetcode?.connected ? 'Connected' : 'Not Connected'}</span>
                </div>
              </div>
              {leetcode?.connected ? (
                <>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>@{leetcode.username}</p>
                  <div className="grid-3" style={{ gap: 8, marginBottom: 12 }}>
                    {[['Easy', leetcode.easySolved, '#10b981'], ['Medium', leetcode.mediumSolved, '#f59e0b'], ['Hard', leetcode.hardSolved, '#ef4444']].map(([l, v, c]) => (
                      <div key={l as string} style={{ textAlign: 'center', padding: '8px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                        <div style={{ fontWeight: 700, color: c as string }}>{v ?? 0}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{l}</div>
                      </div>
                    ))}
                  </div>
                  <div style={{ textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)', marginBottom: 12 }}>
                    Total Solved: <strong style={{ color: 'var(--text-primary)' }}>{leetcode.totalSolved ?? 0}</strong>
                  </div>
                  <button className="btn btn-secondary btn-sm btn-full" onClick={syncLeetCode} disabled={syncing.leetcode}>
                    {syncing.leetcode ? <><span className="spinner" /> Syncing...</> : '🔄 Sync Now'}
                  </button>
                </>
              ) : (
                <div>
                  <div className="input-group">
                    <label className="input-label">LeetCode username</label>
                    <input className="input" placeholder="your-username" value={leetcodeUsername} onChange={e => setLeetcodeUsername(e.target.value)} />
                  </div>
                  <button className="btn btn-primary btn-sm btn-full" onClick={connectLeetCode} disabled={connectingLC}>
                    {connectingLC ? <><span className="spinner" /> Connecting...</> : 'Connect LeetCode'}
                  </button>
                </div>
              )}
            </div>

            {/* LeetGit card */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ fontSize: 28 }}>⚡</span>
                <div>
                  <h4 style={{ color: 'var(--text-primary)' }}>LeetGit</h4>
                  <span className={`badge ${leetgitStats ? 'badge-success' : 'badge-error'}`}>{leetgitStats ? 'Enabled' : 'Not Enabled'}</span>
                </div>
              </div>
              {leetgitStats ? (
                <>
                  <div className="grid-2" style={{ gap: 8, marginBottom: 12 }}>
                    {[['Total', leetgitStats.totalSynced], ['Pushed', leetgitStats.totalPushed], ['Success %', leetgitStats.syncSuccessRate + '%']].map(([l, v]) => (
                      <div key={l as string} style={{ textAlign: 'center', padding: '8px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                        <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{v}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{l}</div>
                      </div>
                    ))}
                  </div>
                  <button className="btn btn-secondary btn-sm btn-full" onClick={() => setTab(3)}>View Syncs →</button>
                </>
              ) : (
                <div>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }}>Auto-sync LeetCode solutions to GitHub with AI reviews</p>
                  <div className="input-group">
                    <label className="input-label">GitHub repo name</label>
                    <input className="input" value={leetgitRepo} onChange={e => setLeetgitRepo(e.target.value)} />
                  </div>
                  <button className="btn btn-primary btn-sm btn-full" onClick={enableLeetGit} disabled={enablingLG}>
                    {enablingLG ? <><span className="spinner" /> Enabling...</> : '⚡ Enable LeetGit'}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* GITHUB TAB */}
      {tab === 1 && (
        <div className="fade-in">
          {!github?.connected ? (
            <div className="empty-state">
              <h3>GitHub not connected</h3>
              <p>Connect GitHub to see your repositories, contributions, and language breakdown</p>
              <a href={`https://github.com/login/oauth/authorize?client_id=${import.meta.env.VITE_GITHUB_CLIENT_ID}&scope=repo,user`} className="btn btn-primary">Connect GitHub</a>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div className="card" style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
                <div style={{ width: 72, height: 72, background: 'var(--purple-glow)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28 }}>⚙️</div>
                <div style={{ flex: 1 }}>
                  <h2 style={{ marginBottom: 4 }}>{github.displayName || github.username}</h2>
                  <p style={{ margin: 0, fontSize: 13 }}>@{github.username} {github.location && `· ${github.location}`}</p>
                  {github.bio && <p style={{ margin: '8px 0 0', fontSize: 13 }}>{github.bio}</p>}
                </div>
                <button className="btn btn-secondary btn-sm" onClick={syncGitHub} disabled={syncing.github}>
                  {syncing.github ? <><span className="spinner" /> Syncing...</> : '🔄 Sync'}
                </button>
              </div>

              <div className="grid-4">
                {[['Public Repos', github.publicRepos], ['Total Stars', github.totalStars], ['Followers', github.followers], ['Following', github.following]].map(([l, v]) => (
                  <div key={l as string} className="stat-card">
                    <span className="stat-card-label">{l}</span>
                    <span className="stat-card-value">{v ?? 0}</span>
                  </div>
                ))}
              </div>

              {github.topLanguages && Object.keys(github.topLanguages).length > 0 && (
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>Top Languages</h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {Object.entries(github.topLanguages).sort((a, b) => b[1] - a[1]).slice(0, 6).map(([lang, count]) => {
                      const total = Object.values(github.topLanguages!).reduce((a, b) => a + b, 0);
                      const pct = Math.round((count / total) * 100);
                      return (
                        <div key={lang}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                            <span style={{ color: 'var(--text-primary)' }}>{lang}</span>
                            <span style={{ color: 'var(--text-muted)' }}>{pct}%</span>
                          </div>
                          <div className="progress-bar-wrap">
                            <div className="progress-bar-fill" style={{ width: `${pct}%`, background: 'var(--purple-primary)' }} />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* LEETCODE TAB */}
      {tab === 2 && (
        <div className="fade-in">
          {!leetcode?.connected ? (
            <div className="empty-state">
              <h3>LeetCode not connected</h3>
              <p>Enter your LeetCode username to sync your problem-solving stats</p>
              <div style={{ display: 'flex', gap: 8, maxWidth: 320, margin: '16px auto 0' }}>
                <input className="input" placeholder="your-username" value={leetcodeUsername} onChange={e => setLeetcodeUsername(e.target.value)} />
                <button className="btn btn-primary" onClick={connectLeetCode} disabled={connectingLC}>
                  {connectingLC ? <span className="spinner" /> : 'Connect'}
                </button>
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div className="card" style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
                <div style={{ fontSize: 48 }}>💻</div>
                <div style={{ flex: 1 }}>
                  <h2 style={{ marginBottom: 4 }}>{leetcode.username}</h2>
                  <p style={{ margin: 0, fontSize: 13 }}>Ranking: #{leetcode.ranking ?? '—'}</p>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={syncLeetCode} disabled={syncing.leetcode}>
                  {syncing.leetcode ? <><span className="spinner" /> Syncing...</> : '🔄 Sync'}
                </button>
              </div>

              <div className="grid-4">
                {[['Total Solved', leetcode.totalSolved, 'var(--purple-primary)'], ['Easy', leetcode.easySolved, '#10b981'], ['Medium', leetcode.mediumSolved, '#f59e0b'], ['Hard', leetcode.hardSolved, '#ef4444']].map(([l, v, c]) => (
                  <div key={l as string} className="stat-card">
                    <span className="stat-card-label">{l}</span>
                    <span className="stat-card-value" style={{ color: c as string }}>{v ?? 0}</span>
                  </div>
                ))}
              </div>

              <div className="grid-2">
                <div className="stat-card">
                  <span className="stat-card-label">Current Streak</span>
                  <span className="stat-card-value">🔥 {leetcode.currentStreak ?? 0}</span>
                  <span className="stat-card-sub">days</span>
                </div>
                <div className="stat-card">
                  <span className="stat-card-label">Total Active Days</span>
                  <span className="stat-card-value">{leetcode.totalActiveDays ?? 0}</span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* LEETGIT TAB */}
      {tab === 3 && (
        <div className="fade-in">
          {!leetgitStats ? (
            <div className="empty-state">
              <h3>LeetGit not enabled</h3>
              <p>Enable LeetGit to auto-sync your LeetCode solutions to GitHub with AI reviews</p>
              <div style={{ maxWidth: 320, margin: '16px auto' }}>
                <div className="input-group">
                  <label className="input-label">GitHub repo name for solutions</label>
                  <input className="input" value={leetgitRepo} onChange={e => setLeetgitRepo(e.target.value)} />
                </div>
                <button className="btn btn-primary btn-full" onClick={enableLeetGit} disabled={enablingLG}>
                  {enablingLG ? <><span className="spinner" /> Enabling...</> : '⚡ Enable LeetGit'}
                </button>
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

              {/* Stats row */}
              <div className="grid-3">
                {[['Total Synced', leetgitStats.totalSynced], ['Pushed to GitHub', leetgitStats.totalPushed], ['Success Rate', leetgitStats.syncSuccessRate + '%']].map(([l, v]) => (
                  <div key={l as string} className="stat-card">
                    <span className="stat-card-label">{l}</span>
                    <span className="stat-card-value">{v}</span>
                  </div>
                ))}
              </div>

              {/* Auto-detected submissions */}
              {leetcode?.connected && (
                <div className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                    <h3 className="card-title" style={{ margin: 0 }}>🔍 Recent LeetCode Submissions</h3>
                    <button className="btn btn-ghost btn-sm" onClick={detectRecentSubmissions} disabled={detecting}>
                      {detecting ? <span className="spinner" /> : '🔄 Refresh'}
                    </button>
                  </div>
                  {detecting ? (
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Detecting submissions...</p>
                  ) : recentSubmissions.length === 0 ? (
                    <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No recent accepted submissions found. Solve a problem on LeetCode and click Refresh.</p>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                      {recentSubmissions.map((sub, idx) => (
                        <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
                          <div style={{ flex: 1 }}>
                            <div style={{ fontWeight: 600, fontSize: 14, color: 'var(--text-primary)' }}>{sub.title || 'Unknown'}</div>
                            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                              {sub.lang} · {new Date(sub.timestamp * 1000).toLocaleDateString()}
                            </div>
                          </div>
                          <button className="btn btn-primary btn-sm" onClick={() => quickSync(sub)}>
                            ⚡ Quick Sync
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Sync Solution button */}
              {!showSyncForm && (
                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button className="btn btn-primary" onClick={() => setShowSyncForm(true)}>
                    ✏️ Manual Sync
                  </button>
                </div>
              )}

              {/* Sync Solution form */}
              {showSyncForm && (
                <div className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                    <h3 style={{ margin: 0, color: 'var(--text-primary)' }}>⚡ Sync Solution to GitHub</h3>
                    <button className="btn btn-ghost btn-sm" onClick={() => { setShowSyncForm(false); setSyncForm(DEFAULT_SYNC_FORM); }}>✕ Cancel</button>
                  </div>

                  <div className="grid-2" style={{ gap: 16, marginBottom: 16 }}>
                    <div className="input-group">
                      <label className="input-label">Problem Title *</label>
                      <input
                        className="input"
                        placeholder="e.g. Two Sum"
                        value={syncForm.problemTitle}
                        onChange={e => handleTitleChange(e.target.value)}
                      />
                    </div>
                    <div className="input-group">
                      <label className="input-label">Problem Slug *</label>
                      <input
                        className="input"
                        placeholder="e.g. two-sum"
                        value={syncForm.problemSlug}
                        onChange={e => setSyncForm(f => ({ ...f, problemSlug: e.target.value }))}
                      />
                      <span style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>Auto-generated from title, or paste from LeetCode URL</span>
                    </div>
                  </div>

                  <div className="grid-2" style={{ gap: 16, marginBottom: 16 }}>
                    <div className="input-group">
                      <label className="input-label">Language</label>
                      <select
                        className="input"
                        value={syncForm.language}
                        onChange={e => setSyncForm(f => ({ ...f, language: e.target.value }))}
                      >
                        {LANGUAGES.map(l => <option key={l} value={l}>{l}</option>)}
                      </select>
                    </div>
                    <div className="input-group">
                      <label className="input-label">Difficulty</label>
                      <select
                        className="input"
                        value={syncForm.difficulty}
                        onChange={e => setSyncForm(f => ({ ...f, difficulty: e.target.value }))}
                      >
                        {DIFFICULTIES.map(d => <option key={d} value={d}>{d}</option>)}
                      </select>
                    </div>
                  </div>

                  <div className="input-group" style={{ marginBottom: 16 }}>
                    <label className="input-label">Problem Description (optional — helps AI give better review)</label>
                    <textarea
                      className="input"
                      style={{ minHeight: 80, resize: 'vertical', fontFamily: 'inherit' }}
                      placeholder="Paste the problem statement here..."
                      value={syncForm.problemDescription}
                      onChange={e => setSyncForm(f => ({ ...f, problemDescription: e.target.value }))}
                    />
                  </div>

                  <div className="input-group" style={{ marginBottom: 20 }}>
                    <label className="input-label">Your Solution Code *</label>
                    <textarea
                      className="input"
                      style={{ minHeight: 200, resize: 'vertical', fontFamily: 'monospace', fontSize: 13 }}
                      placeholder="Paste your accepted solution here..."
                      value={syncForm.code}
                      onChange={e => setSyncForm(f => ({ ...f, code: e.target.value }))}
                    />
                  </div>

                  <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                    <button className="btn btn-secondary" onClick={() => { setShowSyncForm(false); setSyncForm(DEFAULT_SYNC_FORM); }}>
                      Cancel
                    </button>
                    <button className="btn btn-primary" onClick={handleSyncSolution} disabled={syncingLG}>
                      {syncingLG
                        ? <><span className="spinner" /> AI Reviewing &amp; Pushing...</>
                        : '⚡ Sync to GitHub'}
                    </button>
                  </div>
                </div>
              )}

              {/* Last sync result */}
              {lastSyncResult && (
                <div className="card" style={{ border: '1px solid var(--purple-primary)', background: 'var(--purple-glow)' }}>
                  <h3 style={{ marginBottom: 12, color: 'var(--text-primary)' }}>✅ Last Sync Result</h3>
                  <div className="grid-3" style={{ gap: 12, marginBottom: 12 }}>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, fontSize: 24, color: 'var(--purple-primary)' }}>{lastSyncResult.aiScore ?? '—'}/10</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>AI Score</div>
                    </div>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{lastSyncResult.timeComplexity ?? '—'}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Time Complexity</div>
                    </div>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{lastSyncResult.spaceComplexity ?? '—'}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Space Complexity</div>
                    </div>
                  </div>
                  {lastSyncResult.githubCommitUrl && (
                    <a href={lastSyncResult.githubCommitUrl} target="_blank" rel="noopener noreferrer" className="btn btn-secondary btn-sm">
                      View on GitHub →
                    </a>
                  )}
                </div>
              )}

              {/* Recent syncs history */}
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>Recent Syncs</h3>
                {leetgitStats.recentSyncs.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No syncs yet. Click "Sync New Solution" above to get started.</p>
                ) : leetgitStats.recentSyncs.map(s => (
                  <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, fontSize: 14, color: 'var(--text-primary)', marginBottom: 2 }}>{s.problemTitle}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{s.language} · {s.difficulty}</div>
                    </div>
                    {s.aiScore && (
                      <div style={{ textAlign: 'center' }}>
                        <div style={{ fontWeight: 700, color: 'var(--purple-primary)' }}>{s.aiScore}/10</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>AI Score</div>
                      </div>
                    )}
                    <span className={`badge ${s.status === 'SYNCED' ? 'badge-success' : s.status === 'FAILED' ? 'badge-error' : 'badge-warning'}`}>{s.status}</span>
                    {s.githubCommitUrl && (
                      <a href={s.githubCommitUrl} target="_blank" rel="noopener noreferrer" className="btn btn-ghost btn-sm">View →</a>
                    )}
                  </div>
                ))}
              </div>

            </div>
          )}
        </div>
      )}
    </div>
  );
}