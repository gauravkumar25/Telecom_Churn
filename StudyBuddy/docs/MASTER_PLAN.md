# StudyBuddy — Master Development Plan

> Merged from: original Android concept + Gemini educational app plan  
> Last updated: 2026-04-27  
> Status: Planning → Active Development

---

## Resolved Architecture Decisions

| # | Decision | Resolution |
|---|----------|-----------|
| C1 | Can a PWA access camera and mic for students? | **PWA can** (via `getUserMedia`/`MediaRecorder`), but the **student stays on native Android**. Native gives better textbook scanning (resolution/focus control), offline Room DB, and reliable background audio. PWA is only for **parent/admin** who don't need heavy camera use; parents can upload homework via `<input type="file" capture>` if needed. |
| C2 | Which API key? Claude vs Gemini | **Gemini is the primary API** (single key for everything). Advantages: free-tier eligible (Gemini Flash), Gemini Live handles voice natively (same API family), Firebase + Gemini = cohesive Google stack, one key instead of two. `gemini-2.0-flash` for chat + extraction; `gemini-2.5-pro` for complex reasoning/homework eval. |

---

## Vision

A lifelong AI-powered learning companion that grows with the student year over year.  
The AI acts as a **personal study best friend** for the student, and a **professional progress 
partner** for the parent — remembering everything, never starting from scratch.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                 │
│                                                                     │
│   📱 Android App              🌐 Parent PWA        🖥 Admin PWA    │
│   (Student — Kotlin/Compose)  (React/Next.js)      (React/Next.js) │
│        │                           │                      │         │
│        └───────────────────────────┴──────────────────────┘         │
│                                   │                                 │
│                    WebSocket + REST API                             │
└───────────────────────────────────┼─────────────────────────────────┘
                                    │
┌───────────────────────────────────▼─────────────────────────────────┐
│                       BACKEND LAYER (GCP)                           │
│                                                                     │
│   Cloud Run — Python FastAPI                                        │
│   ┌─────────────────────────────────────────────────────────┐      │
│   │  Auth Service  │  Chat Service  │  Memory Service       │      │
│   │  Eval Service  │  Plan Service  │  Admin Service        │      │
│   └─────────────────────────────────────────────────────────┘      │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │
┌───────────────────────────────────▼─────────────────────────────────┐
│                         DATA LAYER                                  │
│                                                                     │
│  Firebase Auth    Firebase Firestore    Firebase Storage            │
│  (users/roles)    (profiles/sessions)   (images/uploads)           │
│                                                                     │
│  Weaviate (GCP)                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  Class: KnowledgeChunk   — chapter/homework extractions  │      │
│  │  Class: StudentMemory    — per-student session summaries │      │
│  │  Class: ParentMemory     — per-parent interaction memory │      │
│  └──────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼─────────────────────────────────┐
│                          AI LAYER                                   │
│                                                                     │
│  Gemini 2.0 Flash  — chat, document OCR, extraction, RAG queries   │
│  Gemini 2.5 Pro    — complex reasoning, homework evaluation         │
│  Gemini Live       — real-time voice streaming (WebSockets)         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

| Layer | Technology | Reason |
|-------|-----------|--------|
| Student App | Android (Kotlin + Jetpack Compose) | Native camera, offline-capable, voice |
| Parent Portal | Progressive Web App (React + Next.js) | No install, any device |
| Admin Dashboard | Progressive Web App (React + Next.js) | Browser-based management |
| Backend | Python FastAPI on GCP Cloud Run | Serverless, scalable |
| Real-time Voice | Gemini Live (WebSockets / WebRTC) | Purpose-built for live voice AI |
| AI (Chat + RAG) | Gemini 2.0 Flash | Free-tier eligible, fast, single API key |
| AI (Reasoning) | Gemini 2.5 Pro | Best-in-class reasoning for homework eval |
| AI (Extraction) | Gemini 2.0 Flash | Vision + structured JSON extraction |
| Authentication | Firebase Authentication | Email + phone, Google SSO |
| User Profiles | Firebase Firestore | Real-time sync across devices |
| Image Storage | Firebase Storage | Scalable, CDN-backed |
| Vector Database | Weaviate (GCP hosted) | Open source, multi-tenant, schema-flexible |
| Hosting (PWA) | Firebase Hosting | CDN, SSL, auto-deploy |
| Hosting (API) | GCP Cloud Run | Auto-scaling containers |

