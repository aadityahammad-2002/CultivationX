import { useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { riseApi } from '../../api/rise';
import type { ResumeResponse, SkillGapResponse } from '../../types';
import styles from './Rise.module.css';

const TABS = ['Upload', 'Analysis', 'Skills', 'Gap Analysis', 'Roadmap', 'History'];

function ScoreBar({ label, value, color }: { label: string; value?: number; color: string }) {
  const v = value ?? 0;
  return (
    <div className={styles.scoreBar}>
      <div className={styles.scoreBarLabel}>
        <span>{label}</span><span style={{ color }}>{v}/100</span>
      </div>
      <div className="progress-bar-wrap">
        <div className="progress-bar-fill" style={{ width: `${v}%`, background: color }} />
      </div>
    </div>
  );
}

export function RisePage() {
  const [tab, setTab] = useState(0);
  const [resume, setResume] = useState<ResumeResponse | null>(null);
  const [skillGap, setSkillGap] = useState<SkillGapResponse | null>(null);
  const [history, setHistory] = useState<ResumeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [uploadPct, setUploadPct] = useState(0);
  const [reanalyzing, setReanalyzing] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    Promise.all([
      riseApi.getActive().then(r => r.data && setResume(r.data)).catch(() => {}),
      riseApi.getSkillGap().then(r => r.data && setSkillGap(r.data)).catch(() => {}),
      riseApi.getHistory().then(r => r.data && setHistory(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);

  const handleFile = async (file: File) => {
    setUploading(true);
    setUploadPct(0);
    try {
      const res = await riseApi.upload(file, setUploadPct);
      if (res.data) {
        setResume(res.data);
        toast.success('Resume uploaded and analyzed!');
        setTab(1);
        riseApi.getHistory().then(r => r.data && setHistory(r.data)).catch(() => {});
        riseApi.getSkillGap().then(r => r.data && setSkillGap(r.data)).catch(() => {});
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleReanalyze = async () => {
    setReanalyzing(true);
    try {
      const res = await riseApi.reanalyze();
      if (res.data) { setResume(res.data); toast.success('Resume reanalyzed!'); }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Reanalysis failed');
    } finally {
      setReanalyzing(false);
    }
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  if (loading) return (
    <div className="page-loader">
      <div className="spinner" style={{ width: 40, height: 40, borderWidth: 3 }} />
      <p>Loading Rise...</p>
    </div>
  );

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 24 }}>
        <h1>🚀 Rise</h1>
        <p>Build a resume that matches your real skills</p>
      </div>

      <div className="tabs">
        {TABS.map((t, i) => (
          <button key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>
            {t}
          </button>
        ))}
      </div>

      {/* UPLOAD TAB */}
      {tab === 0 && (
        <div className="fade-in">
          <div
            className={`upload-zone ${uploading ? 'dragging' : ''}`}
            onDragOver={e => e.preventDefault()}
            onDrop={onDrop}
            onClick={() => !uploading && fileRef.current?.click()}
          >
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.docx"
              style={{ display: 'none' }}
              onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])}
            />
            {uploading ? (
              <div>
                <p style={{ marginBottom: 16, color: 'var(--text-primary)' }}>
                  {uploadPct < 50 ? 'Uploading...' : uploadPct < 80 ? 'Parsing resume...' : 'Running AI analysis...'}
                </p>
                <div className="progress-bar-wrap" style={{ width: 300, margin: '0 auto' }}>
                  <div className="progress-bar-fill" style={{ width: `${uploadPct}%`, background: 'var(--purple-primary)' }} />
                </div>
                <p style={{ marginTop: 8, color: 'var(--text-muted)', fontSize: 12 }}>{uploadPct}%</p>
              </div>
            ) : (
              <>
                <div style={{ fontSize: 48, marginBottom: 16 }}>📄</div>
                <h3 style={{ color: 'var(--text-primary)', marginBottom: 8 }}>
                  {resume ? 'Upload a New Resume' : 'Upload Your Resume'}
                </h3>
                <p style={{ marginBottom: 16, fontSize: 14 }}>Drag & drop or click to select a PDF or DOCX file (max 10MB)</p>
                <button className="btn btn-primary" onClick={e => { e.stopPropagation(); fileRef.current?.click(); }}>
                  Choose File
                </button>
              </>
            )}
          </div>

          {resume && (
            <div className="card" style={{ marginTop: 20 }}>
              <div className="card-header">
                <h3 className="card-title">📋 Current Resume</h3>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-secondary btn-sm" onClick={handleReanalyze} disabled={reanalyzing}>
                    {reanalyzing ? <><span className="spinner" /> Analyzing...</> : '🔄 Reanalyze'}
                  </button>
                  <button className="btn btn-primary btn-sm" onClick={() => setTab(1)}>View Analysis →</button>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                <div><span style={{ color: 'var(--text-muted)', fontSize: 12 }}>File</span><p style={{ color: 'var(--text-primary)', margin: 0 }}>{resume.fileName}</p></div>
                <div><span style={{ color: 'var(--text-muted)', fontSize: 12 }}>ATS Score</span><p style={{ color: 'var(--purple-primary)', margin: 0, fontWeight: 700, fontSize: 18 }}>{resume.atsScore ?? '--'}/100</p></div>
                <div><span style={{ color: 'var(--text-muted)', fontSize: 12 }}>Version</span><p style={{ color: 'var(--text-primary)', margin: 0 }}>v{resume.version}</p></div>
                <div><span style={{ color: 'var(--text-muted)', fontSize: 12 }}>Status</span><span className={`badge ${resume.status === 'ANALYZED' ? 'badge-success' : 'badge-warning'}`}>{resume.status}</span></div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ANALYSIS TAB */}
      {tab === 1 && (
        <div className="fade-in">
          {!resume || resume.status !== 'ANALYZED' ? (
            <div className="empty-state">
              <h3>No analysis yet</h3>
              <p>Upload your resume first to see the ATS analysis</p>
              <button className="btn btn-primary" onClick={() => setTab(0)}>Upload Resume</button>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              {/* Score overview */}
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 20 }}>ATS Score Overview</h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 24 }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 52, fontWeight: 800, color: resume.atsScore! >= 80 ? 'var(--success)' : resume.atsScore! >= 60 ? 'var(--warning)' : 'var(--error)' }}>
                      {resume.atsScore}
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>out of 100</div>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 4 }}>
                      {resume.atsScore! >= 80 ? '✅ Strong ATS compatibility' : resume.atsScore! >= 60 ? '⚠️ Good, room to improve' : '❌ Needs significant improvement'}
                    </div>
                    <p style={{ fontSize: 13, margin: 0 }}>{resume.overallFeedback}</p>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  <ScoreBar label="Keyword Match" value={resume.keywordMatchScore} color="#6c63ff" />
                  <ScoreBar label="Formatting" value={resume.formattingScore} color="#a78bfa" />
                  <ScoreBar label="Action Verbs" value={resume.actionVerbScore} color="#818cf8" />
                  <ScoreBar label="Quantified Achievements" value={resume.quantifiedAchievementsScore} color="#c4b5fd" />
                </div>
              </div>

              {/* Improvements & Strengths */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>✅ Strengths</h3>
                  {resume.strengths?.map((s, i) => (
                    <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, fontSize: 13 }}>
                      <span style={{ color: 'var(--success)' }}>✓</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{s}</span>
                    </div>
                  ))}
                </div>
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 16 }}>🔧 Top Improvements</h3>
                  {resume.improvements?.slice(0, 4).map((s, i) => (
                    <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, fontSize: 13 }}>
                      <span style={{ color: 'var(--warning)' }}>→</span>
                      <span style={{ color: 'var(--text-secondary)' }}>{s}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Experience / Education */}
              {(resume.experienceSummary || resume.educationSummary) && (
                <div className="card" style={{ gridColumn: '1 / -1' }}>
                  <h3 className="card-title" style={{ marginBottom: 16 }}>📋 Resume Summary</h3>
                  <div className="grid-3">
                    {resume.experienceSummary && <div><h4 style={{ color: 'var(--purple-light)', marginBottom: 6 }}>Experience</h4><p style={{ fontSize: 13 }}>{resume.experienceSummary}</p></div>}
                    {resume.educationSummary && <div><h4 style={{ color: 'var(--purple-light)', marginBottom: 6 }}>Education</h4><p style={{ fontSize: 13 }}>{resume.educationSummary}</p></div>}
                    {resume.projectsSummary && <div><h4 style={{ color: 'var(--purple-light)', marginBottom: 6 }}>Projects</h4><p style={{ fontSize: 13 }}>{resume.projectsSummary}</p></div>}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* SKILLS TAB */}
      {tab === 2 && (
        <div className="fade-in">
          {!resume?.extractedSkills?.length ? (
            <div className="empty-state"><h3>No skills extracted</h3><p>Upload and analyze your resume first</p></div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>✅ Detected Skills</h3>
                <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                  {resume.extractedSkills.map((s, i) => <span key={i} className="skill-tag">{s}</span>)}
                </div>
              </div>
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>⚠️ Missing Keywords</h3>
                {resume.missingKeywords?.length ? (
                  <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                    {resume.missingKeywords.map((s, i) => (
                      <span key={i} style={{ display: 'inline-flex', alignItems: 'center', padding: '4px 12px', background: 'var(--warning-bg)', border: '1px solid rgba(245,158,11,0.3)', borderRadius: '100px', fontSize: 12, color: 'var(--warning)', margin: 3 }}>{s}</span>
                    ))}
                  </div>
                ) : <p>No missing critical keywords detected 🎉</p>}
              </div>
            </div>
          )}
        </div>
      )}

      {/* GAP ANALYSIS TAB */}
      {tab === 3 && (
        <div className="fade-in">
          {!skillGap ? (
            <div className="empty-state"><h3>No gap analysis yet</h3><p>Upload your resume to generate a skill gap analysis</p></div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>🎯 Readiness Score</h3>
                <div style={{ fontSize: 48, fontWeight: 800, color: 'var(--purple-primary)', textAlign: 'center', margin: '16px 0' }}>
                  {skillGap.currentReadinessPercent}%
                </div>
                <p style={{ textAlign: 'center', fontSize: 13 }}>~{skillGap.estimatedReadinessWeeks} weeks to full readiness</p>
              </div>
              <div className="card">
                <h3 className="card-title" style={{ marginBottom: 16 }}>🔴 Skills to Build</h3>
                {skillGap.missingSkills?.map((s, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                    <span style={{ color: 'var(--text-primary)' }}>{s.skill}</span>
                    <span className={`badge ${s.priority === 'high' ? 'badge-error' : s.priority === 'medium' ? 'badge-warning' : 'badge-purple'}`}>{s.priority}</span>
                  </div>
                ))}
              </div>
              <div className="card" style={{ gridColumn: '1 / -1' }}>
                <h3 className="card-title" style={{ marginBottom: 16 }}>✅ Existing Skills</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {skillGap.existingSkills?.map((s, i) => (
                    <div key={i}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                        <span style={{ color: 'var(--text-primary)' }}>{s.skill}</span>
                        <span style={{ color: 'var(--text-muted)' }}>{s.proficiency}% · {s.source}</span>
                      </div>
                      <div className="progress-bar-wrap">
                        <div className="progress-bar-fill" style={{ width: `${s.proficiency}%`, background: 'var(--purple-primary)' }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ROADMAP TAB */}
      {tab === 4 && (
        <div className="fade-in">
          {!skillGap?.weeklyRoadmap?.length ? (
            <div className="empty-state"><h3>No roadmap yet</h3><p>Upload your resume to generate your personalized 8-week roadmap</p></div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {skillGap.weeklyRoadmap.map((week, i) => (
                <div key={i} className="card">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ width: 48, height: 48, background: 'var(--purple-glow)', border: '1px solid rgba(108,99,255,0.3)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, color: 'var(--purple-light)', flexShrink: 0 }}>
                      W{week.week}
                    </div>
                    <div style={{ flex: 1 }}>
                      <h4 style={{ color: 'var(--text-primary)', marginBottom: 8 }}>{week.focus}</h4>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                        {week.tasks?.map((t, j) => (
                          <span key={j} style={{ fontSize: 12, padding: '3px 10px', background: 'var(--bg-secondary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-secondary)' }}>
                            {t}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* HISTORY TAB */}
      {tab === 5 && (
        <div className="fade-in">
          {!history.length ? (
            <div className="empty-state"><h3>No resume history</h3><p>Upload your first resume to start tracking</p></div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {history.map((r, i) => (
                <div key={r.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                  <div style={{ fontSize: 32 }}>📄</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <h4 style={{ color: 'var(--text-primary)' }}>{r.fileName}</h4>
                      {r.active && <span className="badge badge-success">Active</span>}
                      <span className="badge badge-purple">v{r.version}</span>
                    </div>
                    <p style={{ fontSize: 12, margin: 0 }}>Uploaded {new Date(r.uploadedAt).toLocaleDateString()}</p>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--purple-primary)' }}>{r.atsScore ?? '--'}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>ATS Score</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}