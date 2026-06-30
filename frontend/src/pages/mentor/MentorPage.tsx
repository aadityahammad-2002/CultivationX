import { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import toast from 'react-hot-toast';
import { mentorApi } from '../../api/mentor';
import type { ConversationDetail, ConversationSummary, CodeReviewResponse } from '../../types';
import styles from './Mentor.module.css';

const TABS = ['AI Chat', 'Code Review', 'History'];
const LANGUAGES = ['Java', 'Python', 'JavaScript', 'TypeScript', 'C++', 'C', 'Go', 'Rust', 'Kotlin'];

export function MentorPage() {
  const [tab, setTab] = useState(0);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [activeConv, setActiveConv] = useState<ConversationDetail | null>(null);
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [reviewing, setReviewing] = useState(false);
  const [reviewHistory, setReviewHistory] = useState<CodeReviewResponse[]>([]);
  const [activeReview, setActiveReview] = useState<CodeReviewResponse | null>(null);
  const [codeForm, setCodeForm] = useState({ code: '', language: 'Java', problemContext: '', requestBetterSolution: false });
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    mentorApi.getConversations().then(r => r.data && setConversations(r.data)).catch(() => {});
    mentorApi.getCodeReviewHistory().then(r => r.data && setReviewHistory(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConv?.messages]);

  const loadConversation = async (id: number) => {
    try {
      const res = await mentorApi.getConversation(id);
      if (res.data) setActiveConv(res.data);
    } catch { toast.error('Failed to load conversation'); }
  };

  const newConversation = () => setActiveConv(null);

  const sendMessage = async () => {
    if (!message.trim() || sending) return;
    setSending(true);
    const msg = message;
    setMessage('');

    // Optimistic update
    const tempId = Date.now();
    if (activeConv) {
      setActiveConv(prev => prev ? {
        ...prev,
        messages: [...prev.messages, { id: tempId, role: 'user', content: msg, createdAt: new Date().toISOString() }]
      } : null);
    }

    try {
      const res = await mentorApi.chat({ message: msg, conversationId: activeConv?.id });
      if (res.data) {
        const convRes = await mentorApi.getConversation(res.data.conversationId);
        if (convRes.data) setActiveConv(convRes.data);
        const convsRes = await mentorApi.getConversations();
        if (convsRes.data) setConversations(convsRes.data);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to send message');
    } finally {
      setSending(false);
    }
  };

  const handleCodeReview = async () => {
    if (!codeForm.code.trim()) { toast.error('Please paste your code'); return; }
    setReviewing(true);
    try {
      const res = await mentorApi.reviewCode(codeForm);
      if (res.data) {
        setActiveReview(res.data);
        setReviewHistory(prev => [res.data!, ...prev]);
        toast.success('Code review complete!');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Review failed');
    } finally {
      setReviewing(false);
    }
  };

  const deleteConv = async (id: number) => {
    try {
      await mentorApi.deleteConversation(id);
      setConversations(prev => prev.filter(c => c.id !== id));
      if (activeConv?.id === id) setActiveConv(null);
    } catch { toast.error('Failed to delete'); }
  };

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 24 }}>
        <h1>🤖 Mentor</h1>
        <p>Your AI coding coach — ask anything, review code, get interview tips</p>
      </div>

      <div className="tabs">
        {TABS.map((t, i) => (
          <button key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</button>
        ))}
      </div>

      {/* AI CHAT TAB */}
      {tab === 0 && (
        <div className={styles.chatLayout}>
          {/* Sidebar */}
          <div className={styles.chatSidebar}>
            <button className="btn btn-primary btn-full btn-sm" onClick={newConversation} style={{ marginBottom: 12 }}>
              + New Chat
            </button>
            <div className={styles.convList}>
              {conversations.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: '12px 0' }}>No conversations yet</p>
              ) : conversations.map(c => (
                <div
                  key={c.id}
                  className={`${styles.convItem} ${activeConv?.id === c.id ? styles.convItemActive : ''}`}
                  onClick={() => loadConversation(c.id)}
                >
                  <div className={styles.convTitle}>{c.title}</div>
                  <div className={styles.convMeta}>{c.messageCount} messages</div>
                  <button className={styles.convDelete} onClick={e => { e.stopPropagation(); deleteConv(c.id); }}>✕</button>
                </div>
              ))}
            </div>
          </div>

          {/* Chat area */}
          <div className={styles.chatMain}>
            <div className={styles.messages}>
              {!activeConv ? (
                <div className={styles.chatWelcome}>
                  <div style={{ fontSize: 48, marginBottom: 16 }}>🤖</div>
                  <h3>AI Mentor</h3>
                  <p>Ask me anything about coding, DSA, system design, career advice, or interview prep.</p>
                  <div className={styles.suggestChips}>
                    {['Explain time complexity', 'Review my Java code', 'System design basics', 'Interview tips for FAANG'].map(s => (
                      <button key={s} className={styles.suggestChip} onClick={() => { setMessage(s); }}>
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              ) : activeConv.messages.map(m => (
                <div key={m.id} className={`${styles.message} ${m.role === 'user' ? styles.messageUser : styles.messageAi}`}>
                  {m.role === 'assistant' && <div className={styles.messageAvatar}>🤖</div>}
                  <div className={styles.messageBubble}>
                    {m.role === 'assistant' ? (
                      <div className="markdown-content">
                        <ReactMarkdown>{m.content}</ReactMarkdown>
                      </div>
                    ) : m.content}
                  </div>
                </div>
              ))}
              {sending && (
                <div className={`${styles.message} ${styles.messageAi}`}>
                  <div className={styles.messageAvatar}>🤖</div>
                  <div className={styles.messageBubble}>
                    <span className="pulse" style={{ color: 'var(--text-muted)' }}>Thinking...</span>
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            <div className={styles.chatInput}>
              <textarea
                className={styles.messageInput}
                placeholder="Ask anything... (Shift+Enter for new line, Enter to send)"
                value={message}
                onChange={e => setMessage(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); } }}
                rows={2}
              />
              <button className="btn btn-primary" onClick={sendMessage} disabled={sending || !message.trim()}>
                {sending ? <span className="spinner" /> : '→'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CODE REVIEW TAB */}
      {tab === 1 && (
        <div className={styles.reviewLayout}>
          <div className={styles.reviewInput}>
            <div className="card">
              <h3 className="card-title" style={{ marginBottom: 16 }}>Submit Code for Review</h3>
              <div className="input-group">
                <label className="input-label">Language</label>
                <select className="input" value={codeForm.language} onChange={e => setCodeForm(f => ({ ...f, language: e.target.value }))}>
                  {LANGUAGES.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
              <div className="input-group">
                <label className="input-label">Problem / Context</label>
                <input className="input" placeholder="e.g. Two Sum, Binary Search, Spring Boot REST endpoint..." value={codeForm.problemContext} onChange={e => setCodeForm(f => ({ ...f, problemContext: e.target.value }))} />
              </div>
              <div className="input-group">
                <label className="input-label">Your Code</label>
                <textarea className="input" rows={14} placeholder="Paste your code here..." style={{ fontFamily: 'monospace', fontSize: 13 }} value={codeForm.code} onChange={e => setCodeForm(f => ({ ...f, code: e.target.value }))} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <input type="checkbox" id="betterSol" checked={codeForm.requestBetterSolution} onChange={e => setCodeForm(f => ({ ...f, requestBetterSolution: e.target.checked }))} />
                <label htmlFor="betterSol" style={{ fontSize: 13, color: 'var(--text-secondary)', cursor: 'pointer' }}>Also generate an optimized solution</label>
              </div>
              <button className="btn btn-primary btn-full" onClick={handleCodeReview} disabled={reviewing}>
                {reviewing ? <><span className="spinner" /> Analyzing code...</> : '🔍 Review Code'}
              </button>
            </div>
          </div>

          {/* Review result */}
          <div className={styles.reviewResult}>
            {!activeReview ? (
              <div className="empty-state"><h3>Submit code for review</h3><p>Paste your code and get AI feedback on correctness, performance, security, and more</p></div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div className="card">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontSize: 48, fontWeight: 800, color: (activeReview.overallScore ?? 0) >= 8 ? 'var(--success)' : (activeReview.overallScore ?? 0) >= 6 ? 'var(--warning)' : 'var(--error)' }}>
                        {activeReview.overallScore}/10
                      </div>
                      <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>{activeReview.grade}</div>
                    </div>
                    <div style={{ flex: 1 }}>
                      <p style={{ fontSize: 13 }}>Language: <strong>{activeReview.language}</strong></p>
                      {activeReview.problemContext && <p style={{ fontSize: 13 }}>Problem: <strong>{activeReview.problemContext}</strong></p>}
                    </div>
                  </div>
                </div>

                {activeReview.review && (
                  <div className="card">
                    <h3 className="card-title" style={{ marginBottom: 16 }}>Detailed Review</h3>
                    {(['correctness', 'performance', 'security', 'readability', 'bestPractices'] as const).map(key => {
                      const section = (activeReview.review as any)?.[key];
                      if (!section) return null;
                      return (
                        <div key={key} style={{ marginBottom: 16 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                            <span style={{ color: 'var(--text-primary)', textTransform: 'capitalize' }}>{key}</span>
                            <span style={{ color: 'var(--purple-light)' }}>{section.score}/10</span>
                          </div>
                          <div className="progress-bar-wrap">
                            <div className="progress-bar-fill" style={{ width: `${section.score * 10}%`, background: 'var(--purple-primary)' }} />
                          </div>
                          {section.issues?.length > 0 && section.issues.map((issue: string, i: number) => (
                            <div key={i} style={{ fontSize: 12, color: 'var(--warning)', marginTop: 4, display: 'flex', gap: 6 }}>
                              <span>⚠</span><span>{issue}</span>
                            </div>
                          ))}
                        </div>
                      );
                    })}
                  </div>
                )}

                {activeReview.betterSolution && (
                  <div className="card">
                    <h3 className="card-title" style={{ marginBottom: 16 }}>🚀 Optimized Solution</h3>
                    <div className="markdown-content">
                      <ReactMarkdown>{String((activeReview.betterSolution as any)?.optimizedCode ?? '')}</ReactMarkdown>
                    </div>
                    <p style={{ fontSize: 13, marginTop: 8 }}>{String((activeReview.betterSolution as any)?.tradeoffs ?? '')}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* HISTORY TAB */}
      {tab === 2 && (
        <div className="fade-in">
          {!reviewHistory.length ? (
            <div className="empty-state"><h3>No code reviews yet</h3><p>Submit code for review to see your history</p></div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {reviewHistory.map((r, i) => (
                <div key={r.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 16, cursor: 'pointer' }} onClick={() => { setActiveReview(r); setTab(1); }}>
                  <div style={{ width: 48, height: 48, background: (r.overallScore ?? 0) >= 8 ? 'var(--success-bg)' : 'var(--warning-bg)', border: `1px solid ${(r.overallScore ?? 0) >= 8 ? 'rgba(16,185,129,0.3)' : 'rgba(245,158,11,0.3)'}`, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 14, color: (r.overallScore ?? 0) >= 8 ? 'var(--success)' : 'var(--warning)', flexShrink: 0 }}>
                    {r.overallScore}/10
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2, fontSize: 14 }}>{r.problemContext || 'General Review'}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.language} · {new Date(r.createdAt).toLocaleDateString()}</div>
                  </div>
                  <span className={`badge ${(r.overallScore ?? 0) >= 8 ? 'badge-success' : 'badge-warning'}`}>{r.grade}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}