---

## 3. User Roles & Registration

### 3.1 Registration Flow

```
Student registers:
  name, email/mobile, grade (Class 7), school, academic year (2024-25)
  → Firebase Auth UID created
  → Firestore: Student profile created with grade + academicYear
  → Weaviate: StudentMemory collection initialised (empty)

Parent registers:
  name, email/mobile, relationship
  → links to Student via invite code or student UID
  → Firestore: Parent profile linked to student
  → Weaviate: ParentMemory collection initialised (empty)

Admin:
  Created by platform super-admin
  Access to all user accounts, no AI interaction
```

### 3.2 Grade Profile — How It Drives the AI

The student's **grade is stored in their Firestore profile**, not filtered from the 
knowledge DB. At the start of every AI session:

```
System prompt receives:
  "Student: Priya, Class 7, Academic Year 2024-25, School: DPS"

The AI:
  - Calibrates explanation complexity to Class 7 level
  - Knows Class 7 curriculum (from its own training)
  - Pitches vocabulary, examples, and depth appropriately
  - Does NOT search the DB for "what is in Class 7 syllabus"
```

When the student moves to Class 8:
- Parent updates grade in Settings → `grade: "Class 8"`, `academicYear: "2025-26"`
- All future AI responses recalibrate to Class 8 level instantly
- All previous knowledge chunks (tagged `grade: "Class 7"`) remain in Weaviate
- New Class 8 uploads are tagged `grade: "Class 8"`, `academicYear: "2025-26"`
- The AI's system prompt shows both: "Currently Class 8. Has Class 7 knowledge from last year."

---

## 4. Feature Specifications

### 4.1 Student Features

#### AI Study Chat (voice + text)
- Real-time voice via **Gemini Live** over WebSocket
- Text chat as fallback and complement
- "AI bestie" persona — warm, encouraging, uses relatable language
- Maintains conversation context within session

#### Homework Evaluation
- Student scans handwritten notes or homework with camera
- **Gemini 2.5 Pro** evaluates:
  - Is the solution complete?
  - Are there mistakes? (flags specific lines/steps)
  - What is done well?
  - What should be corrected?
- Feedback displayed inline, student can ask follow-up questions
- Evaluation stored as a memory event (see Memory System)

#### Knowledge Upload (Chapter Scanning)
- Student or parent photographs textbook pages
- **Gemini 2.0 Flash** extracts structured content:
  - Questions, definitions, formulas, examples (with exercise/item numbers)
- Stored in Weaviate `KnowledgeChunk` class, tagged with `grade` + `academicYear`
- Image can be deleted after extraction — knowledge lives permanently in Weaviate

#### Intelligent Q&A with RAG
- Student asks: "explain question 4 in exercise 3.4"
- **QueryRouter** classifies: structured reference → Weaviate filtered query
- Student asks: "what is photosynthesis?"  
- **QueryRouter** classifies: semantic → cosine similarity search
- Top-K relevant chunks injected into Gemini system prompt → answer uses student's own notes
- Grade profile ensures explanation depth matches Class 7 / Class 8 / etc.

#### Customised Study Plans
- AI generates weekly/monthly study schedules
- Inputs: uploaded syllabus, exam date sheet, current grade, weak topics from memory
- Includes: subject rotation, revision slots, breaks, quiz checkpoints
- Exportable to calendar

#### Quizzes & Revision
- AI quizzes student on uploaded chapter content
- Multiple choice, fill-in-the-blank, explain-in-your-own-words
- Tracks score per topic, stores result in StudentMemory

#### Gamification
- Badges: First Upload, Week Streak, Quiz Master, Exam Ready, etc.
- Milestone progress bar per subject
- Parent can see badges earned

---

### 4.2 Agentic Memory System (Most Critical Feature)

This is what makes the AI feel like a genuine long-term friend, not a stateless chatbot.

