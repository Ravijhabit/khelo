// API type definitions for the Ghost Coach backend.
// All shapes match the JSON produced by the Spring Boot controllers.

// ── Enums / literals ─────────────────────────────────────────────────────────

export type Sport = "Cricket" | "Basketball" | "Badminton";

export type ExperienceLevel = "Beginner" | "Intermediate" | "Advanced";

export type ConfidenceLevel = "Low" | "Medium" | "High";

export type MessageRole = "user" | "assistant";

// ── Auth ─────────────────────────────────────────────────────────────────────

export interface SportProfile {
  sport: Sport;
  position: string;
  experienceLevel: ExperienceLevel;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  /** One profile per sport the player trains in */
  sportProfiles: SportProfile[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserDto {
  id: number;
  name: string;
  email: string;
  sportProfiles: SportProfile[];
}

export interface AuthResponse {
  token: string;
  user: UserDto;
}

// ── Sessions ─────────────────────────────────────────────────────────────────

export interface FeedbackDto {
  overallScore: number;
  strengths: string[];
  areasToImprove: string[];
  priorityFix: string;
  drillSuggestion: string;
  confidenceLevel: ConfidenceLevel;
}

/** Returned by GET /api/sessions and GET /api/sessions/:id */
export interface SessionResponse {
  id: number;
  imagePath: string;
  overallScore: number;
  /** JSON-encoded string array — use JSON.parse() to get string[] */
  strengths: string;
  /** JSON-encoded string array — use JSON.parse() to get string[] */
  areasToImprove: string;
  priorityFix: string;
  drillSuggestion: string;
  confidenceLevel: ConfidenceLevel;
  createdAt: string; // ISO-8601 LocalDateTime
  /** Enum value from backend: BADMINTON | CRICKET | BASKETBALL. Null for sessions created before this feature. */
  sport?: string;
  /** Only present immediately after POST /api/sessions */
  feedback?: FeedbackDto;
}

// ── Chat ─────────────────────────────────────────────────────────────────────

export interface ChatRequest {
  message: string;
}

export interface ChatMessageDto {
  id: number;
  role: MessageRole;
  content: string;
  createdAt: string; // ISO-8601 LocalDateTime
}

// ── API error ─────────────────────────────────────────────────────────────────

export interface ApiError {
  message: string;
}
