# Ghost Coach — Knowledge Graph

Visual map of every directory, its purpose, and how it connects to the rest of the system.

---

## Full System Overview

```mermaid
graph TD
    Browser["🌐 Browser\nlocalhost:5173"]
    GeminiAPI["☁️ Gemini Vision API\ngenerativelanguage.googleapis.com"]
    H2DB[("💾 H2 File DB\nghostcoach-db.mv.db")]
    Disk["📁 Local Disk\nserver/uploads/"]

    Browser -->|HTTP + JWT| FE_Pages
    Browser -->|renders| FE_Components

    subgraph CLIENT ["client/src — React Frontend"]
        FE_Context["context/\nAuthContext\nJWT state & user profile"]
        FE_Services["services/\napi.js\nAxios HTTP client"]
        FE_Components["components/\nUI building blocks"]
        FE_Pages["pages/\nRoute-level views"]

        FE_Pages -->|reads auth state| FE_Context
        FE_Pages -->|calls| FE_Services
        FE_Pages -->|renders| FE_Components
        FE_Components -->|uses auth| FE_Context
    end

    FE_Services -->|REST /api/*| BE_Controllers

    subgraph SERVER ["server/src — Spring Boot Backend"]
        BE_Controllers["controller/\nHTTP request handlers"]
        BE_Services["service/\nBusiness logic"]
        BE_Repos["repository/\nJPA data access"]
        BE_Models["model/\nJPA entities"]
        BE_DTOs["dto/\nRequest & response shapes"]
        BE_Security["security/\nJWT filter & UserDetails"]
        BE_Config["config/\nSecurity chain & beans"]
        BE_Exception["exception/\nGlobal error handler"]

        BE_Controllers -->|delegates to| BE_Services
        BE_Controllers -->|uses| BE_DTOs
        BE_Services -->|queries| BE_Repos
        BE_Services -->|maps to| BE_DTOs
        BE_Repos -->|persists| BE_Models
        BE_Security -->|validates JWT on| BE_Controllers
        BE_Config -->|wires| BE_Security
        BE_Exception -->|wraps errors from| BE_Services
        BE_Exception -->|wraps errors from| BE_Controllers
    end

    BE_Services -->|"POST image + prompt"| GeminiAPI
    BE_Repos -->|SQL| H2DB
    BE_Services -->|"read/write images"| Disk
```

---

## Directory Reference

### Frontend — `client/src/`

| Directory | Role | Key Files | Talks To |
|---|---|---|---|
| `context/` | Global auth state — stores the JWT token and decoded user profile. Single source of truth for "who is logged in." | `AuthContext.jsx` | `services/api.js` (for login/register calls), all `pages/` |
| `services/` | Thin HTTP layer — wraps every backend endpoint in a named function. Attaches the JWT to every request automatically via an Axios interceptor. Redirects to `/login` on 401. | `api.js` | Backend `/api/*` routes |
| `components/` | Reusable, stateless UI blocks. Each component receives data as props and emits events up. No direct API calls. | `FeedbackCard`, `SessionCard`, `UploadZone`, `ChatBox`, `Navbar`, `ProtectedRoute` | `context/` (Navbar reads user), `services/` (ChatBox calls chat API) |
| `pages/` | Route-level smart components. Own their data-fetching `useEffect` hooks and loading/error states. Compose components to build the full view. | `DashboardPage`, `HistoryPage`, `SessionPage`, `ProgressPage`, `LoginPage`, `RegisterPage` | `context/`, `services/`, `components/` |

---

### Backend — `server/src/main/java/com/ghostcoach/`

| Directory | Role | Key Files | Talks To |
|---|---|---|---|
| `model/` | JPA entity definitions — the canonical schema of the database. Hibernate uses these to auto-create and migrate tables. | `User`, `CoachingSession`, `ChatMessage` | `repository/` (persisted via Spring Data) |
| `repository/` | Spring Data JPA interfaces — declare queries as method signatures, Spring generates the SQL. No implementation code needed. | `UserRepository`, `CoachingSessionRepository`, `ChatMessageRepository` | `model/` (entity types), `service/` (called by services) |
| `dto/` | Data transfer objects — define what the API accepts and returns. Decouples the HTTP contract from the internal entity shape. Prevents leaking password hashes or internal IDs. | `RegisterRequest`, `LoginRequest`, `AuthResponse`, `FeedbackDto`, `SessionResponse`, `ChatMessageDto` | `controller/` (request binding), `service/` (response building) |
| `service/` | All business logic lives here. Controllers are kept thin — they delegate immediately. Services call repositories, call Gemini, serialize JSON, enforce ownership. | `AuthService`, `SessionService`, `GeminiService`, `PromptBuilderService`, `ChatService` | `repository/`, `dto/`, external Gemini API, local filesystem |
| `controller/` | HTTP entry points only. Deserialize the request, call a service, return the DTO. No business logic, no direct repository access. | `AuthController`, `SessionController`, `ChatController` | `service/`, `dto/` |
| `security/` | JWT machinery — token generation, validation, and injection into Spring Security's context. Runs as a filter before every request. | `JwtUtil`, `JwtAuthFilter`, `UserDetailsServiceImpl` | `repository/` (loads user by email) |
| `config/` | Spring wiring — defines the security filter chain, CORS policy, and shared beans (RestTemplate, ObjectMapper). | `SecurityConfig`, `AppConfig` | `security/` |
| `exception/` | Single `@RestControllerAdvice` that catches all thrown exceptions and maps them to consistent JSON error responses with correct HTTP status codes. | `GlobalExceptionHandler`, `ApiException` | Wraps all layers |