#### Student Memory
```
After every chat session, a Memory Agent runs:

1. Summarises the session:
   "Priya struggled with calculating speed using the formula. 
    She understood photosynthesis well after the diagram explanation.
    She seemed anxious — mentioned exam is in 4 days."

2. Extracts structured memory events:
   { type: "struggle",    topic: "speed formula",     date: "2024-11-10" }
   { type: "understood",  topic: "photosynthesis",    date: "2024-11-10" }
   { type: "emotion",     value: "anxious",           exam_in_days: 4    }
   { type: "homework",    subject: "Math", score: 6/10, mistakes: ["Q3"] }

3. Embeds each memory event → stores in Weaviate StudentMemory class

At the START of the next session:
  Retrieve top-10 most relevant recent memories
  Inject into system prompt:
  
  "STUDENT MEMORY:
   - Last session (3 days ago): struggled with speed formula — try a different approach
   - Understood photosynthesis well — can use as a confidence boost example
   - Exam anxiety noted — check in on how she's feeling today
   - Math homework: made mistakes on Q3 (fractions) — revisit this"

The AI then opens with: "Hey! How are you feeling today? Last time you were a bit 
stressed about the exam — did it go okay?"
```

#### Parent Memory
```
Separate memory profile — professional tone only.

After every parent interaction:
  Summarises: "Parent asked about Priya's Math performance. Was concerned about 
               upcoming board exams. Requested weekly progress emails."

Stored as ParentMemory events in Weaviate.

At the start of next parent session:
  Retrieve parent's recent memory → inject with professional framing:
  
  "PARENT CONTEXT:
   - Previous concern: Math performance (discussed 2 weeks ago)
   - Requested: weekly summaries
   - Goals set: improve Science score by 10% before December exams"

AI responds: "Welcome back. Following up on our last discussion — 
Priya's Math scores have improved since we worked on the speed and 
distance problems. Here's her progress this week..."
```

#### Memory Schema in Weaviate

```python
# StudentMemory class
{
  "class": "StudentMemory",
  "properties": [
    { "name": "studentId",    "dataType": ["text"] },
    { "name": "sessionId",    "dataType": ["text"] },
    { "name": "memoryType",   "dataType": ["text"] },  # struggle/understood/emotion/homework/milestone
    { "name": "topic",        "dataType": ["text"] },
    { "name": "summary",      "dataType": ["text"] },
    { "name": "grade",        "dataType": ["text"] },
    { "name": "academicYear", "dataType": ["text"] },
    { "name": "createdAt",    "dataType": ["date"] }
  ]
}

# ParentMemory class
{
  "class": "ParentMemory",
  "properties": [
    { "name": "parentId",     "dataType": ["text"] },
    { "name": "studentId",    "dataType": ["text"] },
    { "name": "summary",      "dataType": ["text"] },
    { "name": "concerns",     "dataType": ["text[]"] },
    { "name": "goalsSet",     "dataType": ["text[]"] },
    { "name": "createdAt",    "dataType": ["date"] }
  ]
}

# KnowledgeChunk class (existing — moved to Weaviate)
{
  "class": "KnowledgeChunk",
  "properties": [
    { "name": "studentId",    "dataType": ["text"] },
    { "name": "subjectName",  "dataType": ["text"] },
    { "name": "grade",        "dataType": ["text"] },
    { "name": "academicYear", "dataType": ["text"] },
    { "name": "contentType",  "dataType": ["text"] },  # QUESTION/FORMULA/DEFINITION...
    { "name": "exerciseId",   "dataType": ["text"] },
    { "name": "itemNumber",   "dataType": ["int"] },
    { "name": "chunkText",    "dataType": ["text"] },
    { "name": "sourceUri",    "dataType": ["text"] },
    { "name": "createdAt",    "dataType": ["date"] }
  ]
}
```

---

### 4.3 Parent Features

- **Dashboard**: Subject-wise progress, recent session summaries, quiz scores
- **AI Chat**: Professional-tone AI discusses child's progress (uses ParentMemory)
- **Alerts**: Exam countdown, missed study sessions, struggling topics
- **Badge Feed**: See gamification achievements earned by the student
- **Communication style**: Formal English — no slang, no emojis, data-driven

### 4.4 Admin Features

- User management: create/suspend/delete student and parent accounts
- Account mapping: link/unlink parent ↔ student
- Usage analytics: uploads per student, session frequency, AI call volume
- Content moderation: flag any inappropriate uploads

---

## 5. Multi-Year Knowledge Retention

```
Year 1 — Class 7 (2024-25):
  All KnowledgeChunks: { grade: "Class 7", academicYear: "2024-25" }
  All StudentMemory:   { grade: "Class 7", academicYear: "2024-25" }

Year 2 — Class 8 (2025-26):
  Parent updates profile: grade → "Class 8", academicYear → "2025-26"
  New uploads tagged:  { grade: "Class 8", academicYear: "2025-26" }
  Class 7 data:        still in Weaviate, never deleted

  AI system prompt:
    "Student: Priya, currently Class 8 (2025-26).
     Has accumulated knowledge from Class 7 (2024-25).
     Primary context: Class 8. Cross-grade reference available."

Year 3 — Class 9 and beyond: same pattern compounds
```

