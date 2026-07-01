-- ============================================================
-- 1. PARENT TABLES (No Foreign Keys)
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    bio VARCHAR(1000),
    avatar_url VARCHAR(500),
    goal VARCHAR(50) DEFAULT 'SDE_1',
    experience_level VARCHAR(50) DEFAULT 'FRESHER',
    setup_complete BOOLEAN DEFAULT FALSE,
    target_company VARCHAR(255),
    current_job_role VARCHAR(255),
    years_of_experience INT,
    github_username VARCHAR(255),
    github_access_token VARCHAR(500),
    github_refresh_token VARCHAR(500),
    github_connected_at TIMESTAMP,
    github_last_sync_at TIMESTAMP,
    github_connected BOOLEAN DEFAULT FALSE,
    leetcode_username VARCHAR(255),
    leetcode_connected_at TIMESTAMP,
    leetcode_last_sync_at TIMESTAMP,
    leetcode_connected BOOLEAN DEFAULT FALSE,
    leethub_repo_name VARCHAR(255),
    leethub_last_sync_at TIMESTAMP,
    leethub_enabled BOOLEAN DEFAULT FALSE,
    dev_dna_score INT DEFAULT 0,
    interview_readiness_score INT DEFAULT 0,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_activity_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============================================================
-- 2. CHILD TABLES (FK to users)
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    problem_context TEXT,
    overall_score INT,
    grade VARCHAR(10),
    review_result_json TEXT,
    better_solution_json TEXT,
    has_better_solution BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS devdna_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dev_dna_score INT,
    interview_readiness_score INT,
    github_score INT,
    leetcode_score INT,
    leetgit_score INT,
    resume_score INT,
    consistency_score INT,
    ai_assessment_score INT,
    skill_graph_json TEXT,
    strengths_json TEXT,
    areas_to_improve_json TEXT,
    insights_json TEXT,
    weekly_roadmap_json TEXT,
    interview_readiness_json TEXT,
    todays_mission_json TEXT,
    profile_summary TEXT,
    motivational_message TEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS github_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),
    bio TEXT,
    company VARCHAR(255),
    location VARCHAR(255),
    blog VARCHAR(500),
    email VARCHAR(255),
    public_repos INT,
    followers INT,
    following INT,
    public_gists INT,
    total_stars INT,
    total_forks INT,
    total_commits_this_year INT,
    current_streak INT,
    longest_streak INT,
    top_languages_json TEXT,
    repositories_json TEXT,
    contribution_data_json TEXT,
    github_created_at TIMESTAMP,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS leetcode_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    real_name VARCHAR(255),
    avatar_url VARCHAR(500),
    ranking VARCHAR(50),
    total_solved INT,
    easy_solved INT,
    medium_solved INT,
    hard_solved INT,
    total_submissions INT,
    acceptance_rate DOUBLE,
    contest_rating DOUBLE,
    contest_ranking INT,
    contests_attended INT,
    current_streak INT,
    longest_streak INT,
    total_active_days INT,
    recent_submissions_json TEXT,
    language_stats_json TEXT,
    badges_json TEXT,
    submission_calendar_json TEXT,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS leetgit_syncs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_title VARCHAR(255),
    problem_slug VARCHAR(255),
    difficulty VARCHAR(20),
    language VARCHAR(50),
    user_code TEXT,
    ai_review_json TEXT,
    optimized_code TEXT,
    readme_content TEXT,
    interview_notes TEXT,
    time_complexity VARCHAR(50),
    space_complexity VARCHAR(50),
    ai_score INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    github_commit_url VARCHAR(500),
    github_file_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500) NOT NULL,
    raw_text TEXT,
    ats_score INT,
    keyword_match_score INT,
    formatting_score INT,
    action_verb_score INT,
    quantified_achievements_score INT,
    extracted_skills_json TEXT,
    missing_keywords_json TEXT,
    improvements_json TEXT,
    strengths_json TEXT,
    experience_summary TEXT,
    education_summary TEXT,
    projects_summary TEXT,
    overall_feedback TEXT,
    status VARCHAR(20) DEFAULT 'UPLOADED',
    active BOOLEAN DEFAULT TRUE,
    version INT,
    analyzed_at TIMESTAMP,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS skill_gaps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    existing_skills_json TEXT,
    missing_skills_json TEXT,
    weekly_roadmap_json TEXT,
    priority_areas_json TEXT,
    estimated_readiness_weeks INT,
    current_readiness_percent INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- 3. GRANDCHILD TABLES (FK to other child tables)
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    token_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE
);