---

## Data Flow Diagrams

### Feature 2: Stance Upload & AI Feedback

```mermaid
sequenceDiagram
    actor Player
    participant UploadZone as UploadZone.jsx
    participant api as api.js
    participant SessionCtrl as SessionController
    participant SessionSvc as SessionService
    participant GeminiSvc as GeminiService
    participant PromptSvc as PromptBuilderService
    participant DB as H2 Database
    participant Gemini as Gemini Vision API

    Player->>UploadZone: drops image file
    UploadZone->>UploadZone: validate type + size (client-side)
    UploadZone->>api: POST /api/sessions (multipart)
    api->>SessionCtrl: HTTP POST with JWT header
    SessionCtrl->>SessionSvc: analyze(email, file)
    SessionSvc->>SessionSvc: validate MIME type (server-side)
    SessionSvc->>SessionSvc: save image to disk (UUID filename)
    SessionSvc->>PromptSvc: buildAnalysisPrompt(user)
    PromptSvc-->>SessionSvc: prompt string with sport/position/level injected
    SessionSvc->>GeminiSvc: analyzeStance(imageBytes, mimeType, prompt)
    GeminiSvc->>Gemini: POST with base64 image + prompt
    Gemini-->>GeminiSvc: JSON { overallScore, strengths, ... }
    GeminiSvc-->>SessionSvc: FeedbackDto
    SessionSvc->>DB: save CoachingSession entity
    SessionSvc-->>SessionCtrl: SessionResponse (with nested FeedbackDto)
    SessionCtrl-->>api: 200 OK JSON
    api-->>UploadZone: res.data
    UploadZone->>Player: renders FeedbackCard
```

### Feature 4: AI Improvement Chat

```mermaid
sequenceDiagram
    actor Player
    participant ChatBox as ChatBox.jsx
    participant api as api.js
    participant ChatCtrl as ChatController
    participant ChatSvc as ChatService
    participant PromptSvc as PromptBuilderService
    participant GeminiSvc as GeminiService
    participant DB as H2 Database
    participant Gemini as Gemini Text API

    Player->>ChatBox: types question, presses Enter
    ChatBox->>api: POST /api/sessions/:id/chat { message }
    api->>ChatCtrl: HTTP POST with JWT
    ChatCtrl->>ChatSvc: sendMessage(email, sessionId, message)
    ChatSvc->>DB: load CoachingSession + full chat history
    ChatSvc->>DB: save user ChatMessage
    ChatSvc->>PromptSvc: buildChatPrompt(user, session, history, message)
    PromptSvc-->>ChatSvc: prompt with full session context + conversation thread
    ChatSvc->>GeminiSvc: chat(prompt)
    GeminiSvc->>Gemini: POST text-only request
    Gemini-->>GeminiSvc: coaching reply text
    GeminiSvc-->>ChatSvc: reply string
    ChatSvc->>DB: save assistant ChatMessage
    ChatSvc-->>ChatCtrl: ChatMessageDto (assistant reply)
    ChatCtrl-->>api: 200 OK
    api-->>ChatBox: res.data
    ChatBox->>Player: appends assistant bubble
```

### Auth Flow

```mermaid
sequenceDiagram
    actor User
    participant LoginPage as LoginPage.jsx
    participant AuthCtx as AuthContext.jsx
    participant api as api.js
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthService
    participant JwtUtil as JwtUtil
    participant DB as H2 Database

    User->>LoginPage: submits email + password
    LoginPage->>AuthCtx: login(email, password)
    AuthCtx->>api: POST /api/auth/login
    api->>AuthCtrl: HTTP POST (no JWT needed)
    AuthCtrl->>AuthSvc: login(LoginRequest)
    AuthSvc->>DB: findByEmail
    AuthSvc->>AuthSvc: BCrypt.matches(password, hash)
    AuthSvc->>JwtUtil: generateToken(email)
    JwtUtil-->>AuthSvc: signed JWT (24h expiry)
    AuthSvc-->>AuthCtrl: AuthResponse { token, userDto }
    AuthCtrl-->>api: 200 OK
    api-->>AuthCtx: res.data
    AuthCtx->>AuthCtx: localStorage.setItem(token, user)
    AuthCtx-->>LoginPage: user object
    LoginPage->>User: navigate to /dashboard
```

---

## Dependency Map

```mermaid
graph LR
    subgraph External
        Gemini["Gemini 1.5 Flash API"]
        H2["H2 Database"]
        FS["Local Filesystem"]
    end

    subgraph Spring Boot
        GeminiSvc["GeminiService"] --> Gemini
        SessionSvc["SessionService"] --> FS
        Repos["Repositories"] --> H2
        JwtUtil["JwtUtil"] -->|HS256 signing| JWT["JWT Standard"]
    end

    subgraph React
        Axios["api.js (Axios)"] -->|proxied by Vite| Spring Boot
        localStorage -->|token persisted| Axios
    end
```
