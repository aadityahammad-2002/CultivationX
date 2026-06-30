import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../../api/dashboard';
import type { DashboardResponse } from '../../types';
import styles from './Dashboard.module.css';

function ScoreRing({ value, size = 80, color = '#6c63ff' }: { value: number; size?: number; color?: string }) {
  const radius = (size - 8) / 2;
  const circ = 2 * Math.PI * radius;
  const offset = circ - (value / 100) * circ;
  return (
    <div className="score-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size}>
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--border)" strokeWidth={6} />
        <circle
          cx={size / 2} cy={size / 2} r={radius} fill="none"
          stroke={color} strokeWidth={6}
          strokeDasharray={circ} strokeDashoffset={offset}
          strokeLinecap="round" style={{ transition: 'stroke-dashoffset 1s ease' }}
        />
      </svg>
      <div className="score-ring-text">
        <div className="score-ring-number">{value}</div>
        <div className="score-ring-label">/ 100</div>
      </div>
    </div>
  );
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.get()
      .then(res => { if (res.data) setData(res.data); })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return (
    <div className="page-loader">
      <div className="spinner" style={{ width: 40, height: 40, borderWidth: 3 }} />
      <p>Loading your dashboard...</p>
    </div>
  );

  if (!data) return <div className="empty-state"><h3>Failed to load dashboard</h3></div>;

  return (
    <div className={`${styles.dashboard} fade-in`}>
      {/* Top greeting bar */}
      <div className={styles.greetingBar}>
        <div>
          <h1 className={styles.greeting}>{data.greeting}</h1>
          <p className={styles.greetingSub}>
            {data.interviewReadinessScore < 70
              ? `You're ${data.interviewReadinessScore}% interview ready. Let's push to 90%!`
              : `You're ${data.interviewReadinessScore}% interview ready. Keep going! 🔥`}
          </p>
        </div>
        <div className={styles.streakBadge}>
          🔥 <strong>{data.currentStreak}</strong> day streak
        </div>
      </div>

      {/* Quick stats */}
      <div className={`grid-4 ${styles.statsRow}`}>
        <div className="stat-card">
          <span className="stat-card-label">Interview Readiness</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginTop: 8 }}>
            <ScoreRing value={data.interviewReadinessScore} size={70} color="#6c63ff" />
            <div>
              <div className="stat-card-value" style={{ fontSize: 20 }}>{data.interviewReadinessScore}%</div>
              <div className="stat-card-sub">
                {data.interviewReadinessScore >= 80 ? 'Strong' : data.interviewReadinessScore >= 60 ? 'Good' : 'Building'}
              </div>
            </div>
          </div>
        </div>

        <div className="stat-card">
          <span className="stat-card-label">Dev DNA Score</span>
          <div className="stat-card-value">{data.devDnaScore}<span style={{ fontSize: 16, color: 'var(--text-muted)' }}>/100</span></div>
          <div className="stat-card-sub">AI developer intelligence</div>
        </div>

        <div className="stat-card">
          <span className="stat-card-label">LeetCode Solved</span>
          <div className="stat-card-value">{data.leetcodeSolved}</div>
          <div className="stat-card-sub">problems solved</div>
        </div>

        <div className="stat-card">
          <span className="stat-card-label">Resume ATS</span>
          <div className="stat-card-value">{data.atsScore}<span style={{ fontSize: 16, color: 'var(--text-muted)' }}>/100</span></div>
          <div className="stat-card-sub">ATS compatibility</div>
        </div>
      </div>

      {/* Main content grid */}
      <div className={styles.mainGrid}>
        {/* Left column */}
        <div className={styles.leftCol}>
          {/* Today's tasks */}
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">📅 Today's Tasks</h3>
              <Link to="/devdna" className="btn btn-ghost btn-sm">Full Plan</Link>
            </div>
            <div className={styles.taskList}>
              {data.todaysTasks.length === 0 ? (
                <p className={styles.emptyTasks}>No tasks yet. Generate your Dev DNA report to get personalized tasks.</p>
              ) : data.todaysTasks.map((task, i) => (
                <div key={i} className={styles.taskItem}>
                  <div className={`${styles.taskPriority} ${styles[`priority_${task.priority}`]}`} />
                  <div className={styles.taskText}>
                    <span>{task.task}</span>
                    <span className={styles.taskImpact}>{task.impact}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Progress */}
          <div className="card" style={{ marginTop: 16 }}>
            <div className="card-header">
              <h3 className="card-title">📈 Progress</h3>
            </div>
            <div className={styles.progressList}>
              {data.progress.map((p, i) => (
                <div key={i} className={styles.progressItem}>
                  <div className={styles.progressLabel}>
                    <span>{p.label}</span>
                    <span className={styles.progressValue}>{p.value}%</span>
                  </div>
                  <div className="progress-bar-wrap">
                    <div className="progress-bar-fill" style={{ width: `${p.value}%`, background: p.color }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right column */}
        <div className={styles.rightCol}>
          {/* Connection status */}
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">⚡ Quick Actions</h3>
            </div>
            <div className={styles.quickActions}>
              <Link to="/rise" className={styles.quickAction}>
                <span>📄</span>
                <span>{data.atsScore > 0 ? 'View Resume' : 'Upload Resume'}</span>
              </Link>
              <Link to="/mentor" className={styles.quickAction}>
                <span>🤖</span>
                <span>AI Mentor</span>
              </Link>
              <Link to="/nexus" className={styles.quickAction}>
                <span>🌐</span>
                <span>{data.githubConnected ? 'Sync GitHub' : 'Connect GitHub'}</span>
              </Link>
              <Link to="/devdna" className={styles.quickAction}>
                <span>🧬</span>
                <span>Dev DNA Report</span>
              </Link>
            </div>

            {/* Connection chips */}
            <div className={styles.connectionStatus}>
              <div className={`${styles.connChip} ${data.githubConnected ? styles.connChipOn : ''}`}>
                GitHub {data.githubConnected ? '✓' : '✗'}
              </div>
              <div className={`${styles.connChip} ${data.leetcodeConnected ? styles.connChipOn : ''}`}>
                LeetCode {data.leetcodeConnected ? '✓' : '✗'}
              </div>
              <div className={`${styles.connChip} ${data.leetgitEnabled ? styles.connChipOn : ''}`}>
                LeetGit {data.leetgitEnabled ? '✓' : '✗'}
              </div>
            </div>
          </div>

          {/* Recent activity */}
          <div className="card" style={{ marginTop: 16 }}>
            <div className="card-header">
              <h3 className="card-title">🕐 Recent Activity</h3>
            </div>
            {data.recentActivity.length === 0 ? (
              <div className="empty-state" style={{ padding: '24px' }}>
                <p>No activity yet. Start by uploading your resume!</p>
              </div>
            ) : (
              <div className={styles.activityList}>
                {data.recentActivity.map((a, i) => (
                  <div key={i} className={styles.activityItem}>
                    <span className={styles.activityIcon}>{a.icon}</span>
                    <div className={styles.activityContent}>
                      <span className={styles.activityDesc}>{a.description}</span>
                      <span className={styles.activityTime}>{a.time}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* GitHub stats if connected */}
          {data.githubConnected && (
            <div className={styles.githubCard}>
              <span>⚙️</span>
              <div>
                <strong>{data.githubRepos} repos</strong>
                <p>{data.githubContributions} contributions this year</p>
              </div>
              <Link to="/nexus" className="btn btn-ghost btn-sm">View →</Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}