**Data retention policy**: Knowledge chunks and memories are **never auto-deleted**.  
The parent/admin can manually delete specific documents if needed.

---

## 6. Real-Time Voice Chat Architecture

```
Student opens voice chat
        ↓
Android app → WebSocket connection → GCP Backend
        ↓
Backend → Gemini Live session initialised
        ↓
Audio stream: Android mic → backend → Gemini Live
        ↓
Gemini Live response audio → backend → Android speaker
        ↓
[In parallel] Text transcript of conversation
        ↓
At session end → Memory Agent processes transcript → stores to Weaviate
```

---

## 7. Project Implementation — Master Todo Table

| ID | Phase | Category | Task | Priority | Status |
|----|-------|----------|------|----------|--------|
| **PHASE 1 — Infrastructure** |
| T01 | 1 | Infra | Set up GCP project, enable Cloud Run, Artifact Registry | P0 | Pending |
| T02 | 1 | Infra | Configure Firebase project (Auth + Firestore + Storage) | P0 | Pending |
| T03 | 1 | Infra | Deploy Weaviate cluster on GCP (or Weaviate Cloud) | P0 | Pending |
| T04 | 1 | Infra | Define Weaviate schema: KnowledgeChunk, StudentMemory, ParentMemory | P0 | Pending |
| T05 | 1 | Infra | Set up CI/CD pipeline (GitHub Actions → Cloud Run) | P1 | Pending |
| **PHASE 2 — Backend API** |
| T06 | 2 | Backend | Scaffold FastAPI project with auth middleware | P0 | Pending |
| T07 | 2 | Backend | Auth Service: Firebase token verification, role extraction | P0 | Pending |
| T08 | 2 | Backend | Registration endpoints: student + parent + account linking | P0 | Pending |
| T09 | 2 | Backend | Knowledge Service: image → Gemini Vision OCR → Weaviate ingest | P0 | Pending |
| T10 | 2 | Backend | Search Service: QueryRouter → Weaviate structured + semantic search | P0 | Pending |
| T11 | 2 | Backend | Chat Service: Gemini RAG response with memory + knowledge injection | P0 | Pending |
| T12 | 2 | Backend | Memory Agent: post-session summarisation → Weaviate write | P0 | Pending |
| T13 | 2 | Backend | Memory Retrieval: fetch top-K memories for session start | P0 | Pending |
| T14 | 2 | Backend | Voice Service: Gemini Live WebSocket proxy | P1 | Pending |
| T15 | 2 | Backend | Homework Evaluation: Gemini 2.5 Pro structured feedback pipeline | P1 | Pending |
| T16 | 2 | Backend | Study Plan Generator: Gemini + student profile + exam dates | P1 | Pending |
| T17 | 2 | Backend | Parent Report Service: progress summary with professional tone | P1 | Pending |
| T18 | 2 | Backend | Admin Service: user CRUD, account mapping, analytics | P2 | Pending |
| T19 | 2 | Backend | Gamification Service: badge rules engine, milestone tracking | P2 | Pending |
| **PHASE 3 — Android Student App** |
| T20 | 3 | Android | Migrate auth from local prefs to Firebase Auth SDK | P0 | Pending |
| T21 | 3 | Android | Registration screen: grade, name, school, mobile, academic year | P0 | Pending |
| T22 | 3 | Android | Replace local Room vector store with backend API calls | P0 | Pending |
| T23 | 3 | Android | Chat screen: connect to backend Chat Service (REST) | P0 | Pending |
| T24 | 3 | Android | Voice chat screen: connect to Gemini Live via WebSocket | P1 | Pending |
| T25 | 3 | Android | Knowledge upload: image → backend Knowledge Service | P0 | Pending |
| T26 | 3 | Android | Homework evaluation screen: scan → backend Eval Service | P1 | Pending |
| T27 | 3 | Android | Study plan viewer screen | P1 | Pending |
| T28 | 3 | Android | Knowledge Base screen: docs from backend (not local Room) | P1 | Pending |
| T29 | 3 | Android | Gamification: badges + milestone progress UI | P2 | Pending |
| T30 | 3 | Android | Offline mode: cache last session, queue uploads when offline | P2 | Pending |
| **PHASE 4 — Parent PWA** |
| T31 | 4 | PWA | Scaffold Next.js PWA with Firebase Auth | P0 | Pending |
| T32 | 4 | PWA | Parent registration + student account linking flow | P0 | Pending |
| T33 | 4 | PWA | Progress dashboard: subject scores, session activity, quiz results | P1 | Pending |
| T34 | 4 | PWA | AI chat interface for parent (professional tone) | P1 | Pending |
| T35 | 4 | PWA | Exam countdown + alert system | P1 | Pending |
| T36 | 4 | PWA | Badge/milestone feed | P2 | Pending |
| T37 | 4 | PWA | Weekly progress summary view | P2 | Pending |
| **PHASE 5 — Admin PWA** |
| T38 | 5 | Admin | Admin dashboard: user list, roles, account mapping | P1 | Pending |
| T39 | 5 | Admin | User creation / suspension / deletion | P1 | Pending |
| T40 | 5 | Admin | Usage analytics: uploads, sessions, AI call volume per student | P2 | Pending |
| **PHASE 6 — Testing & Launch** |
| T41 | 6 | QA | End-to-end test: registration → upload → chat → memory → next session | P0 | Pending |
| T42 | 6 | QA | Voice chat latency testing (Gemini Live WebSocket) | P1 | Pending |
| T43 | 6 | QA | Multi-year data retention verification | P1 | Pending |
| T44 | 6 | QA | Parent ↔ student account mapping security audit | P0 | Pending |
| T45 | 6 | Launch | Beta launch with single family | P0 | Pending |

