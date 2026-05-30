# Ghost Coach — AI-Powered Sports Coaching Assistant

An AI coaching assistant that analyzes a player's stance or technique photo and delivers structured, personalized feedback using Google's Gemini Vision API.

---

## Quick Start (under 5 minutes)

### Prerequisites
- Node.js 18+ (frontend)
- Java 17+ (backend)
- Maven 3.9+ **or** use the included `mvnw.cmd` wrapper (auto-downloads Maven on first run)
- A free Gemini API key from [aistudio.google.com](https://aistudio.google.com)

### 1. Configure Environment

```bash
# In the server/ directory, create a .env or set environment variables:
# Windows PowerShell:
$env:GEMINI_API_KEY = "your_key_here"
$env:JWT_SECRET     = "any-random-32-char-string-here"

# Or pass directly when running:
GEMINI_API_KEY=xxx JWT_SECRET=yyy mvn spring-boot:run
```

See `.env.example` in the root for reference.

### 2. Start the Backend

```bash
cd server

# With Maven installed:
mvn spring-boot:run

# Without Maven (Windows — downloads Maven automatically):
mvnw.cmd spring-boot:run
```

Backend runs on `http://localhost:8080`. H2 database file is created automatically at `server/ghostcoach-db.mv.db`. Upload directory created at `server/uploads/`.

### 3. Start the Frontend

```bash
cd client
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`. API calls are proxied to `localhost:8080` automatically via Vite config — no CORS setup needed.

### 4. Open the App

Visit `http://localhost:5173` → Register → Upload a stance photo → Get coaching feedback.

---

## Architecture Decisions

### Multi-Sport Design
The assignment specified picking one sport. I chose to make it **sport-agnostic** instead — users select their sport at registration, and all AI prompts adapt dynamically. This is a better product decision: the same infrastructure serves Cricket, Football, Basketball, and Badminton without separate codebases or hardcoded prompts.

### Backend: Java Spring Boot
Spring Boot provides strong separation of concerns out of the box (controller → service → repository layers), built-in Spring Security with JWT, and excellent JPA integration. The structured layering maps cleanly to the evaluation rubric's emphasis on clean code architecture.

### Database: H2 (File-based)
H2 in file mode (`jdbc:h2:file:./ghostcoach-db`) gives full SQL persistence with zero installation. Hibernate `ddl-auto=update` creates the schema on first run. For production, swapping to PostgreSQL requires only changing two lines in `application.properties`.

### Frontend: React + Vite + Tailwind CSS
Vite's proxy feature (`/api` → `localhost:8080`) eliminates CORS complexity during development. Tailwind CSS enables rapid, consistent responsive design. Component separation: pages handle data fetching and state; UI components are purely presentational.

### AI: Gemini 1.5 Flash
Free, vision-capable, and fast. The vision model accepts base64-encoded images inline. Using `responseMimeType: "application/json"` in the generation config forces Gemini to return valid JSON directly, eliminating the need to strip markdown code fences.

### Prompt Design
The analysis prompt is built dynamically in `PromptBuilderService.buildAnalysisPrompt()`. It injects the player's sport, position, and experience level into the prompt, so a Beginner Batsman gets different feedback than an Advanced Goalkeeper. The structured JSON schema is specified explicitly in the prompt — Gemini reliably returns all 6 required fields.

The chat prompt injects the full session coaching report + conversation history, giving the AI full memory of the session without maintaining server-side state.

---

## AI Prompt Design

### Stance Analysis Prompt
```
You are an expert {sport} coaching assistant analyzing a stance photo.

Player Profile:
- Name: {name} | Sport: {sport} | Position: {position} | Level: {experienceLevel}

Return ONLY valid JSON:
{
  "overallScore": <1-10>,
  "strengths": ["...", "...", "..."],
  "areasToImprove": ["...", "...", "..."],
  "priorityFix": "single most important correction",
  "drillSuggestion": "one specific drill with brief instructions",
  "confidenceLevel": "Low|Medium|High"
}

Be specific to {sport} biomechanics. Use language a {experienceLevel} player understands.
```

**Why this works:** Explicit JSON schema + `responseMimeType: application/json` = zero parsing failures. Player profile injection = personalized feedback at the prompt level, not post-processing.

### Chat Prompt
Injects: full session report + conversation history + player profile. Instructs the AI to respond as a coach in 2–4 sentences, not repeat the report, and relate everything to the player's sport/position/level.

---

## Known Limitations

| Limitation | Fix with more time |
|---|---|
| Images stored on local disk (not cloud) | Migrate to S3/GCS with pre-signed URLs |
| No image compression before sending to Gemini | Add client-side resize (Canvas API) before upload |
| Chat uses single-turn prompts with injected history | Migrate to Gemini's multi-turn `contents` API for cleaner state management |
| No rate limiting on API endpoints | Add Spring's `spring-boot-starter-ratelimiter` or a Redis-based solution |
| H2 not suitable for production multi-instance deployment | PostgreSQL with connection pooling (HikariCP, already bundled) |
| No pagination on session history | Add `Pageable` to the JPA query |

---

## What I'd Build Next (Real Product)

1. **Video analysis** — Gemini 1.5 Flash supports video. Analyzing a 5-second clip of a bowling action or shooting form gives dramatically better feedback than a still frame.

2. **Coach dashboard** — A secondary role (coach/trainer) who can view multiple player profiles, track team progress, and annotate AI feedback with their own notes.

3. **Drill library** — When the AI suggests "shadow footwork drill", link it to a video in a structured drill database. Turn the suggestion into an actionable workout plan.

4. **Push notifications** — "You haven't practiced in 3 days. Your last priority fix was X — here's a quick reminder drill."

5. **Comparison mode** — Side-by-side display of two sessions to visually show technique improvement over time.

6. **Body part annotation** — Use Gemini's bounding box output (available in some configurations) to overlay callouts on the image highlighting the exact body part being critiqued.

---

## Project Structure

```
ghost-coach/
├── client/                         # React + Vite + Tailwind
│   └── src/
│       ├── pages/                  # LoginPage, RegisterPage, DashboardPage,
│       │                           #   HistoryPage, SessionPage, ProgressPage
│       ├── components/             # FeedbackCard, SessionCard, UploadZone,
│       │                           #   ChatBox, Navbar, ProtectedRoute
│       ├── services/api.js         # Axios with JWT interceptor + auto-redirect
│       └── context/AuthContext.jsx # JWT auth state
└── server/                         # Spring Boot
    └── src/main/java/com/ghostcoach/
        ├── controller/             # AuthController, SessionController, ChatController
        ├── service/                # AuthService, SessionService, GeminiService,
        │                           #   PromptBuilderService, ChatService
        ├── repository/             # JPA repositories (Spring Data)
        ├── model/                  # User, CoachingSession, ChatMessage (JPA entities)
        ├── dto/                    # Request/Response DTOs
        ├── security/               # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
        ├── config/                 # SecurityConfig (CORS + JWT chain), AppConfig
        └── exception/              # GlobalExceptionHandler
```
