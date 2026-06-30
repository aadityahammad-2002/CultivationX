import { useEffect, useState } from 'react';
import { RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import toast from 'react-hot-toast';
import { devDnaApi } from '../../api/devdna';
import type { DevDnaReportResponse, WeeklyReportResponse } from '../../types';

const TABS = ['Overview', 'Skills', 'Analytics', 'Insights', 'Reports'];

export function DevDnaPage() {
  const [tab, setTab] = useState(0);
  const [report, setReport] = useState<DevDnaReportResponse | null>(null);
  const [weekly, setWeekly] = useState<WeeklyReportResponse | null>(null);
  const [history, setHistory] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    Promise.all([
      devDnaApi.getLatest().then(r => r.data && setReport(r.data)).catch(() => {}),
      devDnaApi.getWeekly().then(r => r.data && setWeekly(r.data)).catch(() => {}),
      devDnaApi.getHistory().then((r: any) => r.data && setHistory(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);

  const generateReport = async () => {
    setGenerating(true);
    try {
      const res = await devDnaApi.generate();
      if (res.data) { setReport(res.data); toast.success('Dev DNA report generated!'); }
    } catch { toast.error('Generation failed'); }
    finally { setGenerating(false); }
  };

  if (loading) return (
    <div className="page-loader">
      <div className="spinner" style={{ width: 40, height: 40, borderWidth: 3 }} />
      <p>Loading Dev DNA...</p>
    </div>
  );

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div><h1>🧬 Dev DNA</h1><p>AI-powered developer intelligence engine</p></div>
        <button className="btn btn-primary" onClick={generateReport} disabled={generating}>
          {generating ? <><span className="spinner" /> Generating...</> : '🔄 Generate Report'}
        </button>
      </div>

      <div className="tabs">
        {TABS.map((t, i) => (
          <button key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</button>
        ))}
      </div>

      {/* OVERVIEW */}
      {tab === 0 && (
        <div className="fade-in">
          {!report ? (
            <div className="empty-state">
              <h3>No Dev DNA report yet</h3>
              <p>Generate your first report to see your developer intelligence score</p>
              <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={generateReport} disabled={generating}>
                {generating ? <><span className="spinner" /> Generating...</> : '🧬 Generate Dev DNA'}
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              {/* Score cards */}
              <div className="grid-4">
                <div className="stat-card">
                  <span className="stat-card-label">Dev DNA Score</span>
                  <span className="stat-card-value" style={{ fontSize: 40, color: 'var(--purple-primary)' }}>{report.devDnaScore}</span>
                  <span className="stat-card-sub">/ 100</span>
                </div>
                <div className="stat-card">
                  <span className="stat-card-label">Interview Readiness</span>
                  <span className="stat-card-value" style={{ color: (report.interviewReadinessScore ?? 0) >= 80 ? 'var(--success)' : 'var(--warning)' }}>
                    {report.interviewReadinessScore}%
                  </span>
                </div>
                <div className="stat-card">
                  <span className="stat-card-label">GitHub Score</span>
                  <span className="stat-card-value">{report.scoreBreakdown?.github ?? 0}</span>
                </div>
                <div className="stat-card">
                  <span className="stat-card-label">LeetCode Score</span>
                  <span className="stat-card-value">{report.scoreBreakdown?.leetcode ?? 0}</span>
                </div>
              </div>

              {/* Breakdown + profile */}
              <div className="grid-2">
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>Score Breakdown</h3>
                  {report.scoreBreakdown && Object.entries(report.scoreBreakdown).map(([key, val]) => (
                    <div key={key} style={{ marginBottom: 12 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                        <span style={{ color: 'var(--text-secondary)', textTransform: 'capitalize' }}>{key}</span>
                        <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{val}/100</span>
                      </div>
                      <div className="progress-bar-wrap">
                        <div className="progress-bar-fill" style={{ width: `${val}%`, background: 'var(--purple-primary)' }} />
                      </div>
                    </div>
                  ))}
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {report.profileSummary && (
                    <div className="card">
                      <h3 className="card-title" style={{ marginBottom: 12 }}>🤖 AI Profile Summary</h3>
                      <p style={{ fontSize: 14, lineHeight: 1.7 }}>{report.profileSummary}</p>
                    </div>
                  )}
                  {report.motivationalMessage && (
                    <div className="card" style={{ background: 'var(--purple-glow)', border: '1px solid rgba(108,99,255,0.3)' }}>
                      <p style={{ fontSize: 14, color: 'var(--purple-light)', lineHeight: 1.7, margin: 0 }}>
                        💡 {report.motivationalMessage}
                      </p>
                    </div>
                  )}
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', textAlign: 'right' }}>
                    Generated: {new Date(report.generatedAt).toLocaleString()}
                  </div>
                </div>
              </div>

              {/* Strengths and improvements */}
              <div className="grid-2">
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>✅ Top Strengths</h3>
                  {report.strengths?.length ? report.strengths.map((s, i) => (
                    <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, fontSize: 13 }}>
                      <span style={{ color: 'var(--success)', flexShrink: 0 }}>✓</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{s}</span>
                    </div>
                  )) : <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Generate a report to see strengths</p>}
                </div>
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>🎯 Areas to Improve</h3>
                  {report.areasToImprove?.length ? report.areasToImprove.map((s, i) => (
                    <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, fontSize: 13 }}>
                      <span style={{ color: 'var(--warning)', flexShrink: 0 }}>→</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{s}</span>
                    </div>
                  )) : <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Generate a report to see improvements</p>}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SKILLS RADAR */}
      {tab === 1 && (
        <div className="fade-in">
          {!report?.skillGraph?.length ? (
            <div className="empty-state">
              <h3>No skill data yet</h3>
              <p>Generate a Dev DNA report to see your skill radar</p>
              <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={generateReport} disabled={generating}>
                Generate Report
              </button>
            </div>
          ) : (
            <div className="grid-2">
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>Skill Radar</h3>
                <ResponsiveContainer width="100%" height={320}>
                  <RadarChart data={report.skillGraph.slice(0, 8)}>
                    <PolarGrid stroke="var(--border)" />
                    <PolarAngleAxis dataKey="skill" tick={{ fill: 'var(--text-secondary)', fontSize: 12 }} />
                    <Radar name="Score" dataKey="score" stroke="#6c63ff" fill="#6c63ff" fillOpacity={0.2} strokeWidth={2} />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>Skill Breakdown</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {report.skillGraph.map((s, i) => (
                    <div key={i}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                        <div>
                          <span style={{ color: 'var(--text-primary)' }}>{s.skill}</span>
                          <span style={{ color: 'var(--text-muted)', fontSize: 11, marginLeft: 8 }}>{s.category}</span>
                        </div>
                        <span style={{ color: 'var(--text-muted)' }}>{s.score}/100</span>
                      </div>
                      <div className="progress-bar-wrap" style={{ height: 6 }}>
                        <div className="progress-bar-fill" style={{ width: `${s.score}%`, background: 'var(--purple-primary)' }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ANALYTICS / GROWTH */}
      {tab === 2 && (
        <div className="fade-in">
          {!history?.scoreHistory?.length ? (
            <div className="empty-state">
              <h3>No history yet</h3>
              <p>Generate multiple reports over time to track your growth curve</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div className="grid-2">
                <div className="stat-card">
                  <span className="stat-card-label">Total Improvement</span>
                  <span className="stat-card-value" style={{ color: (history.totalImprovement ?? 0) >= 0 ? 'var(--success)' : 'var(--error)' }}>
                    {(history.totalImprovement ?? 0) >= 0 ? '+' : ''}{history.totalImprovement}
                  </span>
                  <span className="stat-card-sub">points overall</span>
                </div>
                <div className="stat-card">
                  <span className="stat-card-label">Trend</span>
                  <span className="stat-card-value">{history.trendDescription}</span>
                </div>
              </div>

              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>Growth Timeline</h3>
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={[...history.scoreHistory].reverse()}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                    <XAxis
                      dataKey="date"
                      tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
                      tickFormatter={(v) => new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
                    />
                    <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} domain={[0, 100]} />
                    <Tooltip
                      contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                      labelFormatter={(v) => new Date(v).toLocaleDateString()}
                    />
                    <Line type="monotone" dataKey="score" stroke="#6c63ff" strokeWidth={2} dot={false} name="Dev DNA" />
                    <Line type="monotone" dataKey="interviewReadiness" stroke="#10b981" strokeWidth={2} dot={false} name="Interview Readiness" />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>
      )}

      {/* INSIGHTS */}
      {tab === 3 && (
        <div className="fade-in">
          {!weekly ? (
            <div className="empty-state">
              <h3>No weekly insights yet</h3>
              <p>Generate a Dev DNA report to unlock personalized weekly insights</p>
              <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={generateReport} disabled={generating}>
                Generate Report
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              {/* Headline card */}
              <div className="card" style={{ background: 'var(--purple-glow)', border: '1px solid rgba(108,99,255,0.3)' }}>
                <h2 style={{ color: 'var(--purple-light)', marginBottom: 8 }}>{weekly.headline}</h2>
                <p style={{ margin: 0 }}>{weekly.overallProgress}</p>
              </div>

              {/* Mission */}
              {weekly.todaysMission && (
                <div className="card">
                  <div className="card-header">
                    <h3 className="card-title">🎯 Today's Mission</h3>
                    <span style={{ color: 'var(--success)', fontSize: 14, fontWeight: 600 }}>{weekly.todaysMission.totalImpact}</span>
                  </div>
                  {weekly.todaysMission.tasks?.map((t, i) => (
                    <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                        <span style={{ color: t.priority === 'high' ? 'var(--error)' : t.priority === 'medium' ? 'var(--warning)' : 'var(--success)', fontSize: 10 }}>●</span>
                        <span style={{ color: 'var(--text-primary)' }}>{t.task}</span>
                      </div>
                      <span style={{ color: 'var(--success)', fontWeight: 600, fontSize: 12, flexShrink: 0 }}>{t.impact}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Strengths & weaknesses */}
              <div className="grid-2">
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 12 }}>✅ This Week's Wins</h3>
                  {weekly.strengths?.map((s, i) => (
                    <div key={i} style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 6, display: 'flex', gap: 8 }}>
                      <span style={{ color: 'var(--success)', flexShrink: 0 }}>✓</span>{s}
                    </div>
                  ))}
                </div>
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 12 }}>🔧 Focus Areas</h3>
                  {weekly.weaknesses?.map((s, i) => (
                    <div key={i} style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 6, display: 'flex', gap: 8 }}>
                      <span style={{ color: 'var(--warning)', flexShrink: 0 }}>→</span>{s}
                    </div>
                  ))}
                </div>
              </div>

              {/* Weekly roadmap */}
              {weekly.weeklyGoals?.length > 0 && (
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>📅 Weekly Plan</h3>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 8 }}>
                    {weekly.weeklyGoals.map((g, i) => (
                      <div key={i} style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)', padding: '12px 8px', textAlign: 'center' }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--purple-light)', marginBottom: 4 }}>{g.day?.slice(0, 3)}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-primary)', fontWeight: 600, marginBottom: 6 }}>{g.focus}</div>
                        {g.tasks?.slice(0, 2).map((t, j) => (
                          <div key={j} style={{ fontSize: 10, color: 'var(--text-muted)', marginBottom: 2 }}>{t}</div>
                        ))}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Interview readiness */}
              {weekly.interviewReadiness && (
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>💼 Interview Readiness: {weekly.interviewReadiness.score}%</h3>
                  <div className="grid-2">
                    <div>
                      <h4 style={{ color: 'var(--success)', marginBottom: 8, fontSize: 14 }}>Ready For</h4>
                      {weekly.interviewReadiness.readyFor?.map((r, i) => (
                        <div key={i} style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 4 }}>✓ {r}</div>
                      ))}
                    </div>
                    <div>
                      <h4 style={{ color: 'var(--warning)', marginBottom: 8, fontSize: 14 }}>Not Ready Yet</h4>
                      {weekly.interviewReadiness.notReadyFor?.map((r, i) => (
                        <div key={i} style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 4 }}>→ {r}</div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Motivational message */}
              {weekly.motivationalMessage && (
                <div className="card" style={{ textAlign: 'center', padding: 28 }}>
                  <p style={{ fontSize: 15, color: 'var(--text-primary)', margin: 0, lineHeight: 1.8 }}>
                    💡 {weekly.motivationalMessage}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* REPORTS */}
      {tab === 4 && (
        <div className="fade-in">
          {!report ? (
            <div className="empty-state">
              <h3>No reports yet</h3>
              <p>Generate your first Dev DNA report to start tracking</p>
              <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={generateReport} disabled={generating}>
                {generating ? <><span className="spinner" /> Generating...</> : 'Generate First Report'}
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Latest Dev DNA Report</h3>
                  <button className="btn btn-primary btn-sm" onClick={generateReport} disabled={generating}>
                    {generating ? <><span className="spinner" /> Regenerating...</> : '🔄 Regenerate'}
                  </button>
                </div>
                <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 20 }}>
                  Generated: {new Date(report.generatedAt).toLocaleString()}
                </p>
                <div className="grid-2">
                  <div>
                    <h4 style={{ color: 'var(--text-secondary)', marginBottom: 8 }}>Dev DNA Score</h4>
                    <div style={{ fontSize: 52, fontWeight: 800, color: 'var(--purple-primary)' }}>{report.devDnaScore}</div>
                    <div style={{ fontSize: 14, color: 'var(--text-muted)' }}>out of 100</div>
                  </div>
                  <div>
                    <h4 style={{ color: 'var(--text-secondary)', marginBottom: 8 }}>Interview Readiness</h4>
                    <div style={{ fontSize: 52, fontWeight: 800, color: 'var(--success)' }}>{report.interviewReadinessScore}%</div>
                  </div>
                </div>
              </div>

              {history?.scoreHistory?.length > 1 && (
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>Report History</h3>
                  {history.scoreHistory.slice(0, 10).map((r: any, i: number) => (
                    <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                      <span style={{ color: 'var(--text-muted)' }}>{new Date(r.date).toLocaleDateString()}</span>
                      <div style={{ display: 'flex', gap: 20 }}>
                        <span>DNA: <strong style={{ color: 'var(--purple-primary)' }}>{r.score}</strong></span>
                        <span>Readiness: <strong style={{ color: 'var(--success)' }}>{r.interviewReadiness}%</strong></span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}