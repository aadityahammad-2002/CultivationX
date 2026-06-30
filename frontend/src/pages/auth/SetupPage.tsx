import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';
import type { Goal, ExperienceLevel } from '../../types';
import styles from './Setup.module.css';

const GOALS: { value: Goal; label: string; desc: string; icon: string }[] = [
  { value: 'INTERNSHIP',      label: 'Internship',         desc: 'Land your first internship', icon: '🎓' },
  { value: 'SDE_1',           label: 'SDE-1',              desc: 'Entry-level full-time role',  icon: '💼' },
  { value: 'FAANG',           label: 'FAANG',              desc: 'Big tech companies',          icon: '🚀' },
  { value: 'PRODUCT_COMPANY', label: 'Product Company',    desc: 'Product-based companies',     icon: '🏢' },
  { value: 'SERVICE_COMPANY', label: 'Service Company',    desc: 'IT service companies',        icon: '🌐' },
];

const LEVELS: { value: ExperienceLevel; label: string }[] = [
  { value: 'FRESHER', label: 'Fresher (0 years)' },
  { value: 'JUNIOR',  label: 'Junior (1-2 years)' },
  { value: 'MID',     label: 'Mid-level (2-4 years)' },
  { value: 'SENIOR',  label: 'Senior (4+ years)' },
];

export function SetupPage() {
  const navigate = useNavigate();
  const { updateUser } = useAuth();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    goal: '' as Goal,
    experienceLevel: 'FRESHER' as ExperienceLevel,
    targetCompany: '',
    currentRole: '',
    leetcodeUsername: '',
    bio: '',
  });

  const TOTAL_STEPS = 4;

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const res = await authApi.setup({
        goal: form.goal,
        experienceLevel: form.experienceLevel,
        targetCompany: form.targetCompany || undefined,
        currentRole: form.currentRole || undefined,
        leetcodeUsername: form.leetcodeUsername || undefined,
        bio: form.bio || undefined,
      });
      if (res.data) {
        updateUser(res.data);
        toast.success('Profile setup complete! 🎉');
        navigate('/dashboard');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Setup failed');
    } finally {
      setLoading(false);
    }
  };

  const canProceed = () => {
    if (step === 1) return !!form.goal;
    return true;
  };

  return (
    <div className={styles.setupPage}>
      <div className={styles.setupCard}>
        {/* Header */}
        <div className={styles.setupHeader}>
          <div className={styles.logoMark}>CX</div>
          <h1>Welcome to CultivationX 👋</h1>
          <p>Let's build your developer profile in just a few steps</p>
        </div>

        {/* Progress */}
        <div className={styles.progressWrap}>
          {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
            <div
              key={i}
              className={`${styles.progressDot} ${i + 1 <= step ? styles.progressDotActive : ''}`}
            />
          ))}
        </div>
        <p className={styles.stepLabel}>Step {step} of {TOTAL_STEPS}</p>

        {/* Step 1 - Goal */}
        {step === 1 && (
          <div className={styles.stepContent}>
            <h2>What's your primary goal?</h2>
            <p className={styles.stepDesc}>This helps us personalize your learning path</p>
            <div className={styles.goalGrid}>
              {GOALS.map(g => (
                <button
                  key={g.value}
                  className={`${styles.goalCard} ${form.goal === g.value ? styles.goalCardActive : ''}`}
                  onClick={() => setForm(f => ({ ...f, goal: g.value }))}
                  type="button"
                >
                  <span className={styles.goalIcon}>{g.icon}</span>
                  <span className={styles.goalLabel}>{g.label}</span>
                  <span className={styles.goalDesc}>{g.desc}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Step 2 - Experience */}
        {step === 2 && (
          <div className={styles.stepContent}>
            <h2>Your experience level</h2>
            <p className={styles.stepDesc}>Help us calibrate recommendations to your level</p>
            <div className={styles.levelList}>
              {LEVELS.map(l => (
                <button
                  key={l.value}
                  className={`${styles.levelCard} ${form.experienceLevel === l.value ? styles.levelCardActive : ''}`}
                  onClick={() => setForm(f => ({ ...f, experienceLevel: l.value }))}
                  type="button"
                >
                  <span className={`${styles.levelRadio} ${form.experienceLevel === l.value ? styles.levelRadioActive : ''}`} />
                  {l.label}
                </button>
              ))}
            </div>
            <div className="input-group" style={{ marginTop: 20 }}>
              <label className="input-label">Target company (optional)</label>
              <input
                className="input"
                placeholder="e.g. Google, Microsoft, Flipkart..."
                value={form.targetCompany}
                onChange={e => setForm(f => ({ ...f, targetCompany: e.target.value }))}
              />
            </div>
          </div>
        )}

        {/* Step 3 - LeetCode */}
        {step === 3 && (
          <div className={styles.stepContent}>
            <h2>Connect LeetCode</h2>
            <p className={styles.stepDesc}>We'll sync your problem-solving stats to track your DSA progress</p>
            <div className={styles.platformCard}>
              <span className={styles.platformIcon}>💻</span>
              <div>
                <strong>LeetCode Username</strong>
                <p>Enter your LeetCode username to sync your stats</p>
              </div>
            </div>
            <div className="input-group" style={{ marginTop: 16 }}>
              <label className="input-label">LeetCode username</label>
              <input
                className="input"
                placeholder="your-leetcode-username"
                value={form.leetcodeUsername}
                onChange={e => setForm(f => ({ ...f, leetcodeUsername: e.target.value }))}
              />
            </div>
            <p className={styles.skipNote}>You can skip this and connect later from Nexus</p>
          </div>
        )}

        {/* Step 4 - Bio */}
        {step === 4 && (
          <div className={styles.stepContent}>
            <h2>Almost done!</h2>
            <p className={styles.stepDesc}>Add a short bio to complete your profile</p>
            <div className="input-group">
              <label className="input-label">Bio (optional)</label>
              <textarea
                className="input"
                rows={3}
                placeholder="e.g. Backend developer passionate about Java and Spring Boot..."
                value={form.bio}
                onChange={e => setForm(f => ({ ...f, bio: e.target.value }))}
              />
            </div>
            <div className={styles.readyCard}>
              <span>🎉</span>
              <div>
                <strong>Your profile is ready!</strong>
                <p>We'll generate your personalized Dev DNA score and recommendations.</p>
              </div>
            </div>
          </div>
        )}

        {/* Nav buttons */}
        <div className={styles.setupNav}>
          {step > 1 && (
            <button className="btn btn-secondary" onClick={() => setStep(s => s - 1)}>
              ← Back
            </button>
          )}
          <div style={{ flex: 1 }} />
          {step < TOTAL_STEPS ? (
            <button
              className="btn btn-primary"
              onClick={() => setStep(s => s + 1)}
              disabled={!canProceed()}
            >
              Next →
            </button>
          ) : (
            <button
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={loading}
            >
              {loading ? <><span className="spinner" /> Setting up...</> : '🚀 Launch Dashboard'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}