---

## 8. Current Status (What's Already Built)

The following is implemented in `StudyBuddy/` on branch `claude/android-study-chat-app-skEmg`:

| Component | Status | Notes |
|-----------|--------|-------|
| Android app shell (Jetpack Compose) | ✅ Done | Needs auth migration (T20) |
| **Gemini API** chat + extraction integration | ✅ Done | Migrated from Claude; single API key |
| Voice input/output (SpeechRecognizer + TTS) | ✅ Done | Upgrade to Gemini Live (T24) |
| Camera capture + image persistence | ✅ Done | Needs to call backend instead of local (T25) |
| Local Room vector DB (v3 with grade/academicYear) | ✅ Done | Will be replaced by Weaviate (T22) |
| Knowledge extraction (structured JSON via Gemini) | ✅ Done | Move to backend service (T09) |
| QueryRouter (hybrid search) | ✅ Done | Move to backend service (T10) |
| TextEmbedder (512-dim, offline) | ✅ Done | Replace with Weaviate's built-in embedder |
| Subjects + Chapters management | ✅ Done | |
| Exam timetable | ✅ Done | |
| Settings screen (Gemini API key) | ✅ Done | Add registration screen (T21) |
| Agentic Memory | ❌ Not yet | T12, T13 — highest priority next |
| Multi-user (Auth / Parent / Admin) | ❌ Not yet | T06–T08, T31–T40 |
| Homework evaluation | ❌ Not yet | T15 |
| Gamification | ❌ Not yet | T19, T29 |

---

## 9. API Design (Key Endpoints)

```
POST /auth/register/student
POST /auth/register/parent
POST /auth/link                      # link parent to student

POST /knowledge/upload               # image → extract → store in Weaviate
GET  /knowledge/documents            # list all docs for student
DELETE /knowledge/document/:id

POST /chat/message                   # send message, get RAG response
WS   /chat/voice                     # Gemini Live WebSocket session

POST /homework/evaluate              # scan homework → get structured feedback

GET  /memory/student/:id             # retrieve student memories
GET  /memory/parent/:id              # retrieve parent memories

GET  /progress/student/:id           # for parent dashboard
GET  /gamification/badges/:id

GET  /admin/users
POST /admin/users
DELETE /admin/users/:id
```

---

## 10. Migration Path from Current Build

The current local Android app will evolve in stages:

```
Stage 1 (current): Fully local — Claude API called from device, Room DB
Stage 2 (next):    Hybrid — Firebase Auth added, uploads go to backend,
                   chat still from device using stored API key
Stage 3 (target):  Full cloud — all AI calls via backend, Weaviate replaces
                   Room vectors, parent PWA live, memory system active
```

This staged approach means the app remains usable throughout development
and the family can start using it immediately while the cloud features are built.
