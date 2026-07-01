// ===== AUTH =====
export interface User {
  id: number;
  name: string;
  email: string;
  bio?: string;
  avatarUrl?: string;
  goal?: Goal;
  experienceLevel?: ExperienceLevel;
  targetCompany?: string;
  currentRole?: string;
  yearsOfExperience?: number;
  setupComplete: boolean;
  githubConnected: boolean;
  leetcodeConnected: boolean;
  leetgitEnabled: boolean;
  githubUsername?: string;
  leetcodeUsername?: string;
  leetgitRepoName?: string;
  devDnaScore: number;
  interviewReadinessScore: number;
  currentStreak: number;
  longestStreak: number;
  createdAt: string;
}

export type Goal = 'INTERNSHIP' | 'SDE_1' | 'FAANG' | 'PRODUCT_COMPANY' | 'SERVICE_COMPANY';
export type ExperienceLevel = 'FRESHER' | 'JUNIOR' | 'MID' | 'SENIOR';

export interface AuthResponse { token: string; user: User; }
export interface RegisterRequest { name: string; email: string; password: string; }
export interface LoginRequest { email: string; password: string; }
export interface SetupRequest {
  goal: Goal;
  experienceLevel?: ExperienceLevel;
  targetCompany?: string;
  currentRole?: string;
  yearsOfExperience?: number;
  leetcodeUsername?: string;
  bio?: string;
}

// ===== API RESPONSE =====
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
  timestamp: string;
}

// ===== DASHBOARD =====
export interface DashboardResponse {
  greeting: string;
  user: User;
  devDnaScore: number;
  interviewReadinessScore: number;
  currentStreak: number;
  atsScore: number;
  leetcodeSolved: number;
  githubRepos: number;
  githubContributions: number;
  leetgitSynced: number;
  recentActivity: ActivityItem[];
  todaysTasks: TaskItem[];
  progress: ProgressItem[];
  githubConnected: boolean;
  leetcodeConnected: boolean;
  leetgitEnabled: boolean;
  setupComplete: boolean;
}

export interface ActivityItem { icon: string; description: string; time: string; type: string; }
export interface TaskItem { task: string; impact: string; priority: string; completed: boolean; }
export interface ProgressItem { label: string; value: number; color: string; }

// ===== RISE =====
export type ResumeStatus = 'UPLOADED' | 'PARSING' | 'ANALYZING' | 'ANALYZED' | 'FAILED';

export interface ResumeResponse {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  atsScore?: number;
  keywordMatchScore?: number;
  formattingScore?: number;
  actionVerbScore?: number;
  quantifiedAchievementsScore?: number;
  extractedSkills?: string[];
  missingKeywords?: string[];
  improvements?: string[];
  strengths?: string[];
  experienceSummary?: string;
  educationSummary?: string;
  projectsSummary?: string;
  overallFeedback?: string;
  status: ResumeStatus;
  active: boolean;
  version?: number;
  uploadedAt: string;
  analyzedAt?: string;
}

export interface SkillGapResponse {
  existingSkills: { skill: string; proficiency: number; source: string }[];
  missingSkills: { skill: string; priority: string; reason: string }[];
  weeklyRoadmap: { week: number; focus: string; tasks: string[]; resources: string[] }[];
  priorityAreas: string[];
  estimatedReadinessWeeks: number;
  currentReadinessPercent: number;
  createdAt: string;
}

// ===== MENTOR =====
export interface ChatResponse {
  conversationId: number;
  conversationTitle: string;
  reply: string;
  timestamp: string;
}

export interface ConversationSummary {
  id: number;
  title: string;
  lastMessage?: string;
  lastMessageAt?: string;
  messageCount: number;
}

export interface ConversationDetail {
  id: number;
  title: string;
  messages: MessageDto[];
  createdAt: string;
}

export interface MessageDto {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

export interface CodeReviewResponse {
  id: number;
  language: string;
  problemContext?: string;
  overallScore?: number;
  grade?: string;
  review?: Record<string, unknown>;
  betterSolution?: Record<string, unknown>;
  createdAt: string;
}

// ===== NEXUS =====
export interface GitHubProfileResponse {
  username?: string;
  displayName?: string;
  avatarUrl?: string;
  bio?: string;
  company?: string;
  location?: string;
  publicRepos?: number;
  followers?: number;
  following?: number;
  totalStars?: number;
  totalForks?: number;
  currentStreak?: number;
  longestStreak?: number;
  topLanguages?: Record<string, number>;
  repositories?: Record<string, unknown>[];
  lastSyncAt?: string;
  connected: boolean;
}

export interface LeetCodeProfileResponse {
  username?: string;
  realName?: string;
  avatarUrl?: string;
  ranking?: string;
  totalSolved?: number;
  easySolved?: number;
  mediumSolved?: number;
  hardSolved?: number;
  acceptanceRate?: number;
  contestRating?: number;
  currentStreak?: number;
  longestStreak?: number;
  totalActiveDays?: number;
  recentSubmissions?: Record<string, unknown>[];
  languageStats?: Record<string, number>;
  lastSyncAt?: string;
  connected: boolean;
}

export interface LeetGitSyncResponse {
  id: number;
  problemTitle: string;
  problemSlug: string;
  difficulty?: string;
  language: string;
  aiScore?: number;
  timeComplexity?: string;
  spaceComplexity?: string;
  aiReview?: Record<string, unknown>;
  optimizedCode?: string;
  githubCommitUrl?: string;
  status: string;
  createdAt: string;
  syncedAt?: string;
}

export interface LeetGitStats {
  totalSynced: number;
  totalPushed: number;
  syncSuccessRate: number;
  lastSyncAt?: string;
  recentSyncs: LeetGitSyncResponse[];
}

// ===== DEV DNA =====
export interface DevDnaReportResponse {
  devDnaScore: number;
  interviewReadinessScore: number;
  scoreBreakdown: {
    github: number;
    leetcode: number;
    leetgit: number;
    resume: number;
    consistency: number;
    aiAssessment: number;
  };
  skillGraph: { skill: string; score: number; category: string }[];
  strengths: string[];
  areasToImprove: string[];
  insights?: string[];
  profileSummary?: string;
  motivationalMessage?: string;
  generatedAt: string;
}

export interface WeeklyReportResponse {
  headline: string;
  overallProgress: string;
  strengths: string[];
  weaknesses: string[];
  insights: string[];
  todaysMission: {
    title: string;
    tasks: { task: string; impact: string; priority: string }[];
    totalImpact: string;
  };
  weeklyGoals: { day: string; focus: string; tasks: string[] }[];
  interviewReadiness: {
    score: number;
    breakdown: Record<string, number>;
    readyFor: string[];
    notReadyFor: string[];
  };
  motivationalMessage: string;
  generatedAt: string;
}
